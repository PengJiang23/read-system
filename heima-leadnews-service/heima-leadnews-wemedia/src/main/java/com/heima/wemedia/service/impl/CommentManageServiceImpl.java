package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.apis.article.IArticleClient;
import com.heima.apis.user.IUserClient;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentManageDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.pojos.ApComment;
import com.heima.wemedia.pojos.ApCommentLike;
import com.heima.wemedia.pojos.ApCommentRepay;
import com.heima.wemedia.pojos.CommentRepayListVo;
import com.heima.wemedia.service.CommentManageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class CommentManageServiceImpl implements CommentManageService {

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 查询当前用户文章的所有评论情况
     *
     * @param dto
     * @return
     */
    @Override
    public PageResponseResult findNewsComments(ArticleCommentDto dto) {
        /**
         * - 按照时间倒序查询发布的文章
         * - 展示文章标题 评论状态 评论总量
         * - 分页可以查询
         * - 可以按照时间范围查询
         *
         * aparticle article config
         */

        WmUser user = WmThreadLocalUtil.getUser();
        dto.setWmUserId(user.getId());
        return articleClient.findNewsComments(dto);
    }


    /**
     * update 评论状态
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult updateCommentStatus(CommentConfigDto dto) {

        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 这里关闭评论也可以删除掉db中的评论数据
        return articleClient.updateCommentStatus(dto);
    }


    /**
     * 查询当前文章的所有评论
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(CommentManageDto dto) {

        List<CommentRepayListVo> commentRepayListVoList = new ArrayList<>();


        Query query = Query.query(Criteria.where("entryId").is(dto.getArticleId()));
        Pageable pageable = PageRequest.of(dto.getPage(), dto.getSize());
        // todo 分页有问题，size超出实际条数返回为0但是gpt将不会返回0
//        query.with(pageable);

        query.with(Sort.by(Sort.Direction.DESC, "createdTime"));

        List<ApComment> list = mongoTemplate.find(query, ApComment.class);
        System.err.println(list.size());
        for (ApComment apComment : list) {
            CommentRepayListVo vo = new CommentRepayListVo();
            vo.setApComments(apComment);
            Query query2 = Query.query(Criteria.where("commentId").is(apComment.getId()));
            query2.with(Sort.by(Sort.Direction.DESC, "createdTime"));
            List<ApCommentRepay> apCommentRepays = mongoTemplate.find(query2, ApCommentRepay.class);
            vo.setApCommentRepays(apCommentRepays);

            commentRepayListVoList.add(vo);
        }
        return ResponseResult.okResult(commentRepayListVoList);

    }


    /**
     * 删除主评论
     *
     * @param commentId
     * @return
     */
    @Override
    public ResponseResult delComment(String commentId) {

        /**
         * 删除主评论  like表   repay  repay表
         */
        if (StringUtils.isBlank(commentId)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        // todo jvm 堆异常， 这里commentid换成query.query的对象就会报错
        ApComment apComment = mongoTemplate.findById(commentId, ApComment.class);
        Integer authorId = apComment.getAuthorId();

        mongoTemplate.remove(Query.query
                (
                        Criteria.where("id").is(commentId)
                ), ApComment.class);

        mongoTemplate.remove(Query.query
                (
                        Criteria.where("authorId").is(authorId)
                                .and("commentId").is(commentId)
                ), ApCommentLike.class);

        mongoTemplate.remove(Query.query
                (
                        Criteria.where("authorId").is(authorId)
                                .and("commentId").is(commentId)
                ), ApCommentRepay.class);


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除回复评论
     *
     * @param commentRepayId
     * @return
     */
    @Override
    public ResponseResult delCommentRepay(String commentRepayId) {

        if (StringUtils.isBlank(commentRepayId)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        WmUser dbWmUser = wmUserMapper.selectById(user.getId());
        ApUser dbUser = userClient.findUserById(dbWmUser.getApUserId());


        // 临时获取主评论id
        ApCommentRepay apCommentRepay = mongoTemplate.findById(commentRepayId, ApCommentRepay.class);
        // update 主评论的repay
        ApComment apComment = mongoTemplate.findById(apCommentRepay.getCommentId(), ApComment.class);
        apComment.setReply((apComment.getReply() - 1) < 0 ? 0 : (apComment.getReply() - 1));
        mongoTemplate.save(apComment);

        // 删除回复评论，这里回复评论不可点赞（增加点赞，需要将回复点赞表同样remove）
        mongoTemplate.remove(Query.query(
                Criteria.where("authorId").is(dbUser.getId())
                        .and("id").is(commentRepayId)
        ), ApCommentRepay.class);


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmUserMapper wmUserMapper;

    @Autowired
    private IUserClient userClient;

    /**
     * 回复评论
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {
        // 只能对主评论回复
        // 记录当前回复的信息
        // 获取当前用户信息
        //1.检查参数
        if (dto == null || StringUtils.isBlank(dto.getContent()) || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if (dto.getContent().length() > 140) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "评论内容不能超过140字");
        }


        // we端用户 ->      app端用户及相关info
        WmUser wmUser = WmThreadLocalUtil.getUser();
        WmUser dbUser = wmUserMapper.selectById(wmUser.getId());
        if (dbUser == null) {
            log.error("为空");
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //获取app端用户信息
        ApUser apUser = userClient.findUserById(dbUser.getApUserId());

        ApCommentRepay apCommentRepay = new ApCommentRepay();
        apCommentRepay.setAuthorId(apUser.getId());
        apCommentRepay.setAuthorName(apUser.getName());
        apCommentRepay.setContent(dto.getContent());
        apCommentRepay.setCreatedTime(new Date());
        apCommentRepay.setCommentId(dto.getCommentId());

        apCommentRepay.setUpdatedTime(new Date());
        apCommentRepay.setLikes(0);
        mongoTemplate.save(apCommentRepay);

        ApComment apComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);
        apComment.setReply(apComment.getReply() + 1);
        mongoTemplate.save(apComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * 主评论点赞
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(CommentLikeDto dto) {
        /**
         * 点赞，主评论更新点赞数量
         * 记录点赞数量表
         */

        if (dto == null || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }


        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        WmUser dbWmUser = wmUserMapper.selectById(user.getId());
        ApUser dbUser = userClient.findUserById(dbWmUser.getApUserId());

        // 点赞
        ApComment apComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);
        if (dto.getOperation() == 0 && dbUser != null) {
            apComment.setLikes(apComment.getLikes() + 1);
            mongoTemplate.save(apComment);

            ApCommentLike apCommentLike = new ApCommentLike();
            apCommentLike.setAuthorId(dbUser.getId());
            apCommentLike.setCommentId(dto.getCommentId());
            mongoTemplate.save(apCommentLike);
        } else {
            // update主评论点赞数
            int tmp = apComment.getLikes();
            tmp = tmp - 1;
            apComment.setLikes(tmp >= 0 ? tmp : 0);
            mongoTemplate.save(apComment);

            // 点赞记录remove
            mongoTemplate.remove(Query.query
                            (
                                    Criteria.where("authorId").is(dbUser.getId())
                                            .and("commentId").is(dto.getCommentId())
                            )
                    , ApCommentLike.class);

        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
