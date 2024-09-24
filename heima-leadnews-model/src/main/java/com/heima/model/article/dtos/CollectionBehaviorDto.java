package com.heima.model.article.dtos;

import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

import java.util.Date;

@Data
public class CollectionBehaviorDto {


    /**
     * 文章id
     */
    @IdEncrypt
    private Long entryId;

    /**
     * 0收藏    1取消收藏
     */
    private Short operation;

    private Date publishedTime;

    /**
     * 0文章    1动态
     */
    private Short type;
}
