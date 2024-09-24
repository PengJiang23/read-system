package com.heima.model.mess;

import lombok.Data;

@Data
public class ArticleVisitStreamMess {
    /**
     * 文章id
     */
    private Long articleId;
    /**
     * 阅读
     */
    private Integer view;
    /**
     * 收藏
     */
    private Integer collect;
    /**
     * 评论
     */
    private Integer comment;
    /**
     * 点赞
     */
    private Integer like;
}