package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAuditRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserJoinRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserRecommendRequest;
import com.lumenglover.yuemupicturebackend.model.entity.SpaceUser;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;
import com.lumenglover.yuemupicturebackend.service.SpaceService;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_add", time = 3600, count = 10, message = "空间成员添加过于频繁，请稍后再试")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_delete", time = 60, count = 10, message = "空间成员删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_list", time = 60, count = 30, message = "空间成员列表查询过于频繁，请稍后再试")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_edit", time = 60, count = 10, message = "空间成员编辑过于频繁，请稍后再试")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 退出空间
     */
    @PostMapping("/quit")
    @RateLimiter(key = "space_user_quit", time = 3600, count = 5, message = "退出空间操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> quitSpace(@RequestBody DeleteRequest deleteRequest,
                                           HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        long spaceId = deleteRequest.getId();
        boolean result = spaceUserService.quitSpace(spaceId, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    @RateLimiter(key = "space_user_my_list", time = 60, count = 30, message = "我的空间列表查询过于频繁，请稍后再试")
    public BaseResponse<List<SpaceVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.isLogin(request);
        if(loginUser == null){
            return ResultUtils.success(Collections.emptyList());
        }

        List<SpaceVO> spaceVOList = spaceService.listMyTeamSpaceWithActivityAndRecommendedUsers(loginUser.getId());
        return ResultUtils.success(spaceVOList);
    }

    /**
     * 审核空间成员申请
     */
    @PostMapping("/audit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_audit", time = 60, count = 20, message = "空间成员审核过于频繁，请稍后再试")
    public BaseResponse<Boolean> auditSpaceUser(@RequestBody SpaceUserAuditRequest spaceUserAuditRequest,
                                                HttpServletRequest request) {
        if (spaceUserAuditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean result = spaceUserService.auditSpaceUser(spaceUserAuditRequest, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 申请加入空间
     */
    @PostMapping("/join")
    @RateLimiter(key = "space_user_join", time = 3600, count = 5, message = "加入空间操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> joinSpace(@RequestBody SpaceUserJoinRequest spaceUserJoinRequest,
                                           HttpServletRequest request) {
        if (spaceUserJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean result = spaceUserService.joinSpace(spaceUserJoinRequest, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 设置推荐成员
     */
    @PostMapping("/setRecommended")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @RateLimiter(key = "space_user_set_recommended", time = 60, count = 10, message = "设置推荐成员过于频繁，请稍后再试")
    public BaseResponse<Boolean> setRecommendedMember(@RequestBody SpaceUserRecommendRequest recommendRequest,
                                                      HttpServletRequest request) {
        if (recommendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean result = spaceUserService.setRecommendedMember(
                recommendRequest.getSpaceId(),
                recommendRequest.getUserId(),
                recommendRequest.getIsRecommended(),
                loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取推荐成员列表
     */
    @PostMapping("/list/recommended")
    @RateLimiter(key = "space_user_list_recommended", time = 60, count = 30, message = "推荐成员查询过于频繁，请稍后再试")
    public BaseResponse<List<SpaceUserVO>> listRecommendedMembers(@RequestBody SpaceUserQueryRequest queryRequest,
                                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null || queryRequest.getSpaceId() == null, ErrorCode.PARAMS_ERROR);

        List<SpaceUserVO> spaceUserVOList = spaceUserService.getRecommendedMembersVO(queryRequest.getSpaceId());
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 检查用户是否已有未审核的申请
     */
    @PostMapping("/checkPending")
    @RateLimiter(key = "space_user_check_pending", time = 60, count = 30, message = "待处理申请查询过于频繁，请稍后再试")
    public BaseResponse<SpaceUser> checkPendingApplication(@RequestBody SpaceUserQueryRequest queryRequest,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null || queryRequest.getUserId() == null || queryRequest.getSpaceId() == null,
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.isLogin(request);
        if (loginUser == null) {
            return ResultUtils.success(null);
        }

        // 只能查询自己的申请状态
        if (!queryRequest.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查询自己的申请状态");
        }

        SpaceUser spaceUser = spaceUserService.checkPendingApplication(
                queryRequest.getUserId(), queryRequest.getSpaceId());

        return ResultUtils.success(spaceUser);
    }
}
