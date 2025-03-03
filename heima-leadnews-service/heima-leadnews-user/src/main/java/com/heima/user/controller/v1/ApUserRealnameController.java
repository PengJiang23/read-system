package com.heima.user.controller.v1;

import com.heima.common.constants.UserConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.valid.ValidationGroup;
import com.heima.model.user.dtos.AuthDto;
import com.heima.user.service.ApUserRealnameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class ApUserRealnameController {

    @Autowired
    private ApUserRealnameService apUserRealnameService;


    @PostMapping("/list")
    public ResponseResult loadListByStatus(@RequestBody @Validated(ValidationGroup.AuthInfo.class) AuthDto dto){
        return apUserRealnameService.loadListByStatus(dto);
    }

    @PostMapping("/authPass")
    public ResponseResult authPass(@RequestBody  @Validated(ValidationGroup.AuthPass.class) AuthDto dto ){
        return apUserRealnameService.updateStatus(dto, UserConstants.PASS_AUTH);
    }

    @PostMapping("/authFail")
    public ResponseResult authFail(@RequestBody  @Validated(ValidationGroup.AuthFail.class) AuthDto dto ){
        return apUserRealnameService.updateStatus(dto, UserConstants.FAIL_AUTH);
    }

}