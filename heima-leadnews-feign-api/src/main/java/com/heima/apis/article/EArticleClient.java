package com.heima.apis.article;


import com.heima.apis.article.fallback.EArticleClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 本地控制器已经实现的远程方法
 */
@FeignClient(value = "leadnews-article",contextId = "EArticle", fallbackFactory = EArticleClientFallbackFactory.class)
public interface EArticleClient {

    @GetMapping("/api/v1/article/test")
    public String test();

}
