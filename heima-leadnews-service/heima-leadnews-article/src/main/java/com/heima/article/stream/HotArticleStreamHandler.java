package com.heima.article.stream;


import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@Slf4j
public class HotArticleStreamHandler {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);

        /**
         * 1. 流中数据，转为原始obj
         * 2. 原始obj中以articleId为key， value为 behavior:add
         * 3. 对于一定时间范围内的流处理，根据文章id聚合，然后对同一篇的四种行为分别累加
         * 4. 累加的结果更新到articleMess对象中
         * 5. 发送到其他主题队列中
         */
        stream.map((key, value) -> {
                    UpdateArticleMess mess = JSON.parseObject((String) value, UpdateArticleMess.class);
                    return KeyValue.pair(mess.getArticleId().toString(), mess.getType().name() + "-" + mess.getAdd());
                })
                .groupBy((key, value) -> key)
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))

                // aggregate(初始化，聚合逻辑，聚合结果暂存)
                .aggregate(new Initializer<String>() {
                    // 初始化每篇文章行为都是0，也就是下面的aggvalue
                    @Override
                    public String apply() {
                        return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                    }
                }, new Aggregator<String, String, String>() {
                    // 根据每篇文章的所有value进行对应行为计数累加
                    @Override
                    public String apply(String key, String value, String aggValue) {
                        // value 用户行为空，直接返回
                        if (StringUtils.isBlank(value)) {
                            return aggValue;
                        }
                        // 解析aggvalue初始化字符串
                        String[] aggList = aggValue.split(",");
                        int col = 0, com = 0, like = 0, view = 0;
                        for (String agg : aggList) {
                            String[] split = agg.split(":");
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])) {
                                case COLLECTION:
                                    col = Integer.parseInt(split[1]);
                                    break;
                                case COMMENT:
                                    com = Integer.parseInt(split[1]);
                                    break;
                                case LIKES:
                                    like = Integer.parseInt(split[1]);
                                    break;
                                case VIEWS:
                                    view = Integer.parseInt(split[1]);
                                    break;
                            }
                        }

                        // 解析value，累加操作
                        String[] behavior = value.split("-");
                        log.error("获取到的kafka-stream的value"+value);
                        // 有可能value为空
                        if(behavior != null || behavior.length >= 2){
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(behavior[0])) {
                                case COLLECTION:
                                    col += Integer.parseInt(behavior[1]);
                                    break;
                                case COMMENT:
                                    com += Integer.parseInt(behavior[1]);
                                    break;
                                case LIKES:
                                    like += Integer.parseInt(behavior[1]);
                                    break;
                                case VIEWS:
                                    view += Integer.parseInt(behavior[1]);
                                    break;
                            }
                        }

                        String formatStr = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", col, com, like, view);
                        System.out.println("文章的id:" + key);
                        System.out.println("当前时间窗口内的消息处理结果：" + formatStr);
                        return formatStr;
                    }
                }, Materialized.as("hot-atricle-stream-count-001"))
                .toStream()
                .map((key, value) -> {
                    return new KeyValue<>(key.key().toString(), formatObj(key.key().toString(), value));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);

        return stream;
    }

    public String formatObj(String articleId, String value) {
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        mess.setArticleId(Long.valueOf(articleId));

        String[] behavior = value.split(",");
        for (String val : behavior) {
            String[] split = val.split(":");
            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])) {
                case COLLECTION:
                    mess.setCollect(Integer.parseInt(split[1]));
                    break;
                case COMMENT:
                    mess.setComment(Integer.parseInt(split[1]));
                    break;
                case LIKES:
                    mess.setLike(Integer.parseInt(split[1]));
                    break;
                case VIEWS:
                    mess.setView(Integer.parseInt(split[1]));
                    break;
            }
        }

        log.info("聚合消息处理之后的结果为:{}", JSON.toJSONString(mess));
        return JSON.toJSONString(mess);
    }


}
