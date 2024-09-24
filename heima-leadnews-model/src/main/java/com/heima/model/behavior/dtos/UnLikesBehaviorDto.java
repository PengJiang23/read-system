package com.heima.model.behavior.dtos;


import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

@Data
public class UnLikesBehaviorDto {


    /**
     * 文章id
     */
    @IdEncrypt
    private Long articleId;



    /**
     * 0 不喜欢 1 取消不喜欢
     */
    private Short type;

}
