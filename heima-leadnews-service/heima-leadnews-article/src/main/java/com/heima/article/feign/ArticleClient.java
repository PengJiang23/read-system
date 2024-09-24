package com.heima.article.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.apis.article.IArticleClient;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.article.service.ApArticleService;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.CollectionBehaviorDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.vos.ArticleCommnetVo;
import com.heima.model.article.vos.ArticleInfoVo;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.StatisticsDto;
import com.heima.utils.common.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@Slf4j
public class ArticleClient implements IArticleClient {

    @Autowired
    private ApArticleService apArticleService;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleConfigService apArticleConfigService;


    @Override
    @PostMapping("/api/v1/article/save")
    public ResponseResult saveArticle(@RequestBody ArticleDto dto) {
        return apArticleService.saveArticle(dto);
    }


    @GetMapping("/api/v1/article/findArticleConfigByArticleId/{articleId}")
    @Override
    public ResponseResult findArticleConfigByArticleId(@PathVariable("articleId") Long articleId) {
        return ResponseResult.okResult(apArticleConfigMapper.selectOne(new LambdaQueryWrapper<ApArticleConfig>().eq(ApArticleConfig::getArticleId, articleId)));
    }

    @PostMapping("/api/v1/article/findNewsComments")
    @Override
    public PageResponseResult findNewsComments(ArticleCommentDto dto) {
        return apArticleService.findNewsComments(dto);
    }


    @PostMapping("/api/v1/article/updateCommentStatus")
    @Override
    public ResponseResult updateCommentStatus(CommentConfigDto dto) {
        return apArticleConfigService.updateCommentStatus(dto);
    }


    @GetMapping("/api/v1/article/findAllArticle/{apUserId}/{beginDate}/{endDate}")
    @Override
    public List<ApArticle> findAllArticle(@PathVariable("apUserId") Integer apUserId, @PathVariable("beginDate") String beginDate, @PathVariable("endDate") String endDate) {

        Date beginDate1 = DateUtils.stringToDate(beginDate);
        Date endDate1 = DateUtils.stringToDate(endDate);
        return apArticleService.findAllArticle(apUserId,beginDate1,endDate1);
    }


    @Override
    public PageResponseResult newPage(StatisticsDto dto) {

//        dto.checkParam();

//        // 查询分页数据，自定义实现，需要手动设置分页数和total之类的
//        Integer currentPage = dto.getPage();
//        dto.setPage((dto.getPage()-1)*dto.getSize());
//
//
//        // 获取所有文章评论详情
//        List<ArticleInfoVo> list = apArticleMapper.findArticle(dto);
//        // 获取当前用户的所有文章的数量
//        int count = apArticleMapper.findArticleCount(dto);
//
//        PageResponseResult responseResult = new PageResponseResult(currentPage,dto.getSize(),count);
//        responseResult.setData(list);


        return apArticleService.newPage(dto);
    }

}
