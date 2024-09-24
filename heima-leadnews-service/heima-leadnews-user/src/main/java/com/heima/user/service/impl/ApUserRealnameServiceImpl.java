package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.common.constants.UserConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
@Slf4j
@Transactional
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {

    @Autowired
    private ApUserRealnameMapper apUserRealnameMapper;

    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());
        IPage result = page(page, new LambdaQueryWrapper<ApUserRealname>().eq(dto.getStatus() != null, ApUserRealname::getStatus, dto.getStatus()));

        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) result.getTotal());
        responseResult.setData(result.getRecords());

        return responseResult;

    }

    @Override
    public ResponseResult updateStatus(AuthDto dto, Short status) {

        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 更新实名表状态信息
        ApUserRealname apUserRealname = apUserRealnameMapper.selectById(dto.getId());
        if (apUserRealname == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }


        // 更新状态/审核理由
        if (status.equals(UserConstants.FAIL_AUTH)) {
            apUserRealname.setReason(dto.getMsg());
        }
        apUserRealname.setStatus(status);
        apUserRealname.setUpdatedTime(new Date());

        apUserRealnameMapper.updateById(apUserRealname);

        // 如果审核通过，创建wemedia-user账号
        if (status.equals(UserConstants.PASS_AUTH)) {
            ResponseResult wmUserAndAuthor = createWmUserAndAuthor(dto);
            if (wmUserAndAuthor != null) {
                return wmUserAndAuthor;
            }
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private ApUserMapper apUserMapper;

    @Autowired
    private IWemediaClient wemediaClient;

    /**
     * 创建自媒体账户
     * todo 用户实名是在app端，用户已经拥有了app端的账号所以只需迁移到wemedia端即可
     *
     * @param dto
     * @return
     */
    private ResponseResult createWmUserAndAuthor(AuthDto dto) {
        Integer userRealnameId = dto.getId();

        // 重新从db获取用户实名信息
        ApUserRealname apUserRealname = getById(userRealnameId);
        if (apUserRealname == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        Integer userId = apUserRealname.getUserId();
        ApUser apUser = apUserMapper.selectById(userId);
        if (apUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }


        // 查看wemedia是否已经拥有账号
        WmUser wmUser = wemediaClient.findWmUserByName(apUser.getName());
        if (wmUser == null) {
            wmUser = new WmUser();
            wmUser.setApUserId(apUser.getId());
            wmUser.setCreatedTime(new Date());
            wmUser.setName(apUser.getName());
            wmUser.setPassword(apUser.getPassword());
            wmUser.setSalt(apUser.getSalt());
            wmUser.setPhone(apUser.getPhone());
            wmUser.setStatus(9);
            wemediaClient.saveWmUser(wmUser);
        }

        apUser.setFlag((short) 9);

        apUserMapper.updateById(apUser);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
