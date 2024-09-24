package com.heima.model.article.dtos;


import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

@Data
public class ArticleInfoDto {

    @IdEncrypt
    Integer equipmentId;

    @IdEncrypt
    private Long articleId;

    @IdEncrypt
    private Integer authorId;

}
