package com.heima.model.user.dtos;

import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

@Data
public class UserRelationDto {

    /**
     * 文章ID
     */
    @IdEncrypt
    private Long articleId;


    /**
     * 作者ID
     */
    @IdEncrypt
    private Integer authorId;


    /**
     * 操作类型 0 关注 1 取消关注
     */
    private Short operation;

}
