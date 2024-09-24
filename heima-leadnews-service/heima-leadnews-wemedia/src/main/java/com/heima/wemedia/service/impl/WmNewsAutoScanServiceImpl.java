package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScanV1;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rx.exceptions.Exceptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScanV1 greenImageScanV1;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;
    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    @Autowired
    private Tess4jClient tess4jClient;



    @Override
    @Async
    public void autoScanWmNews(Integer id) {
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl-当前审核的文章不存在");
        }

        // 检查文章是否处于待审核状态
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {

            // 文章中分别提取文字和图片
            Map<String, Object> textAndImages = handleTextAndImage(wmNews);

            // todo 构建特殊的敏感词数据库
            boolean isSensitive = handleSensitiveScan(textAndImages.get("content").toString(), wmNews);
            if (!isSensitive) return;

            //审核文本内容
            boolean isTextScan = handleTextScan(textAndImages.get("content").toString(), wmNews);
            if (!isTextScan) return;


            //审核图片
            boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) return;


            // 审核通过，将文章消息同步到app端
//            wmNews.setPublishTime(new Date());
            ResponseResult responseResult = saveAppArticle(wmNews);
            if (!responseResult.getCode().equals(200)) {
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败");
            }

            //回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews, (short) 9, "审核成功");


        }

    }

    /**
     * 本地敏感词库查询匹配
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        boolean flag = true;
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(new LambdaQueryWrapper<WmSensitive>().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        SensitiveWordUtil.initMap(sensitiveList);

        Map<String, Integer> matchWords = SensitiveWordUtil.matchWords(content);

        if (matchWords.size() > 0) {
            updateWmNews(wmNews, (short) 2, "文章存在与本地构建敏感词相似词汇");
            flag = false;
        }

        return flag;
    }

    public ResponseResult saveAppArticle(WmNews wmNews) {

        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, dto);
        //文章的布局
        dto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        //作者
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }

        //设置文章id
        if (wmNews.getArticleId() != null) {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = articleClient.saveArticle(dto);
        return responseResult;

    }


    /**
     * 图片审核
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {

        boolean flag = true;
        if (images == null || images.size() == 0) {
            return flag;
        }

        //图片去重，文章图片可能有封面存在重复
        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();


        //审核图片，todo 这里一张一张图片审核，不是全部图片一块审核
        try {
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                // 图片ocr获取文字，文字审查
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                BufferedImage imageBuffer = ImageIO.read(byteArrayInputStream);
                String result = tess4jClient.doOCR(imageBuffer);
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if(!isSensitive){
                    return false;
                }

                Map map = greenImageScanV1.imageScan(bytes);
                if (map != null) {

                    //审核失败
                    if (!map.get("label").equals("nonLabel")) {
                        flag = false;
                        updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
                    }

                    //不确定信息  需要人工审核
//                    if (map.get("label").equals("review")) {
//                        flag = false;
//                        updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
//                    }
                }
            }

        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;

    }

    /**
     * 文本内容审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;

        if ((wmNews.getTitle() + "-" + content).length() == 0) {
            return flag;
        }

        try {
            Map map = greenTextScan.greeTextScan(content);
            if (map != null) {

                if (map.get("labels") == null) {
                    flag = true;
                    return flag;
                }

                if (map.get("labels").equals("contraband")) {
                    flag = false;
                    log.error("违规文本");
                    updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
                }

                //不确定信息  需要人工审核
                if (map.get("suggestion").equals("review")) {
                    flag = false;
                    updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
                }
            }

        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }

    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }


    /**
     * 提取文章中的图片和文本
     * 文本只存在content和title
     * 图片存在content和封面
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImage(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();

        ArrayList<String> images = new ArrayList<>();


        // 文本内容解析
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);

            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                }

                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }

        // 获取封面图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        // 提取标题
        if (StringUtils.isNotBlank(wmNews.getTitle())) {
            stringBuilder.append(wmNews.getTitle());
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("content", stringBuilder);
        hashMap.put("images", images);
        return hashMap;
    }
}
