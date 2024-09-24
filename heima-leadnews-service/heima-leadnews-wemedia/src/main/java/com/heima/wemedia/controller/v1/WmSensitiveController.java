package com.heima.wemedia.controller.v1;


import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.service.WmSensitiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensitive")
@Api("admin端-敏感词管理")
public class WmSensitiveController {

    @Autowired
    private WmSensitiveService wmSensitiveService;

    @PostMapping("/update")
    public ResponseResult update(@RequestBody WmSensitive wmSensitive) {
        return wmSensitiveService.updateSensitive(wmSensitive);
    }

    /**
     * 新增敏感词
     *
     * @param wmSensitive
     * @return
     */
    @PostMapping("/save")
    public ResponseResult insertSensitive(@RequestBody WmSensitive wmSensitive) {
        return wmSensitiveService.insertSensitive(wmSensitive);
    }


    /**
     * 查询敏感词列表，分页
     *
     * @param sensitiveDto
     * @return
     */
    @PostMapping("/list")
    public ResponseResult listSensitive(@RequestBody SensitiveDto sensitiveDto) {

        return wmSensitiveService.listSensitive(sensitiveDto);
    }


    /**
     * 删除敏感词
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "删除敏感词")
    @DeleteMapping("/del/{id}")
    public ResponseResult deleteSensitive(@PathVariable Integer id) {

        return wmSensitiveService.deleteSensitive(id);
    }


}
