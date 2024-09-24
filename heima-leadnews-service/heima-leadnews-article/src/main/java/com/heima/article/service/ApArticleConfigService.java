package com.heima.article.service;

import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.ResponseResult;

import java.util.Map;

public interface ApArticleConfigService {
    void updateByMap(Map map);

    ResponseResult updateCommentStatus(CommentConfigDto dto);
}
