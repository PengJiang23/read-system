package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApCollectionService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.data.Json;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApCollectionServiceImpl implements ApCollectionService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Override
    public ResponseResult collection(CollectionBehaviorDto dto) {
        //条件判断
        if (dto == null || dto.getEntryId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        // 构建对象，发送到kafka
        UpdateArticleMess updateArticleMess = new UpdateArticleMess();
        updateArticleMess.setType(UpdateArticleMess.UpdateArticleType.COLLECTION);
        updateArticleMess.setArticleId(dto.getEntryId());

        // 是否重复触发收藏事件
        String articleBehaviorJSON = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR_KEY + user.getId(), dto.getEntryId().toString());
        if (StringUtils.isNotBlank(articleBehaviorJSON) && dto.getOperation() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "已收藏");
        }


        // 收藏/取消收藏
        if (dto.getOperation() == 0) {
            cacheService.hPut(BehaviorConstants.COLLECTION_BEHAVIOR_KEY + user.getId(), dto.getEntryId().toString(), JSON.toJSONString(dto));
            updateArticleMess.setAdd(1);
            log.info("文章收藏，保存key:{},{},{}", dto.getEntryId(), user.getId().toString(), JSON.toJSONString(dto));
        } else {
            cacheService.hDelete(BehaviorConstants.COLLECTION_BEHAVIOR_KEY + user.getId(), dto.getEntryId().toString());
            updateArticleMess.setAdd(-1);
            log.info("文章收藏，删除key:{},{},{}", dto.getEntryId(), user.getId().toString(), JSON.toJSONString(dto));
        }

        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(updateArticleMess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}