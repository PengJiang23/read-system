package com.heima.search.service.impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;


@Service
@Transactional
@Log4j2
public class ApUserSearchServiceImpl implements ApUserSearchService {


    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 将搜索记录存储mongodb
     *
     * @param keyword
     * @param userId
     */
    @Async
    public void insert(String keyword, Integer userId) {

        // 查询当前用户的关键词
        Query query = Query.query(Criteria.where("userId").is(userId).and("keyword").is(keyword));
        ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);

        // 更新创建时间
        if (apUserSearch != null) {
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);
            return;
        }


        // 用户查询历史不存在，创建用户搜索历史+判断用户搜索历史是否超过10条
        apUserSearch = new ApUserSearch();
        apUserSearch.setUserId(userId);
        apUserSearch.setKeyword(keyword);
        apUserSearch.setCreatedTime(new Date());

        Query query1 = Query.query(Criteria.where("userId").is(userId));
        query1.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<ApUserSearch> apUserSearches = mongoTemplate.find(query1, ApUserSearch.class);

        if (apUserSearches == null || apUserSearches.size() < 10) {
            mongoTemplate.save(apUserSearch);
        } else {
            ApUserSearch lastUserSearch = apUserSearches.get(apUserSearches.size() - 1);
            mongoTemplate.findAndReplace(Query.query(Criteria.where("id").is(lastUserSearch.getId())), apUserSearch);
        }

        log.info("保存用户搜索历史完成");
    }


    /**
     * 获取用户所有搜索记录
     *
     * @return
     */
    public ResponseResult findUserSearch() {

        ApUser user = AppThreadLocalUtil.getUser();

        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        List<ApUserSearch> apUserSearches = mongoTemplate.find(Query.query(Criteria
                                .where("userId").is(user.getId()))
                        .with(Sort.by(Sort.Direction.DESC, "createdTime")),
                ApUserSearch.class);

        return ResponseResult.okResult(apUserSearches);
    }

    /**
     * 删除用户搜索记录
     */
    @Override
    public ResponseResult delUserSearch(HistorySearchDto dto) {

        if(dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.判断是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        mongoTemplate.remove(Query.query(Criteria
                .where("userId").is(user.getId())
                .and("id").is(dto.getId())),ApUserSearch.class);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}