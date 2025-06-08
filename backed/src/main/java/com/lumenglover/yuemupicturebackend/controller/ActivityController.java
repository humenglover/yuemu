package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
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
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

    /**
     * 创建活动（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addActivity(@RequestBody ActivityAddRequest activityAddRequest, HttpServletRequest request) {
        if (activityAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long activityId = activityService.addActivity(activityAddRequest, loginUser);
        return ResultUtils.success(activityId);
    }

    /**
     * 分页获取活动列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Activity>> listActivityByPage(@RequestBody ActivityQueryRequest activityQueryRequest,
                                                           HttpServletRequest request) {
        if (activityQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Page<Activity> activityPage = activityService.listActivities(activityQueryRequest, loginUser);
        return ResultUtils.success(activityPage);
    }

    /**
     * 获取轮播图活动列表
     */
    @PostMapping("/list/carousel")
    public BaseResponse<Page<Activity>> listCarouselActivities(@RequestBody ActivityQueryRequest activityQueryRequest) {
        if (activityQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Activity> activityPage = activityService.listCarouselActivities(activityQueryRequest);
        return ResultUtils.success(activityPage);
    }

    /**
     * 根据 id 获取活动详情
     */
    @GetMapping("/get")
    public BaseResponse<Activity> getActivityById(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.isLogin(request);
        Activity activity = activityService.getActivityDetail(id, loginUser, request);
        return ResultUtils.success(activity);
    }

    /**
     * 审核活动（仅管理员）
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = "admin")
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
     * 删除活动（仅管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteActivity(@RequestParam Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        activityService.removeById(id);
        return ResultUtils.success(true);
    }
}
