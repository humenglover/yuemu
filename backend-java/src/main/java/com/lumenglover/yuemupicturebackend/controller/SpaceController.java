package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.auth.SpaceUserAuthManager;
import com.lumenglover.yuemupicturebackend.model.dto.space.*;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceLevelEnum;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;
import com.lumenglover.yuemupicturebackend.service.SpaceService;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 鹿梦
 */
@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;



    @Resource
    private SpaceUserService spaceUserService;

    @PostMapping("/add")
    @RateLimiter(key = "space_add", time = 3600, count = 5, message = "空间创建过于频繁，请稍后再试")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    @PostMapping("/delete")
    @RateLimiter(key = "space_delete", time = 60, count = 10, message = "空间删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        // 判断是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或者管理员可删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 批量删除空间
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchSpace(@RequestBody List<DeleteRequest> deleteRequestList) {
        if (CollectionUtils.isEmpty(deleteRequestList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取要删除的空间ID列表
        List<Long> ids = deleteRequestList.stream()
                .map(DeleteRequest::getId)
                .collect(Collectors.toList());

        // 批量删除MySQL数据
        boolean result = spaceService.removeByIds(ids);

        return ResultUtils.success(result);
    }

    /**
     * 更新空间（仅管理员可用）
     *
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "space_update", time = 60, count = 10, message = "空间更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                             HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 更新MySQL
        boolean result = spaceService.updateById(space);

        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    @RateLimiter(key = "space_get_vo", time = 60, count = 50, message = "空间详情查询过于频繁，请稍后再试")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    @RateLimiter(key = "space_list_vo", time = 60, count = 30, message = "空间列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));

        // 获取封装类（包含活动和推荐用户信息）
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPageWithActivityAndRecommendedUsers(spacePage, request);

        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 编辑空间（给用户使用）
     */
    @PostMapping("/edit")
    @RateLimiter(key = "space_edit", time = 60, count = 10, message = "空间编辑过于频繁，请稍后再试")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新MySQL
        boolean result = spaceService.updateById(space);

        return ResultUtils.success(result);
    }

    /**
     * 获取空间级别列表，便于前端展示
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    /**
     * 设置空间推荐状态
     *
     * @param spaceId 空间ID
     * @param recommendStatus 推荐状态：0-取消推荐 1-推荐
     * @param request
     * @return
     */
    @PostMapping("/recommend")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "space_recommend", time = 60, count = 10, message = "空间推荐设置过于频繁，请稍后再试")
    public BaseResponse<Boolean> setRecommendStatus(@RequestParam Long spaceId, @RequestParam Integer recommendStatus, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(spaceId == null || recommendStatus == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        boolean result = spaceService.setRecommendStatus(spaceId, recommendStatus, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * 获取推荐空间列表
     *
     * @param request
     * @return
     */
    @GetMapping("/recommended")
    @RateLimiter(key = "space_list_recommended", time = 60, count = 30, message = "推荐空间列表查询过于频繁，请稍后再试")
    public BaseResponse<List<SpaceVO>> listRecommendedSpaces(HttpServletRequest request) {
        List<SpaceVO> spaceVOList = spaceService.listRecommendedSpaces();
        return ResultUtils.success(spaceVOList);
    }
}
