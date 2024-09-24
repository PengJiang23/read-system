package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Message;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.search.vos.SearchArticleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SyncArticleListener {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /**
     * mq获取数据，构建es索引
     * @param message
     */
    @KafkaListener(topics = ArticleConstants.ARTICLE_ES_SYNC_TOPIC)
    public void onMessage(String message) {

        if (StringUtils.isNotBlank(message)) {
            log.info("SyncArticleListener,message={}", message);

            SearchArticleVo searchArticleVo = JSON.parseObject(message, SearchArticleVo.class);

            IndexRequest request = new IndexRequest("app_info_article");
            request.id(searchArticleVo.getId().toString());
            // kafka中的message也是序列化json格式，可以直接发到es中构建index
            request.source(message, XContentType.JSON);

            try {
                restHighLevelClient.index(request, RequestOptions.DEFAULT);
            }catch (IOException e){
                e.printStackTrace();
                log.error("sync es error={}", e);
            }
        }

    }


}
