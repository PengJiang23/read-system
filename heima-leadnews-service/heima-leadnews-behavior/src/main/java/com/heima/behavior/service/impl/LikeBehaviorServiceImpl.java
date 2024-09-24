package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.LikeBehaviorService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
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
public class LikeBehaviorServiceImpl implements LikeBehaviorService {


    @Autowired
    private CacheService cacheService;


    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 点赞
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult likeBehavior(LikesBehaviorDto dto) {

        // 检查参数，文章是否存在
        // todo 可以redis定时任务统计好，点赞数数据，然后mq异步更新到数据库中 文章状态点赞数
        // todo 在定时任务完成
        // 更新文章收藏表，点赞内容类型0 or 1；如果是评论需要存在mongodb

        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        // 构建对象，发送到kafka
        UpdateArticleMess updateArticleMess = new UpdateArticleMess();
        updateArticleMess.setType(UpdateArticleMess.UpdateArticleType.LIKES);
        updateArticleMess.setArticleId(dto.getArticleId());

        // redis和kafka中消息处理逻辑
        if (dto.getOperation() == BehaviorConstants.LIKE_BEHAVIOR) {
            Object obj = cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());

            if (obj != null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户已点赞");
            }

            log.info("保存当前key:{} ,{}, {}", dto.getArticleId(), user.getId(), dto);
            // redis
            cacheService.hPut(BehaviorConstants.LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString().toString(), JSON.toJSONString(dto));
            // 消息体处理
            updateArticleMess.setAdd(1);

        } else {
            // 删除当前key
            log.info("删除当前key:{}, {}", dto.getArticleId(), user.getId());
            cacheService.hDelete(BehaviorConstants.LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
            updateArticleMess.setAdd(-1);
        }

        // 消息发送
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(updateArticleMess));
        log.error("kafka发送消息");

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
