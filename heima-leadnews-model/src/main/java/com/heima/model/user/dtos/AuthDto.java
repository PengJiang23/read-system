package com.heima.model.user.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import com.heima.model.common.valid.ValidationGroup;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AuthDto  extends PageRequestDto {

    /**
     * 状态
     */
    private Short status;

    private Integer id;

    //驳回的信息
    @Size(message = "实名不通过原因",min = 10,groups = {ValidationGroup.AuthFail.class})
    @Size(message = "展示所有实名情况",min = 0,groups = {ValidationGroup.AuthInfo.class})
    @Size(message = "实名通过",min = 0,groups = {ValidationGroup.AuthPass.class})
    private String msg;

}
