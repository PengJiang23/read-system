package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService {

    /**
     * 更新敏感词
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult updateSensitive(WmSensitive wmSensitive) {

        if (wmSensitive == null || wmSensitive.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmSensitive dbSensitive = getOne(new LambdaQueryWrapper<WmSensitive>().eq(WmSensitive::getSensitives, wmSensitive.getSensitives()));
        if(dbSensitive != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"更新的敏感词已存在");
        }

        updateById(wmSensitive);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 新增，如果存在相同敏感词则不能新增
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult insertSensitive(WmSensitive wmSensitive) {

        if (wmSensitive == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmSensitive sensitive = getOne(new LambdaQueryWrapper<WmSensitive>().eq(WmSensitive::getSensitives, wmSensitive.getSensitives()));
        if (sensitive != null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "关键词已存在");
        }

        wmSensitive.setCreatedTime(new Date());
        save(wmSensitive);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询所有操作+条件查询：关键词模糊查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult listSensitive(SensitiveDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 检查分页参数
        dto.checkParam();
        // 构建分页对象+得到查询结果
        Page page = new Page(dto.getPage(), dto.getSize());
        Page result = page(page, new LambdaQueryWrapper<WmSensitive>()
                .like(dto.getName() != null, WmSensitive::getSensitives, dto.getName())
                .orderByDesc(WmSensitive::getCreatedTime));

        // 构造返回view层的分页数据
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(result.getRecords());

        return responseResult;
    }

    /**
     * 删除操作
     * 不存在，也不能删除
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult deleteSensitive(Integer id) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmSensitive wmSensitive = getById(id);
        if (wmSensitive == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
