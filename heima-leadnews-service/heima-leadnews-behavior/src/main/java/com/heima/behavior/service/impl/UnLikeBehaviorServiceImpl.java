package com.heima.behavior.service.impl;


import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.UnLikeBehaviorService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UnLikeBehaviorServiceImpl implements UnLikeBehaviorService {


    @Autowired
    private CacheService cacheService;


    @Override
    public ResponseResult unLikeBehavior(UnLikesBehaviorDto dto) {

        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        if (dto.getType().equals(BehaviorConstants.IS_UNLIKE_BEHAVIOR)) {
            Object obj = cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
            if (obj != null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户已不喜欢");
            }

            log.info("保存当前key:{} ,{}, {}", dto.getArticleId(), user.getId(), dto);
            cacheService.hPut(BehaviorConstants.UN_LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString().toString(), JSON.toJSONString(dto));
        } else {
            // 删除当前key
            log.info("删除当前key:{}, {}", dto.getArticleId(), user.getId());
            cacheService.hDelete(BehaviorConstants.UN_LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
        }


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
