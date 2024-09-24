package com.heima.wemedia.controller.v1;


import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.wemedia.service.WmMaterialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/material")
@AllArgsConstructor
@Api(value = "文件上传")
public class WmMaterialController {


    private final WmMaterialService wmMaterialService;

    @ApiOperation(value = "图片上传")
    @PostMapping("/upload_picture")
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        return wmMaterialService.uploadPicture(multipartFile);
    }


    @ApiOperation("素材库查询")
    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmMaterialDto wmMaterialDto) {
        return wmMaterialService.findList(wmMaterialDto);
    }


    @ApiOperation("图片删除")
    @GetMapping("/del_picture/{id}")
    public ResponseResult deletePicture(@PathVariable("id") Integer id) {
        return wmMaterialService.deletePicture(id);
    }


    @ApiOperation("图片收藏")
    @GetMapping("/collect/{id}")
    public ResponseResult CollectPicture(@PathVariable("id") Integer id) {
        return wmMaterialService.collectPicture(id);
    }

    @ApiOperation("图片收藏取消")
    @GetMapping("/cancel_collect/{id}")
    public ResponseResult cancelCollectPicture(@PathVariable("id") Integer id) {
        return wmMaterialService.cancelCollectPicture(id);
    }

}
