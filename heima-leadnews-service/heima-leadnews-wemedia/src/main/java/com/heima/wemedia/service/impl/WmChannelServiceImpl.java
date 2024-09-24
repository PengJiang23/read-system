package com.heima.wemedia.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.pojos.*;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmNewsService wmNewsService;


    /**
     * 自媒体端查询所有频道
     *
     * @return
     */
    @Override
    public ResponseResult finaAll() {

        List<WmChannel> wmChannelList = wmChannelMapper.selectList(
                new LambdaQueryWrapper<WmChannel>()
                        .orderByDesc(WmChannel::getCreatedTime));

        // list() 查询所有，自带接口
        return ResponseResult.okResult(wmChannelList);
    }

    // -------------------------------以下为admin端的频道操作----------------------------------------


    /**
     * 新增频道
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult insert(WmChannel wmChannel) {

        if (wmChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmChannel dbChannel = wmChannelMapper.selectOne(new LambdaQueryWrapper<WmChannel>().eq(WmChannel::getName, wmChannel.getName()));

        if (dbChannel != null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "该频道已存在，不可重复添加");
        }

        save(wmChannel);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * 修改频道信息
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult updateObj(WmChannel wmChannel) {

        if (wmChannel == null || wmChannel.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        int count = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, wmChannel.getId()));

        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道被引用不能修改或禁用");
        }

        //2.修改
        updateById(wmChannel);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * 查询所有频道+条件查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult listChannel(ChannelDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "传入参数为空");
        }

        dto.checkParam();
        Page page = new Page(dto.getPage(), dto.getSize());
        Page result = page(page, new LambdaQueryWrapper<WmChannel>().like(StringUtils.isNoneBlank(dto.getName()), WmChannel::getName, dto.getName())
                .orderByDesc(WmChannel::getCreatedTime)

        );

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(result.getRecords());
        return responseResult;


    }


    /**
     * 删除频道
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult delete(Integer id) {

        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmChannel wmChannel = wmChannelMapper.selectById(id);
        if (wmChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "删除的频道不存在");
        }

        if (!wmChannel.getStatus().equals(false)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "不可删除启用的频道");
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
