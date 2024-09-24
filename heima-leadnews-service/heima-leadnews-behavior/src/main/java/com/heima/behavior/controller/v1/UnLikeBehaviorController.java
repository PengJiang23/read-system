package com.heima.behavior.controller.v1;

import com.heima.behavior.service.LikeBehaviorService;
import com.heima.behavior.service.UnLikeBehaviorService;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class UnLikeBehaviorController {

    @Autowired
    private UnLikeBehaviorService unLikeBehaviorService;

    @PostMapping("/un_likes_behavior")
    public ResponseResult unLikeBehavior(@RequestBody UnLikesBehaviorDto unLikesBehaviorDto){
        return unLikeBehaviorService.unLikeBehavior(unLikesBehaviorDto);
    }

}
