package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.zip.Checksum;


@Service
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    WmMaterialMapper wmMaterialMapper;

    @Autowired
    FileStorageService fileStorageService;

    @Override
    public ResponseResult uploadPicture(MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        String originalFilename = file.getOriginalFilename();
        String filename = UUID.randomUUID().toString().replace("-", "");
        // 获取文件后缀 lastindexof最后一次出现索引，substring开启切割位置
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));

        String fileUrl = null;
        try {
            // 上传到minio中的文件，名字不能是原来的
            fileUrl = fileStorageService.uploadImgFile("", filename + postfix, file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("图片上传minio失败");
        }


        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setType((short)0);
        wmMaterial.setUrl(fileUrl);
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        return ResponseResult.okResult(wmMaterial);

    }


    @Override
    public ResponseResult findList(WmMaterialDto dto) {

        // 分页查询，参数错误，默认设置查询第一页数据
        dto.checkParam();

        Page page = new Page(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();

        if (dto.getIsCollection() != null && dto.getIsCollection() == 1) {
            queryWrapper.eq(WmMaterial::getIsCollection, dto.getIsCollection());
        }

        queryWrapper.eq(WmMaterial::getUserId, WmThreadLocalUtil.getUser().getId());
        queryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        Page result = page(page, queryWrapper);

        log.error(String.valueOf(result.getTotal()));
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;

    }


    @Override
    public ResponseResult deletePicture(Integer id) {

        // 校验前端传入参数
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmMaterial wmMaterial = wmMaterialMapper.selectOne(new LambdaQueryWrapper<WmMaterial>()
                .eq(WmMaterial::getUserId, WmThreadLocalUtil.getUser().getId())
                .eq(WmMaterial::getId, id));

        // 图片在素材库？
        if (wmMaterial == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        // 图片数据库  +  minio删除（minio不一定，可以看一下数据库是否存在其他使用/记录，不存在则删除）
        int deleteStatus = wmMaterialMapper.deleteById(wmMaterial);
        // todo minio图片是否删除 其次 该用户文章中有这个图片是否允许删除/提醒
//        fileStorageService.delete(wmMaterial.getUrl());

        if (deleteStatus == 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.SUCCESS);
        }

        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
    }


    @Override
    public ResponseResult collectPicture(Integer id) {

        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmMaterial material = new WmMaterial();
        material.setId(id);
        material.setUserId(WmThreadLocalUtil.getUser().getId());
        material.setIsCollection((short) 1);
        int i = wmMaterialMapper.updateById(material);
        if (i == 1) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        return ResponseResult.okResult(AppHttpCodeEnum.PARAM_INVALID);
    }

    @Override
    public ResponseResult cancelCollectPicture(Integer id) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmMaterial material = new WmMaterial();
        material.setId(id);
        material.setUserId(WmThreadLocalUtil.getUser().getId());
        material.setIsCollection((short) 0);
        int i = wmMaterialMapper.updateById(material);
        if (i == 1) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        return ResponseResult.okResult(AppHttpCodeEnum.PARAM_INVALID);
    }
}
