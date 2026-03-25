package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.systemnotify.SystemNotifyAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.systemnotify.SystemNotifyQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.systemnotify.SystemNotifyUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 系统通知接口
 */
@RestController
@RequestMapping("/notifies")
@Slf4j
public class SystemNotifyController {

    @Resource
    private SystemNotifyService systemNotifyService;

    @Resource
    private UserService userService;

    // ==================== 通知管理接口（管理员/系统内部调用） ====================

    /**
     * 创建通知（管理员推送/系统触发）
     */
    @PostMapping
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSystemNotify(@RequestBody SystemNotifyAddRequest systemNotifyAddRequest) {
        if (systemNotifyAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemNotify systemNotify = new SystemNotify();
        BeanUtils.copyProperties(systemNotifyAddRequest, systemNotify);
        // 设置操作人信息
        systemNotify.setOperatorId("admin"); // 实际应用中应该从登录信息获取
        systemNotify.setOperatorType("ADMIN");
        // 设置默认值
        if (systemNotify.getIsGlobal() == null) {
            systemNotify.setIsGlobal(0);
        }
        if (systemNotify.getIsEnabled() == null) {
            systemNotify.setIsEnabled(1);
        }
        long id = systemNotifyService.addSystemNotify(systemNotify);
        return ResultUtils.success(id);
    }

    /**
     * 更新通知（仅未发送/特定状态）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SystemNotify> updateSystemNotify(@PathVariable Long id, @RequestBody SystemNotifyUpdateRequest systemNotifyUpdateRequest) {
        if (id == null || id <= 0 || systemNotifyUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemNotify systemNotify = new SystemNotify();
        BeanUtils.copyProperties(systemNotifyUpdateRequest, systemNotify);
        systemNotify.setId(id);
        boolean result = systemNotifyService.updateSystemNotify(systemNotify);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(systemNotify);
    }

    /**
     * 逻辑删除通知
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSystemNotify(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = systemNotifyService.deleteSystemNotify(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询所有通知
     */
    @GetMapping
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SystemNotify>> listSystemNotifies(SystemNotifyQueryRequest systemNotifyQueryRequest) {
        if (systemNotifyQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemNotify systemNotify = new SystemNotify();
        BeanUtils.copyProperties(systemNotifyQueryRequest, systemNotify);
        Page<SystemNotify> page = new Page<>(systemNotifyQueryRequest.getCurrent(), systemNotifyQueryRequest.getPageSize());
        Page<SystemNotify> result = systemNotifyService.listSystemNotifies(page, getQueryWrapper(systemNotify));
        return ResultUtils.success(result);
    }



    // ==================== 用户通知接口（普通用户调用） ====================

    /**
     * 分页查询个人通知（包含用户个人通知和未读的全局通知）
     */
    @GetMapping("/user/notifies")
    @RateLimiter(key = "system_notify_list_user", time = 60, count = 30, message = "用户通知查询过于频繁，请稍后再试")
    public BaseResponse<Page<SystemNotify>> listUserNotifies(SystemNotifyQueryRequest systemNotifyQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<SystemNotify> page = new Page<>(systemNotifyQueryRequest.getCurrent(), systemNotifyQueryRequest.getPageSize());
        Page<SystemNotify> result = systemNotifyService.listUserNotifies(page, String.valueOf(loginUser.getId()),
                systemNotifyQueryRequest.getReadStatus(), systemNotifyQueryRequest.getNotifyType());
        return ResultUtils.success(result);
    }

    /**
     * 分页查询用户已读通知历史
     */
    @GetMapping("/user/notifies/read-history")
    @RateLimiter(key = "system_notify_read_history", time = 60, count = 30, message = "已读通知查询过于频繁，请稍后再试")
    public BaseResponse<Page<SystemNotify>> listUserReadNotifies(SystemNotifyQueryRequest systemNotifyQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<SystemNotify> page = new Page<>(systemNotifyQueryRequest.getCurrent(), systemNotifyQueryRequest.getPageSize());
        Page<SystemNotify> result = systemNotifyService.listUserReadNotifies(page, String.valueOf(loginUser.getId()),
                systemNotifyQueryRequest.getNotifyType());
        return ResultUtils.success(result);
    }

    /**
     * 获取未读通知数量（包括个人通知和全局通知）
     */
    @GetMapping("/user/notifies/unread-count")
    @RateLimiter(key = "system_notify_unread_count", time = 60, count = 50, message = "未读通知查询过于频繁，请稍后再试")
    public BaseResponse<Long> getUserUnreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long count = systemNotifyService.getUserUnreadCount(String.valueOf(loginUser.getId()));
        return ResultUtils.success(count);
    }

    /**
     * 查看通知详情（自动标已读）
     */
    @GetMapping("/user/notifies/{id}")
    @RateLimiter(key = "system_notify_get_detail", time = 60, count = 30, message = "通知详情查询过于频繁，请稍后再试")
    public BaseResponse<SystemNotify> getUserNotifyById(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取通知详情
        SystemNotify systemNotify = systemNotifyService.getById(id);
        if (systemNotify == null || systemNotify.getIsDelete() == 1 || systemNotify.getIsEnabled() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查是否有权限查看该通知
        boolean isUserNotify = "SPECIFIC_USER".equals(systemNotify.getReceiverType()) &&
                String.valueOf(loginUser.getId()).equals(systemNotify.getReceiverId());
        boolean isGlobalNotify = systemNotify.getIsGlobal() == 1;
        if (!isUserNotify && !isGlobalNotify) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 自动标记为已读
        if (systemNotify.getReadStatus() == 0) {
            systemNotifyService.markAsRead(id, String.valueOf(loginUser.getId()));
        }
        return ResultUtils.success(systemNotify);
    }

    /**
     * 手动标记单个通知为已读
     */
    @PutMapping("/user/notifies/{id}/read")
    @RateLimiter(key = "system_notify_mark_read", time = 60, count = 30, message = "标记已读操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = systemNotifyService.markAsRead(id, String.valueOf(loginUser.getId()));
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口（仅管理员调用） ====================

    /**
     * 查询通知详情
     */
    @GetMapping("/{id}")
    public BaseResponse<SystemNotify> getSystemNotifyById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemNotify systemNotify = systemNotifyService.getById(id);
        if (systemNotify == null || systemNotify.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(systemNotify);
    }


    /**
     * 构建查询条件
     */
    private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SystemNotify> getQueryWrapper(SystemNotify systemNotify) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SystemNotify> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (systemNotify == null) {
            return queryWrapper;
        }
        // 只对非空字段进行查询
        if (systemNotify.getId() != null) {
            queryWrapper.eq("id", systemNotify.getId());
        }
        if (systemNotify.getNotifyType() != null && !systemNotify.getNotifyType().isEmpty()) {
            queryWrapper.eq("notifyType", systemNotify.getNotifyType());
        }
        if (systemNotify.getReceiverType() != null && !systemNotify.getReceiverType().isEmpty()) {
            queryWrapper.eq("receiverType", systemNotify.getReceiverType());
        }
        if (systemNotify.getIsEnabled() != null) {
            queryWrapper.eq("isEnabled", systemNotify.getIsEnabled());
        }
        if (systemNotify.getReadStatus() != null) {
            queryWrapper.eq("readStatus", systemNotify.getReadStatus());
        }
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }
}
