package com.heima.behavior.service;


import com.heima.model.behavior.dtos.ReadsBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ReadBehaviorService {


    ResponseResult readBehavior(ReadsBehaviorDto readsBehaviorDto);
}
