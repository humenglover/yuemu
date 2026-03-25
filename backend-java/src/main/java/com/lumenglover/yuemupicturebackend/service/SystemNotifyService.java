package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;

import javax.servlet.http.HttpServletRequest;

/**
 * 系统通知服务
 */
public interface SystemNotifyService extends IService<SystemNotify> {

    /**
     * 创建系统通知
     * @param systemNotify 通知对象
     * @return 通知ID
     */
    long addSystemNotify(SystemNotify systemNotify);

    /**
     * 更新系统通知
     * @param systemNotify 通知对象
     * @return 是否成功
     */
    boolean updateSystemNotify(SystemNotify systemNotify);

    /**
     * 删除系统通知（逻辑删除）
     * @param id 通知ID
     * @return 是否成功
     */
    boolean deleteSystemNotify(Long id);

    /**
     * 分页查询所有通知
     * @param page 分页对象
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    Page<SystemNotify> listSystemNotifies(Page<SystemNotify> page, QueryWrapper<SystemNotify> queryWrapper);

    /**
     * 分页查询用户通知
     * @param page 分页对象
     * @param userId 用户ID
     * @param readStatus 阅读状态（可选）
     * @param notifyType 通知类型（可选）
     * @return 分页结果
     */
    Page<SystemNotify> listUserNotifies(Page<SystemNotify> page, String userId, Integer readStatus, String notifyType);

    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUserUnreadCount(String userId);

    /**
     * 标记通知为已读
     * @param id 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long id, String userId);

    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @return 更新数量
     */
    long markAllAsRead(String userId);

    /**
     * 分页查询全局通知
     * @param page 分页对象
     * @return 分页结果
     */
    Page<SystemNotify> listGlobalNotifies(Page<SystemNotify> page);

    /**
     * 分页查询用户全局通知（考虑用户个人阅读状态）
     * @param page 分页对象
     * @param userId 用户ID
     * @param readStatus 阅读状态（可选）
     * @return 分页结果
     */
    Page<SystemNotify> listUserGlobalNotifies(Page<SystemNotify> page, String userId, Integer readStatus);

    /**
     * 分页查询用户已读通知历史
     * @param page 分页对象
     * @param userId 用户ID
     * @param notifyType 通知类型（可选）
     * @return 分页结果
     */
    Page<SystemNotify> listUserReadNotifies(Page<SystemNotify> page, String userId, String notifyType);

    /**
     * 获取用户所有的系统通知（包括已读和未读）
     * @param userId 用户ID
     * @param current 当前页
     * @param pageSize 页面大小
     * @return 分页的系统通知列表
     */
    Page<SystemNotify> getAllNotifiesByUserId(String userId, long current, long pageSize);
}
