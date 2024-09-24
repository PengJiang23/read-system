package com.heima.model.user.dtos;

import com.heima.model.common.annotation.AuthStatusValue;
import com.heima.model.common.dtos.PageRequestDto;
import com.heima.model.common.valid.ValidationGroup;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AuthDto  extends PageRequestDto {

    /**
     * 状态
     *      0 创建中
     *     1 待审核
     *     2 审核失败
     *     9 审核通过
     */
//    @AuthStatusValue(status = {0,1,2,9},message = "状态字段没有遵循规则",groups = {ValidationGroup.AuthFail.class})
    private Short status;

    private Integer id;

    //驳回的信息
    @Size(message = "驳回原因最少10个字",min = 10,groups = {ValidationGroup.AuthFail.class})
    @Size(message = "展示所有实名情况",min = 0,groups = {ValidationGroup.AuthInfo.class})
    @Size(message = "实名通过",min = 0,groups = {ValidationGroup.AuthPass.class})
    private String msg;

}
