package com.heima.wemedia.controller.v1;

import com.heima.common.constants.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;

    @ApiOperation("文章查询")
    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmNewsPageReqDto wmNewsPageReqDto) {
        return wmNewsService.finaList(wmNewsPageReqDto);
    }

    @ApiOperation(value = "文章接口", tags = "用户提交/修改文章内容")
    @PostMapping("/submit")
    public ResponseResult submitOrUpdate(@RequestBody WmNewsDto wmNewsDto){
        return wmNewsService.submitOrUpdate(wmNewsDto);
    }

    @ApiOperation(value = "文章编辑", tags = "用户触发可以进入文章内容展示界面")
    @GetMapping("/one/{id}")
    public ResponseResult editNews(@PathVariable("id") Integer newsId){
        return wmNewsService.editNews(newsId);
    }


    @ApiOperation(value = "文章删除", tags = "删除文章")
    @GetMapping("/del_news/{id}")
    public ResponseResult deleteNews(@PathVariable("id") Integer newsId){
        return wmNewsService.deleteNews(newsId);
    }


    @ApiOperation(value = "文章下架")
    @PostMapping("/down_or_up")
    public ResponseResult downOrUpNews(@RequestBody WmNewsDto wmNewsDto){
        return wmNewsService.downOrUpNews(wmNewsDto);
    }



    // ----------------admin端的文章人工审核接口--------------------

    // 获取所有文章+条件查询
    @PostMapping("/list_vo")
    public ResponseResult listNewsVo(@RequestBody NewsAuthDto newsAuthDto){
        return wmNewsService.finaNewsList(newsAuthDto);
    }


    // 查询指定文章内容
    @GetMapping("/one_vo/{id}")
    public  ResponseResult getOneNews(@PathVariable("id") Integer id){
        return wmNewsService.findWmNewsVo(id);
    }

    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_PASS,dto);
    }

    @PostMapping("/auth_fail")
    public ResponseResult authFail(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_FAIL,dto);
    }



}
