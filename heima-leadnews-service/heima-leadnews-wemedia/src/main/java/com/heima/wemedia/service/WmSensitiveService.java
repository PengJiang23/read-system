package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService extends IService<WmSensitive> {
    ResponseResult deleteSensitive(Integer id);

    ResponseResult listSensitive(SensitiveDto sensitiveDto);

    ResponseResult insertSensitive(WmSensitive wmSensitive);

    ResponseResult updateSensitive(WmSensitive wmSensitive);
}
