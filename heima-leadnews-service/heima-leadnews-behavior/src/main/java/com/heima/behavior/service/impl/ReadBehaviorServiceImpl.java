package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ReadBehaviorService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.ReadsBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ReadBehaviorServiceImpl implements ReadBehaviorService {


    @Autowired
    private CacheService cacheService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Override
    public ResponseResult readBehavior(ReadsBehaviorDto dto) {


        // 校验参数
        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 用户登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        // 构建msg对象，发送到kafka
        UpdateArticleMess updateArticleMess = new UpdateArticleMess();
        updateArticleMess.setType(UpdateArticleMess.UpdateArticleType.VIEWS);
        updateArticleMess.setArticleId(dto.getArticleId());

        String readDtoJSON = (String) cacheService.hGet(BehaviorConstants.READ_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
        if (StringUtils.isNotBlank(readDtoJSON)) {
            ReadsBehaviorDto readsBehaviorDto = JSON.parseObject(readDtoJSON, ReadsBehaviorDto.class);
            dto.setCount((short) (dto.getCount() + readsBehaviorDto.getCount()));
        }


        log.info("保存当前key:{} ,{}, {}", dto.getArticleId(), user.getId(), dto);
        cacheService.hPut(BehaviorConstants.READ_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString(), JSON.toJSONString(dto));
        updateArticleMess.setAdd(1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(updateArticleMess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS.getCode(), "阅读次数+1");
    }
}
