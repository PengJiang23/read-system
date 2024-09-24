package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vo.WmNewsVo;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.misc.VM;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Override
    public ResponseResult finaList(WmNewsPageReqDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 默认分页参数配置
        dto.checkParam();

        WmUser user = WmThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        IPage page = new Page(dto.getPage(), dto.getSize());

        // 多条件动态查询
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<WmNews>()
                .eq(WmNews::getUserId, user.getId())
                .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                .eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId())
                .like(StringUtils.isNoneBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword())
                .between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate())
                .orderByDesc(WmNews::getCreatedTime);

        IPage result = page(page, queryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(result.getRecords());
        return responseResult;
    }


    @Transactional
    @Override
    public ResponseResult submitOrUpdate(WmNewsDto dto) {

        // 文章和素材
        // 文章与素材一对多关系

        /**
         * 文章保存
         * 1.检查文章的封面图片，list处理为string
         * 2.检查文章封面图片种类，自动类型则设置空
         *
         * 文章是否为草稿，草稿就不需要将文章中素材和文章绑定存储（浪费资源）
         *
         * 非草稿
         * 1.获取文章中的图片，然后记录文章素材与对应的文章关系
         * 2.记录文章封面图片与文章关系
         */
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);


        // 检查封面图片
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            String imagerStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imagerStr);
        }

        // 封面图片种类，单图，多图，自动（后续自动需要手动根据文章中图内容进行匹配）
        if (dto.getType() == WemediaConstants.WM_NEWS_TYPE_AUTO) {
            wmNews.setType(null);
        }

        saveOrUpdateWmNews(wmNews);

        // 文章是否为草稿
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.SUCCESS);
        }


        // 文章图片与素材关系记录
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());

        // 文章封面图片与素材关系记录
        saveRelativeInfoForCover(dto, wmNews, materials);


        // 文章审核（文本+图片）
        // todo 这里审核是顺序执行，保存数据库+内容审核
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        // 文章审核加入延时队列中

        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Override
    public ResponseResult editNews(Integer newsId) {
        if (newsId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // todo 可以不用判断是否为当前用户，因为这里用户id是唯一的，除非分库
        WmNews wmNews = wmNewsMapper.selectById(newsId);
        return ResponseResult.okResult(wmNews);
    }

    @Override
    public ResponseResult deleteNews(Integer newsId) {
        if (newsId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文章Id不可缺少");
        }

        // todo 是否要判断是当前用户对应的news？？？？
        WmNews wmNews = wmNewsMapper.selectById(newsId);

        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }

        if (wmNews.getStatus().equals((short) 9)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文章已发布");
        }

        int i = wmNewsMapper.deleteById(wmNews);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 文章上下架 + kafka
     * 自媒体端和app端都需要上下架
     *
     * @param dto
     * @return
     */

    @Override
    public ResponseResult downOrUpNews(WmNewsDto dto) {


        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文章Id不可缺少");
        }

        WmNews wmNews = wmNewsMapper.selectById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }

        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(501, "当前文章不是发布状态，不能上下架");
        }

        // 文章下架/上架
        if (wmNews.getEnable().equals(WmNews.Enable.DOWN.getEnable())) {
            wmNews.setEnable(WmNews.Enable.UP.getEnable());
        } else if (wmNews.getEnable().equals(WmNews.Enable.UP.getEnable())) {
            wmNews.setEnable(WmNews.Enable.DOWN.getEnable());
        }

        // 自媒体端文章上下架状态更新
        wmNewsMapper.updateById(wmNews);

        // 传递给kafka消息
        if (wmNews.getArticleId() != null) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("articleId", wmNews.getArticleId());
            hashMap.put("enable", wmNews.getEnable());
            kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(hashMap));
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();

        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {

            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() < 3 && materials.size() >= 1) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
        }

        if (images != null && images.size() > 0) {
            wmNews.setImages(StringUtils.join(images, ","));
        }
        updateById(wmNews);
        if (images != null && images.size() > 0) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }


    }


    /**
     * 保存文章图片的url和文章关系
     *
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        // type是0，表示文章内容中的关系
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }


    /**
     * 判断素材库是否记录了文章中包含的图片url
     * 获取图片在素材库中的id（素材id）
     *
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {

        if (materials != null && !materials.isEmpty()) {
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(new LambdaQueryWrapper<WmMaterial>().in(WmMaterial::getUrl, materials));

            if (dbMaterials == null || dbMaterials.size() == 0) {
                throw new CustomException(AppHttpCodeEnum.PARAM_IMAGE_FORMAT_ERROR);
            }
            if (materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.PARAM_IMAGE_FORMAT_ERROR);
            }

            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            wmNewsMaterialMapper.saveRelations(idList, newsId, type);
        }


    }


    /**
     * 文章内容中的图片需要提取，数据转为json格式，提取type为image的
     *
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);

        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }

        return materials;
    }

    /**
     * 保存/修改文章
     * 如果修改文章，这里还要删除文章与素材关系，因为后面都会重新根据前端传入数据重新插入关系（防止修改文章存在图片更新或者删除）
     * todo
     *
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {


        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setSubmitedTime(new Date());

        // 默认允许上架
        wmNews.setEnable((short) 1);


        // 新增文章
        if (wmNews.getId() == null) {
            wmNews.setCreatedTime(new Date());
            save(wmNews);
        } else {
            wmNewsMaterialMapper.delete(new LambdaQueryWrapper<WmNewsMaterial>()
                    .eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }


    }

// ----------------admin端的文章人工审核接口--------------------

    @Override
    public ResponseResult finaNewsList(NewsAuthDto dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());
        IPage result = page(page, new LambdaQueryWrapper<WmNews>().eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                .like(StringUtils.isNoneBlank(dto.getTitle()), WmNews::getTitle, dto.getTitle())
                .orderByDesc(WmNews::getCreatedTime));

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(result.getRecords());

        return responseResult;
    }


    @Autowired
    private WmUserMapper wmUserMapper;

    @Override
    public ResponseResult findWmNewsVo(Integer id) {

        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }


        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.查询用户信息
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());

        //4.封装vo返回
        WmNewsVo vo = new WmNewsVo();
        //属性拷贝
        BeanUtils.copyProperties(wmNews,vo);
        if(wmUser != null){
            vo.setAuthorName(wmUser.getName());
        }

        ResponseResult responseResult = new ResponseResult().ok(vo);

        return responseResult;
    }

    /**
     * 文章审核，修改状态
     * @param status 2  审核失败  4 审核成功
     * @param dto
     * todo 人工审核这里有问题 2024/9/13
     * @return
     */
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDto dto) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章信息
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.修改文章的状态
        wmNews.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }else{
            wmNews.setReason("人工审核通过");
        }
        updateById(wmNews);

        //审核成功，则需要创建app端文章数据，并修改自媒体文章
        if(status.equals(WemediaConstants.WM_NEWS_AUTH_PASS)){
            //创建app端文章数据
            ResponseResult responseResult = wmNewsAutoScanService.saveAppArticle(wmNews);
            if(responseResult.getCode().equals(200)){
                wmNews.setArticleId((Long) responseResult.getData());
                wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
                updateById(wmNews);
            }
        }

        //4.返回
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

}
