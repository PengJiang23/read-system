package com.heima.user.service.impl;

import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserFollowService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ApUserFollowServiceImpl implements ApUserFollowService {

    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult follow(UserRelationDto dto) {

        if (dto.getOperation() == null || dto.getOperation() < 0 || dto.getOperation() > 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        Integer userId = user.getId();
        Integer followUserId = dto.getAuthorId();


        // 关注
        if (dto.getOperation() == 0) {
            // 用户关注表
            cacheService.zAdd(BehaviorConstants.APUSER_FOLLOW_RELATION_KEY + userId, followUserId.toString(), System.currentTimeMillis());
            // 被关注用户的粉丝表
            cacheService.zAdd(BehaviorConstants.APUSER_FANS_RELATION_KEY + followUserId, userId.toString(), System.currentTimeMillis());

        } else {

            // 用户关注表
            cacheService.zRemove(BehaviorConstants.APUSER_FOLLOW_RELATION_KEY + userId, followUserId.toString());
            // 被关注用户的粉丝表
            cacheService.zRemove(BehaviorConstants.APUSER_FANS_RELATION_KEY + followUserId, userId.toString());
        }


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
