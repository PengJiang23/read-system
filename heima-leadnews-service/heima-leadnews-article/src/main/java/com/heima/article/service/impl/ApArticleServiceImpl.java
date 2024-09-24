package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.ArticleBehaviorVO;
import com.heima.model.article.vos.ArticleCommnetVo;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.comment.dtos.CommentConfigDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.dtos.StatisticsDto;
import com.heima.utils.common.DateUtils;
import com.heima.utils.thread.AppThreadLocalUtil;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    private final static short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;


    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDto articleHomeDto) {

        Integer size = articleHomeDto.getSize();
        if (size == null || size == 0) {
            size = 10;
        }

        size = Math.min(size, MAX_PAGE_SIZE);
        articleHomeDto.setSize(size);

        if (!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }

        if (StringUtils.isEmpty(articleHomeDto.getTag())) {
            articleHomeDto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        if (articleHomeDto.getMaxBehotTime() == null) articleHomeDto.setMaxBehotTime(new Date());
        if (articleHomeDto.getMaxBehotTime() == null) articleHomeDto.setMinBehotTime(new Date());

        List<ApArticle> apArticleList = apArticleMapper.loadArticleList(articleHomeDto, loadtype);

        return ResponseResult.okResult(apArticleList);
    }


    /**
     * 审核通过以后，需要将文章同步到app端
     * 同时生成静态文件
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional
    public ResponseResult saveArticle(ArticleDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto, apArticle);

        if (dto.getId() == null) {
            save(apArticle);

            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            ApArticleContent articleContent = new ApArticleContent();
            articleContent.setArticleId(apArticle.getId());
            articleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(articleContent);
        } else {

            updateById(apArticle);

            ApArticleContent articleContent = apArticleContentMapper.selectOne(new LambdaQueryWrapper<ApArticleContent>().eq(ApArticleContent::getArticleId, dto.getId()));
            articleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(articleContent);

        }


        // 生成静态文件
        articleFreemarkerService.buildArticleToMinIO(apArticle, dto.getContent());

        return ResponseResult.okResult(apArticle.getId());
    }

    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult loadBehavior(ArticleInfoDto dto) {

        // 参数校验
        if (dto == null || dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        // hashmap init，返回文章相关behavior
        Map<String, Object> resultMap = new HashMap<>();
        boolean isfollow = false, islike = false, isunlike = false, iscollection = false;


        String likeObj = (String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
        log.error(BehaviorConstants.LIKE_BEHAVIOR_KEY + dto.getArticleId().toString());
        log.error(likeObj);
        if (StringUtils.isNotBlank(likeObj)) {
            // 取消喜欢
            islike = true;
        }

        String unLikeObj = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR_KEY + dto.getArticleId().toString(), user.getId().toString());
        if (StringUtils.isNotBlank(unLikeObj)) {
            isunlike = true;
        }

        String collectionJSON = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR_KEY + user.getId(), dto.getArticleId().toString());
        if (StringUtils.isNotBlank(collectionJSON)) {
            iscollection = true;
        }

        Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION_KEY + user.getId().toString(), dto.getAuthorId().toString());
        if (score != null) {
            isfollow = true;
        }

        resultMap.put("isfollow", isfollow);
        resultMap.put("islike", islike);
        resultMap.put("isunlike", isunlike);
        resultMap.put("iscollection", iscollection);

        return ResponseResult.okResult(resultMap);
    }


    /**
     * redis加载热点文章
     * 否则从db加载
     *
     * @param loadType
     * @param dto
     * @param firstPage
     * @return
     */
    @Override
    public ResponseResult load2(Short loadType, ArticleHomeDto dto, boolean firstPage) {
        if (firstPage) {
            String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(jsonStr)) {
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                ResponseResult responseResult = ResponseResult.okResult(hotArticleVoList);
                return responseResult;
            }
        }
        return load(loadType, dto);
    }


    /**
     * 从kafka获取文章的用户行为数据保存到db和redis
     *
     * @param mess
     */
    @Override
    public void updateScore(ArticleVisitStreamMess mess) {

        ApArticle apArticle = updateArticle(mess);

        Integer score = computeScore(apArticle);
        score = score * 4;

        //3.替换当前文章对应频道的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

        //4.替换推荐对应的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);

    }

    private void replaceDataToRedis(ApArticle apArticle, Integer score, String key) {
        // 获取该频道的热点文章
        String articleListStr = cacheService.get(key);
        if (StringUtils.isNotBlank(articleListStr)) {
            List<HotArticleVo> hotArticleVos = JSON.parseArray(articleListStr, HotArticleVo.class);
            // 判断当前文章是否在热点文章中
            boolean flag = true;
            for (HotArticleVo hotArticleVo : hotArticleVos) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }

            // 如果不在热点文章中，将当前文章加入热点文章
            if (flag) {

                // 热点文章数量大于30，替换最小的热点分数文章
                if (hotArticleVos.size() >= 30) {
                    hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore)).collect(Collectors.toList());
                    HotArticleVo lastHotArticleVo = hotArticleVos.get(hotArticleVos.size() - 1);
                    if (lastHotArticleVo.getScore() < score) {
                        hotArticleVos.remove(lastHotArticleVo);
                        HotArticleVo hot = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hot);
                        hot.setScore(score);
                        hotArticleVos.add(hot);
                    }
                }else{
                    HotArticleVo hot = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hot);
                    hot.setScore(score);
                    hotArticleVos.add(hot);
                }
            }


            // 2redis
            hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            cacheService.set(key, JSON.toJSONString(hotArticleVos));
        }
    }

    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;

        if (apArticle.getLikes() != null) {
            score += apArticle.getLikes();
        }
        if (apArticle.getCollection() != null) {
            score += apArticle.getCollection();
        }
        if (apArticle.getComment() != null) {
            score += apArticle.getComment();
        }
        if (apArticle.getViews() != null) {
            score += apArticle.getViews();
        }
        return score;
    }

    private ApArticle updateArticle(ArticleVisitStreamMess mess) {
        update(new LambdaUpdateWrapper<ApArticle>().eq(ApArticle::getId, mess.getArticleId())
                .set(mess.getLike() != null, ApArticle::getLikes, mess.getLike())
                .set(mess.getCollect() != null, ApArticle::getCollection, mess.getCollect())
                .set(mess.getComment() != null, ApArticle::getComment, mess.getComment())
                .set(mess.getView() != null, ApArticle::getViews, mess.getView())
        );
        ApArticle apArticle = getById(mess.getArticleId());
        return apArticle;
    }


    @Override
    public PageResponseResult findNewsComments(ArticleCommentDto dto) {

        // 查询分页数据，自定义实现，需要手动设置分页数和total之类的
        Integer currentPage = dto.getPage();
        dto.setPage((dto.getPage()-1)*dto.getSize());


        // 获取所有文章评论详情
        List<ArticleCommnetVo> list = apArticleMapper.findNewsComments(dto);
        // 获取当前用户的所有文章的数量
        int count = apArticleMapper.findNewsCommentsCount(dto);

        PageResponseResult responseResult = new PageResponseResult(currentPage,dto.getSize(),count);
        responseResult.setData(list);

        return responseResult;
    }


    @Override
    public PageResponseResult newPage(StatisticsDto dto) {
        //类型转换
        Date beginDate = DateUtils.stringToDate(dto.getBeginDate());
        Date endDate = DateUtils.stringToDate(dto.getEndDate());
        //检查参数
        dto.checkParam();
        //分页查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<ApArticle> lambdaQueryWrapper = Wrappers.<ApArticle>lambdaQuery()
                .eq(ApArticle::getAuthorId, dto.getWmUserId())
                .between(ApArticle::getPublishTime,beginDate, endDate)
                .select(ApArticle::getId,ApArticle::getTitle,ApArticle::getLikes,ApArticle::getCollection,ApArticle::getComment,ApArticle::getViews);

        lambdaQueryWrapper.orderByDesc(ApArticle::getPublishTime);

        page = page(page,lambdaQueryWrapper);

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }


    @Override
    public List<ApArticle> findAllArticle(Integer apUserId, Date beginDate1, Date endDate1) {
        List<ApArticle> list = list(new LambdaQueryWrapper<ApArticle>().eq(ApArticle::getAuthorId, apUserId)
                .between(ApArticle::getPublishTime,beginDate1,endDate1));

        return list;
    }
}
