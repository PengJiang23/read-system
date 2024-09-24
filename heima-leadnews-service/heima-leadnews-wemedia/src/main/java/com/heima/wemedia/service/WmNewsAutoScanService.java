package com.heima.wemedia.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.scheduling.annotation.Async;

public interface WmNewsAutoScanService {

    /**
     * 文章审核
     * @param id
     */
//    @Async
    public void autoScanWmNews(Integer id);

    ResponseResult saveAppArticle(WmNews wmNews);
}
