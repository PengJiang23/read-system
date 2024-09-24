package com.heima.wemedia.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {

    ResponseResult finaList(WmNewsPageReqDto wmNewsPageReqDto);

    ResponseResult submitOrUpdate(WmNewsDto wmNewsDto);

    ResponseResult editNews(Integer newsId);

    ResponseResult deleteNews(Integer newsId);

    ResponseResult downOrUpNews(WmNewsDto wmNewsDto);

    ResponseResult finaNewsList(NewsAuthDto newsAuthDto);

    ResponseResult findWmNewsVo(Integer id);

    ResponseResult updateStatus(Short wmNewsAuthPass, NewsAuthDto dto);
}
