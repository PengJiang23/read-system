package com.heima.article.controller.v1;


import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1/article")
@Api(value = "文章接口",tags = "app文章相关操作接口")
public class ArticleHomeController {

    @Autowired
    ApArticleService apArticleService;


    @ApiOperation(value = "加载",notes = "默认首页加载")
    @PostMapping("/load")
    public ResponseResult load(@RequestBody ArticleHomeDto articleHomeDto){
//        return apArticleService.load(ArticleConstants.LOADTYPE_LOAD_MORE,articleHomeDto);
        return apArticleService.load2(ArticleConstants.LOADTYPE_LOAD_MORE,articleHomeDto,true);
    }


    @ApiOperation(value = "加载更多",notes = "后续加载")
    @PostMapping("/loadmore")
    public ResponseResult loadmore(@RequestBody ArticleHomeDto articleHomeDto){
        return apArticleService.load(ArticleConstants.LOADTYPE_LOAD_MORE,articleHomeDto);
    }


    @ApiOperation(value = "加载最新", notes = "加载最新页面")
    @PostMapping("/loadnew")
    public ResponseResult loadnew(@RequestBody ArticleHomeDto articleHomeDto) {
        return apArticleService.load(ArticleConstants.LOADTYPE_LOAD_NEW, articleHomeDto);
    }


    /**
     * 用户行为数据回显
     * @param dto
     * @return
     */
    @PostMapping("/load_article_behavior")
    public ResponseResult loadBehavior(@RequestBody ArticleInfoDto dto) {
        return apArticleService.loadBehavior(dto);
    }

}
