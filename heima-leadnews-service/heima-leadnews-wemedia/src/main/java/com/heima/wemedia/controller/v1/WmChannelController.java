package com.heima.wemedia.controller.v1;

import com.heima.apis.article.EArticleClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
public class WmChannelController {

    @Autowired
    private WmChannelService wmChannelService;

    @ApiOperation("频道查询")
    @GetMapping("/channels")
    public ResponseResult findAll() {
        return wmChannelService.finaAll();
    }

    @PostMapping("/save")
    public ResponseResult save(@RequestBody WmChannel wmChannel) {
        return wmChannelService.insert(wmChannel);
    }

    @PostMapping("/update")
    public ResponseResult update(@RequestBody WmChannel wmChannel) {
        return wmChannelService.updateObj(wmChannel);
    }

    @GetMapping("/del/{id}")
    public ResponseResult delete(@PathVariable("id") Integer id) {
        return wmChannelService.delete(id);
    }

    @PostMapping("/list")
    public ResponseResult list(@RequestBody ChannelDto channelDto) {
        return wmChannelService.listChannel(channelDto);
    }

    @Autowired
    private EArticleClient eArticleClient;

    @GetMapping("/test1")
    public String testFeign(){
        return eArticleClient.test();
    }


}
