package com.heima.wemedia.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {

    ResponseResult finaAll();

    ResponseResult delete(Integer id);

    ResponseResult listChannel(ChannelDto channelDto);

    ResponseResult insert(WmChannel wmChannel);

    ResponseResult updateObj(WmChannel wmChannel);
}
