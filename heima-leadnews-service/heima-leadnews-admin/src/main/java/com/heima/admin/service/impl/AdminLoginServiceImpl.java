package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdminLoginMapper;
import com.heima.admin.service.AdminLoginService;
import com.heima.model.admin.dtos.AdUserDto;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j

public class AdminLoginServiceImpl extends ServiceImpl<AdminLoginMapper, AdUser> implements AdminLoginService {

    @Autowired
    private AdminLoginMapper adminLoginMapper;

    @Override
    public ResponseResult login(AdUserDto dto) {

        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户名或密码为空");
        }

        AdUser adUser = adminLoginMapper.selectOne(new LambdaQueryWrapper<AdUser>()
                .eq(AdUser::getName, dto.getName()));
        if(adUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 校验密码
        String salt = adUser.getSalt();
        String password = dto.getPassword();

        String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes());

        // 密码错误返回
        if (!pswd.equals(adUser.getPassword())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }

        // 为登录用户生成token
        String token = AppJwtUtil.getToken(adUser.getId().longValue());
        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);
        adUser.setPassword("");
        adUser.setSalt("");
        map.put("user", adUser);
        return ResponseResult.okResult(map);

    }
}
