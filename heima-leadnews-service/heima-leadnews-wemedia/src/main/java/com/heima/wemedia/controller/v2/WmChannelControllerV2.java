package com.heima.wemedia.controller.v2;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.wemedia.service.WmChannelService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/v1/channel")
@Slf4j
public class WmChannelControllerV2 {

    @Autowired
    private WmChannelService wmChannelService;

    @ApiOperation("频道查询")
    @GetMapping("/channels")
    public ResponseResult findAll() {
        log.error("错误测试");
        return wmChannelService.finaAll();
    }

}
