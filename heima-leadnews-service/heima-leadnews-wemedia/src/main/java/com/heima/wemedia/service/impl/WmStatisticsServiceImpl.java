package com.heima.wemedia.service.impl;

import com.heima.apis.article.IArticleClient;
import com.heima.apis.user.IUserClient;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.dtos.StatisticsDto;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.DateUtils;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


@Service
@Slf4j
public class WmStatisticsServiceImpl implements WmStatisticsService {

    @Autowired
    private IArticleClient articleClient;


    @Autowired
    private WmUserMapper wmUserMapper;

    @Override
    public ResponseResult newsDimension(String beginDate, String endDate) {

        // 获取该用户所有文章的指标累加

        if (StringUtils.isBlank(beginDate) || StringUtils.isBlank(endDate)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        // 获取当前用户app端的所有文章info
        List<ApArticle> apArticleList = articleClient.findAllArticle(user.getId(), beginDate, endDate);

        int likesNum = 0, collectNum = 0, publishNum = apArticleList.size();

        for (ApArticle apArticle : apArticleList) {
            if (apArticle.getLikes() != null) {
                likesNum += apArticle.getLikes();
            }
            if (apArticle.getCollection() != null) {
                collectNum += apArticle.getCollection();
            }
        }


        HashMap<String, Integer> resultMap = new HashMap<>();
        resultMap.put("likesNum", likesNum);
        resultMap.put("collectNum", collectNum);
        resultMap.put("publishNum", publishNum);
        return ResponseResult.okResult(resultMap);
    }

    @Override
    public PageResponseResult newsPage(StatisticsDto dto) {
        WmUser user = WmThreadLocalUtil.getUser();
        // 这里直接设置app端id
        dto.setWmUserId(user.getId());
        PageResponseResult responseResult = articleClient.newPage(dto);
        return responseResult;
    }
}
