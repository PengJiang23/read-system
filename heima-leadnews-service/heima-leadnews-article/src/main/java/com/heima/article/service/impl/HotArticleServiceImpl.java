package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.ArticleScoreConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class HotArticleServiceImpl implements HotArticleService {


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Override
    public void computeHotArticle() {

        log.info("获取文章");
        // 查询获取前5天所有文章，计算文章每篇文章分值
        Date dateParam = DateTime.now().minusDays(200).toDate();
        List<ApArticle> articleList = apArticleMapper.selectArticle(dateParam);
        log.info("分数计算");
        List<HotArticleVo> hotArticleVoList = computeArticleScore(articleList);
        log.info("存入缓存");
        // 为每个频道，获取score前30的文章存入redis
        article2Cache(hotArticleVoList);
    }


    @Autowired
    private IWemediaClient wemediaClient;

    @Autowired
    private CacheService cacheService;

    /**
     * 每个频道前30的文章，缓存到redis
     * 首页是全部文章的前30
     *
     * @param hotArticleVoList
     */
    private void article2Cache(List<HotArticleVo> hotArticleVoList) {
        ResponseResult responseChannel = wemediaClient.getChannels();

        if (responseChannel.getCode().equals(200)) {
            String jsonString = JSON.toJSONString(responseChannel.getData());
            List<WmChannel> wmChannelList = JSON.parseArray(jsonString, WmChannel.class);


            if (wmChannelList != null && wmChannelList.size() > 0) {

                for (WmChannel wmChannel : wmChannelList) {
                    List<HotArticleVo> hotArticleVos = hotArticleVoList.stream()
                            .filter(x -> x.getChannelId().equals(wmChannel.getId()))
                            .collect(Collectors.toList());
                    // 每个频道缓存
                    sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }

        // 首页缓存
        sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
    }

    private void sortAndCache(List<HotArticleVo> hotArticleVoList, String key) {
        List<HotArticleVo> hotArticleSort = hotArticleVoList.stream()
                .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                .collect(Collectors.toList());

        if (hotArticleSort.size() > 30) {
            hotArticleSort = hotArticleVoList.subList(0, 30);
        }

        cacheService.set(key, JSON.toJSONString(hotArticleSort));

    }


    /**
     * 计算每一篇文章score
     * todo 当文章数量很多：手动实现属性复制，使用parallelStream()，根据推荐的文章数量获取有限数量的原始文章
     *
     * @param articleList
     * @return
     */
    private List<HotArticleVo> computeArticleScore(List<ApArticle> articleList) {
        // 遍历，获取其中的参数不为空，并且score累加

        List<HotArticleVo> hotArticleVoList = articleList.stream()
                .map(article -> {
                    int likes = article.getLikes() != null ? article.getLikes() : 0;
                    int collection = article.getCollection() != null ? article.getCollection() : 0;
                    int comment = article.getComment() != null ? article.getComment() : 0;
                    int views = article.getViews() != null ? article.getViews() : 0;
                    int score = likes * ArticleScoreConstants.LIKE_WEIGHT
                            + collection * ArticleScoreConstants.COLLECTION_WEIGHT
                            + comment * ArticleScoreConstants.COMMENT_WEIGHT
                            + views * ArticleScoreConstants.VIEW_WEIGHT;
                    HotArticleVo hotArticleVo = new HotArticleVo();
                    BeanUtils.copyProperties(article, hotArticleVo);
                    hotArticleVo.setScore(score);
                    return hotArticleVo;
                })
                .collect(Collectors.toList());

        return hotArticleVoList;
    }


}
