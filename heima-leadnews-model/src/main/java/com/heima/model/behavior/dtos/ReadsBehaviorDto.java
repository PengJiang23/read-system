package com.heima.model.behavior.dtos;

import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

@Data
public class ReadsBehaviorDto {

    @IdEncrypt
    private Long articleId;


    private int count;

}
