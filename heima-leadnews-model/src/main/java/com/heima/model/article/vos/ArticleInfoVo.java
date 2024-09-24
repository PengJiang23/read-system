package com.heima.model.article.vos;

import lombok.Data;

import java.util.Date;

@Data
public class ArticleInfoVo {

    private Long id;

    private String title;

    private Integer comment;
    private Integer likes;
    private Integer collection;
    private Integer views;
}
