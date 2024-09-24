package com.heima.comment.controller.v1;


import com.heima.comment.service.CommentRepayService;
import com.heima.comment.service.CommentService;
import com.heima.model.comment.dtos.*;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comment_repay")
public class CommentRepayController {

    @Autowired
    private CommentRepayService commentRepayService;


    /**
     * 保存评论
     */
    @PostMapping("/save")
    public ResponseResult saveComment(@RequestBody CommentRepaySaveDto dto) {
        return commentRepayService.saveCommentRepay(dto);
    }

    @PostMapping("/like")
    public ResponseResult like(@RequestBody CommentRepayLikeDto dto){
        return commentRepayService.repayLike(dto);
    }

    @PostMapping("/load")
    public ResponseResult findByArticleId(@RequestBody CommentRepayDto dto){
        return commentRepayService.findRepayByArticleId(dto);
    }

}
