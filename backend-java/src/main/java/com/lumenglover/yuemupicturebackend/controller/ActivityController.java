package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.ActivityService;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动接口
 */
@RestController
@RequestMapping("/activity")
@Slf4j
public class ActivityController {

    @Resource
    private ActivityService activityService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SystemNotifyService systemNotifyService;

    /**
     * 创建活动（系统管理员或有活动管理权限的用户）
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.ACTIVITY_MANAGE)
    @RateLimiter(key = "activity_add", time = 3600, count = 12, message = "活动创建过于频繁，请稍后再试")
    public BaseResponse<Long> addActivity(@RequestBody ActivityAddRequest activityAddRequest, HttpServletRequest request) {
        if (activityAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        // 检查是否为系统管理员
        boolean isSystemAdmin = userService.isAdmin(loginUser);
        Long spaceId = activityAddRequest.getSpaceId();

        // 如果没有指定空间ID，需要是系统管理员才能创建全局活动
        if (spaceId == null && !isSystemAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非系统管理员无法创建全局活动");
        }

        // 调用服务方法创建活动，服务内部会处理权限检查
        Long activityId = activityService.addActivity(activityAddRequest, loginUser);

        return ResultUtils.success(activityId);
    }

    /**
     * 系统管理员分页获取活动列表
     */
    @PostMapping("/list/page")
    @RateLimiter(key = "activity_list", time = 60, count = 30, message = "活动列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<Activity>> listActivityByPage(@RequestBody ActivityQueryRequest activityQueryRequest,
                                                           HttpServletRequest request) {
        if (activityQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);


        Page<Activity> activityPage = activityService.listActivities(activityQueryRequest, loginUser);

        // 如果不是管理员，过滤掉过期的活动
        if (loginUser == null || !"admin".equals(loginUser.getUserRole())) {
            List<Activity> filteredRecords = activityPage.getRecords().stream()
                    .filter(activity -> activity.getIsExpired() == null || activity.getIsExpired().equals(0))
                    .collect(Collectors.toList());
            activityPage.setRecords(filteredRecords);
        }

        return ResultUtils.success(activityPage);
    }

    /**
     * 根据空间ID分页获取活动列表（空间成员可以查看活动，空间管理员可以查看所有活动）
     */
    @PostMapping("/list/page/space")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.ACTIVITY_VIEW)
    @RateLimiter(key = "activity_list_space", time = 60, count = 30, message = "空间活动列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<Activity>> listActivityBySpaceId(@RequestBody ActivityQueryRequest activityQueryRequest,
                                                              HttpServletRequest request) {
        if (activityQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        Page<Activity> activityPage = activityService.listActivitiesBySpaceId(activityQueryRequest, loginUser);

        // 如果不是系统管理员，过滤掉过期的活动
        if (loginUser == null || !userService.isAdmin(loginUser)) {
            List<Activity> filteredRecords = activityPage.getRecords().stream()
                    .filter(activity -> activity.getIsExpired() == null || activity.getIsExpired().equals(0))
                    .collect(Collectors.toList());
            activityPage.setRecords(filteredRecords);
        }

        return ResultUtils.success(activityPage);
    }

    /**
     * 获取轮播图活动列表（仅公共空间活动）
     */
    @PostMapping("/list/carousel")
    @RateLimiter(key = "activity_list_carousel", time = 60, count = 60, message = "轮播活动查询过于频繁，请稍后再试")
    public BaseResponse<Page<Activity>> listCarouselActivities(@RequestBody ActivityQueryRequest activityQueryRequest) {
        if (activityQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 只查询公共空间活动（spaceId为null的活动）
        activityQueryRequest.setSpaceId(null);
        Page<Activity> activityPage = activityService.listCarouselActivities(activityQueryRequest);

        // 过滤掉过期的活动
        List<Activity> filteredRecords = activityPage.getRecords().stream()
                .filter(activity -> activity.getIsExpired() == null || activity.getIsExpired().equals(0))
                .collect(Collectors.toList());
        activityPage.setRecords(filteredRecords);

        return ResultUtils.success(activityPage);
    }

    /**
     * 根据 id 获取活动详情
     */
    @GetMapping("/get")
    @RateLimiter(key = "activity_get", time = 60, count = 50, message = "活动详情查询过于频繁，请稍后再试")
    public BaseResponse<Activity> getActivityById(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.isLogin(request);
        Activity activity = activityService.getActivityDetail(id, loginUser, request);

        // 如果活动已过期且用户不是管理员，返回错误
        if (activity.getIsExpired() != null && activity.getIsExpired().equals(1)
                && (loginUser == null || !userService.isAdmin(loginUser))) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "活动已过期");
        }

        return ResultUtils.success(activity);
    }

    /**
     * 审核活动（仅管理员）
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = "admin")
    @RateLimiter(key = "activity_review", time = 60, count = 60, message = "活动审核过于频繁，请稍后再试")
    public BaseResponse<Boolean> reviewActivity(@RequestParam Long activityId,
                                                @RequestParam Integer status,
                                                @RequestParam(required = false) String message,
                                                HttpServletRequest request) {
        if (activityId == null || activityId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        activityService.reviewActivity(activityId, status, message, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除活动（系统管理员或有活动管理权限的用户）
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.ACTIVITY_MANAGE)
    @RateLimiter(key = "activity_delete", time = 60, count = 60, message = "活动删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteActivity(@RequestParam Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        activityService.deleteActivity(id, loginUser);
        return ResultUtils.success(true);
    }
}
