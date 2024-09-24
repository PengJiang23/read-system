package com.heima.comment.service.impl;


import com.alibaba.fastjson.JSON;
import com.heima.apis.article.IArticleClient;
import com.heima.apis.user.IUserClient;
import com.heima.comment.pojos.ApComment;
import com.heima.comment.pojos.ApCommentLike;
import com.heima.comment.pojos.CommentVo;
import com.heima.comment.service.CommentService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private IUserClient userClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Override
    public ResponseResult saveComment(CommentSaveDto dto) {

        // 登录判断
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        if (dto == null || dto.getArticleId() == null || StringUtils.isBlank(dto.getContent())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }


        if (dto.getContent().length() > 140) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "评论内容长度超出限制");
        }


        // 文章是否可以评论
        if (!checkParam(dto.getArticleId())) {
            log.error("禁止评论, status:{}, articleId:{}",checkParam(dto.getArticleId()),dto.getArticleId());
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "该文章评论权限已关闭");
        }


        // 评论保存2mongodb
        ApUser dbUser = userClient.findUserById(user.getId());
        if (dbUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前登录信息有误");
        }

        ApComment comment = new ApComment();
        comment.setAuthorId(user.getId());
        comment.setContent(dto.getContent());
        comment.setCreatedTime(new Date());
        comment.setEntryId(dto.getArticleId());
        comment.setImage(dbUser.getImage());
        comment.setAuthorName(dbUser.getName());
        comment.setLikes(0);
        comment.setReply(0);
        comment.setType((short) 0);
        comment.setFlag((short) 0);
        mongoTemplate.save(comment);


        // 评论用户行为，需要发送到kafka，实习计算文章热度
        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.COMMENT);
        mess.setAdd(1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 文章是否可以评论
     *
     * @param articleId
     * @return
     */
    private boolean checkParam(Long articleId) {

        ResponseResult articleConfigResult = articleClient.findArticleConfigByArticleId(articleId);
        if (!articleConfigResult.getCode().equals(200) || articleConfigResult.getData() == null) {
            return false;
        }

        ApArticleConfig apArticleConfig = JSON.parseObject(JSON.toJSONString(articleConfigResult.getData()), ApArticleConfig.class);
        if (apArticleConfig == null || !apArticleConfig.getIsComment()) {
            return false;
        }

        return true;
    }

    @Override
    public ResponseResult like(CommentLikeDto dto) {

        if (dto == null || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 登录判断
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        // 点赞，需要更新评论表更新点赞数+记录用户点赞情况
        ApComment apComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);

        if (apComment != null && dto.getOperation() == 0) {
            apComment.setLikes(apComment.getLikes() + 1);
            mongoTemplate.save(apComment);

            // sava 用户点赞情况
            ApCommentLike apCommentLike = new ApCommentLike();
            apCommentLike.setCommentId(dto.getCommentId());
            apCommentLike.setAuthorId(user.getId());
            mongoTemplate.save(apCommentLike);
        } else {

            int tmp = apComment.getLikes() - 1;
            tmp = tmp < 1 ? 0 : tmp;
            apComment.setLikes(tmp);
            mongoTemplate.save(apComment);


            mongoTemplate.remove(Query.query(Criteria
                    .where("commentId").is(dto.getCommentId())
                    .and("authorId").is(user.getId())), ApCommentLike.class);
        }

        //4.触发点赞操作，还需要将点赞数量返回
        Map<String, Object> result = new HashMap<>();
        result.put("likes", apComment.getLikes());
        return ResponseResult.okResult(result);
    }

    @Override
    public ResponseResult findByArticleId(CommentDto dto) {

        /**
         * 展示评论内容、评论的作者、点赞数、回复数、时间
         * - 查询评论列表，根据当前文章进行检索，按照创建时间倒序，分页查询（默认10条数据）
         * - 在结果返回中，如果当天登录人点赞了某条评论，需要高亮展示“**点赞按钮**”
         */


        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        int size = 10;

        Query query = Query.query(Criteria.where("entryId").is(dto.getArticleId())
                .and("createdTime").lt(dto.getMinDate()));
        query.with(Sort.by(Sort.Direction.DESC, "createdTime")).limit(size);

        List<ApComment> commentList = mongoTemplate.find(query, ApComment.class);


        // 登录判断
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.okResult(commentList);
        }

        List<String> list = commentList.stream().map(x -> x.getId()).collect(Collectors.toList());
        Query query1 = Query.query(Criteria.where("commentId").in(list).and("authorId").is(user.getId()));
        List<ApCommentLike> apCommentLikes = mongoTemplate.find(query1, ApCommentLike.class);
        if (apCommentLikes == null) {
            return ResponseResult.okResult(commentList);
        }

        List<CommentVo> resultList = new ArrayList<>();
        commentList.forEach(x -> {
            CommentVo vo = new CommentVo();
            BeanUtils.copyProperties(x, vo);
            for (ApCommentLike apCommentLike : apCommentLikes) {
                if (x.getId().equals(apCommentLike.getCommentId())) {
                    vo.setOperation((short) 0);
                    break;
                }
            }
            resultList.add(vo);
        });

        return ResponseResult.okResult(resultList);
    }
}
