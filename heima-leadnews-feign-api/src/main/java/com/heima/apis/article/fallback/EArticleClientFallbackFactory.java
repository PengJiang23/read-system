package com.heima.apis.article.fallback;

import com.heima.apis.article.EArticleClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class EArticleClientFallbackFactory implements FallbackFactory<EArticleClient> {
    @Override
    public EArticleClient create(Throwable throwable) {
        return new EArticleClient() {
            @Override
            public String test() {

                return throwable.getMessage() + " " + throwable;
            }
        };
    }
}
