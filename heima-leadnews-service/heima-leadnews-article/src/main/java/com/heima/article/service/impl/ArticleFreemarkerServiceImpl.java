package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rx.exceptions.Exceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ApArticleService apArticleService;

    @Override
    @Async
    public void buildArticleToMinIO(ApArticle apArticle, String content) {

        ApArticleContent articleContent = apArticleContentMapper.selectOne(new LambdaQueryWrapper<ApArticleContent>()
                .eq(ApArticleContent::getArticleId, apArticle.getId()));

        if(articleContent != null && StringUtils.isNotBlank(articleContent.getContent())){

            Template template = null;
            try {
                template = configuration.getTemplate("article.ftl");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            HashMap<String, Object> params = new HashMap<>();
            params.put("content", JSONArray.parseArray(articleContent.getContent(), Map.class));
            params.put("article",apArticle);
            params.put("authorApUserId",apArticle.getAuthorId());
            StringWriter out = new StringWriter();

            try {

                template.process(params,out);

                ByteArrayInputStream is = new ByteArrayInputStream(out.toString().getBytes());
                String path = fileStorageService.uploadHtmlFile("", articleContent.getArticleId() + ".html", is);
                apArticleService.update(new LambdaUpdateWrapper<ApArticle>().eq(ApArticle::getId,apArticle.getId())
                        .set(ApArticle::getStaticUrl,path));

                //创建静态文件同时发送kafka消息，创建索引
                createArticleESIndex(apArticle,content,path);

            } catch (Exception e) {
                throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
            }


        }


    }


    @Autowired
    private KafkaTemplate kafkaTemplate;
    /**
     * 发送文章内容到mq中
     * @param apArticle
     * @param content
     * @param path
     */
    private void createArticleESIndex(ApArticle apArticle, String content, String path) {
        SearchArticleVo searchArticleVo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle,searchArticleVo);
        searchArticleVo.setContent(content);
        searchArticleVo.setStaticUrl(path);

        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(searchArticleVo));
    }
}
