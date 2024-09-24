package com.heima.comment.service.impl;


import com.heima.apis.user.IUserClient;
import com.heima.comment.pojos.*;
import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentRepayServiceImpl implements CommentRepayService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IUserClient userClient;


    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {
        /**
         * 涉及repay+comment表
         *
         * checkStatus：是否可以评论、当前评论info、用户是否登录
         * save评论：构造repay数据，保存；更新comment表repay字段
         */

        if (dto == null || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        if (dto.getContent().length() > 140) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "评论内容不能超过140字");
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        ApComment mainComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);
        // db中没有save该字段
//        if(mainComment.getStatus().equals(false)){
//            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "该条评论下不可以回复");
//        }

        // todo 文本内容审核
        /**
         * 审核
         */

        ApUser dbUser = userClient.findUserById(user.getId());
        if (dbUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前登录信息有误");
        }

        // save repay
        ApCommentRepay commentRepay = new ApCommentRepay();
        commentRepay.setAuthorName(dbUser.getName());
        commentRepay.setAuthorId(user.getId());
        commentRepay.setCommentId(dto.getCommentId());
        commentRepay.setContent(dto.getContent());
        commentRepay.setLikes(0);
        commentRepay.setCreatedTime(new Date());
        commentRepay.setUpdatedTime(new Date());
        mongoTemplate.save(commentRepay);

        // update comment
        mainComment.setReply(mainComment.getReply() + 1);
        mongoTemplate.save(mainComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult repayLike(CommentRepayLikeDto dto) {

        /**
         * 点赞行为：取消和点赞
         *  repay和repay-like表
         *  check-status：登录状态下； 更新或者修改某个字段确保不能造成异常
         *  update：insert/delete repay-like   + update repay
         *  封装前端要求return参数
         */

        if (dto == null || dto.getCommentRepayId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        // 点赞 todo 防止重复点赞和取消点赞
        ApCommentRepay commentRepay = mongoTemplate.findById(dto.getCommentRepayId(), ApCommentRepay.class);
        if (commentRepay != null && dto.getOperation().equals((short) 0)) {
            commentRepay.setLikes(commentRepay.getLikes() + 1);
            mongoTemplate.save(commentRepay);

            ApCommentLike apCommentLike = new ApCommentLike();
            apCommentLike.setAuthorId(user.getId());
            apCommentLike.setCommentId(dto.getCommentRepayId());
            mongoTemplate.save(apCommentLike);
        } else {
            int tmp = commentRepay.getLikes() - 1;
            tmp = tmp < 1 ? 0 : tmp;
            commentRepay.setLikes(tmp);
            mongoTemplate.save(commentRepay);

            mongoTemplate.remove(Query.query(Criteria.where("authorId").is(user.getId())
                    .and("commentRepayId").is(dto.getCommentRepayId())));
        }

        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("like", commentRepay.getLikes());

        return ResponseResult.okResult(hashMap);
    }

    @Override
    public ResponseResult findRepayByArticleId(CommentRepayDto dto) {

        if (dto == null || dto.getCommentId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        int size = 10;
        Query query = Query.query(
                Criteria.where("commentId").is(dto.getCommentId())
                        .and("createdTime").lt(dto.getMinDate())
        );
        query.with(Sort.by(Sort.Direction.DESC, "createdTime")).limit(size);

        // BR：时间倒序，10条，该评论下的所有用户评论
        List<ApCommentRepay> commentRepayList = mongoTemplate.find(query, ApCommentRepay.class);

        // 给所有评论增加点赞属性：只有当前用户点赞的评论设置该字段为0（check回复的评论是否有当前用户点赞）
        List<String> idList = commentRepayList.stream().map(x -> x.getId()).collect(Collectors.toList());
        Query query1 = Query.query(Criteria.where("commentRepayId").in(idList).and("authorId").is(user.getId()));
        List<ApCommentRepayLike> apCommentRepayLikeList = mongoTemplate.find(query1, ApCommentRepayLike.class);
        if (apCommentRepayLikeList == null || apCommentRepayLikeList.size() == 0) {
            // 如果所有评论都没有当前用户点赞，直接返回
            return ResponseResult.okResult(commentRepayList);
        }


        List<CommentRepayVo> resultList = new ArrayList<>();
        commentRepayList.forEach(commentRepay -> {
            CommentRepayVo vo = new CommentRepayVo();
            BeanUtils.copyProperties(commentRepay, vo);

            for (ApCommentRepayLike apCommentRepayLike : apCommentRepayLikeList) {
                if (commentRepay.getId().equals(apCommentRepayLike.getCommentRepayId())) {
                    vo.setOperation((short) 0);
                    break;
                }
            }
            resultList.add(vo);

        });


        return ResponseResult.okResult(resultList);
    }
}
