package com.heima.behavior.service;


import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface LikeBehaviorService {
    ResponseResult likeBehavior(LikesBehaviorDto likesBehaviorDto);
}
