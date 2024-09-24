package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.objenesis.instantiator.basic.FailingInstantiator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * es查询
     *
     * @param
     * @return
     * @throws IOException
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {

        // 参数校验
        if (dto == null || StringUtils.isBlank(dto.getSearchWords())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }


        ApUser user = AppThreadLocalUtil.getUser();

        if(user != null && dto.getFromIndex() == 0){
            // 异步保存用户搜索历史
            apUserSearchService.insert(dto.getSearchWords(),user.getId());
        }

        // 构造请求+查询builder
        SearchRequest request = new SearchRequest("app_info_article");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();


        sourceBuilder.query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.queryStringQuery(dto.getSearchWords())
                                .field("title")
                                .field("content")
                                .defaultOperator(Operator.OR))
                        .filter(QueryBuilders.rangeQuery("publishTime")
                                .lt(dto.getMinBehotTime().getTime())))
                .from(0)
                .size(dto.getPageSize())
                .sort("publishTime", SortOrder.DESC)
                .highlighter(new HighlightBuilder().field("title")
                        .preTags("<font style='color: blue; font-size: inherit;'>")
                        .postTags("</font>"));


        request.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(request, RequestOptions.DEFAULT);


        //3.结果封装返回

        List<Map> list = new ArrayList<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class);
            //处理高亮
            if (hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0) {
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String title = StringUtils.join(titles);
                //高亮标题
                map.put("h_title", title);
            } else {
                //原始标题
                map.put("h_title", map.get("title"));
            }
            list.add(map);
        }

        return ResponseResult.okResult(list);

    }
}
