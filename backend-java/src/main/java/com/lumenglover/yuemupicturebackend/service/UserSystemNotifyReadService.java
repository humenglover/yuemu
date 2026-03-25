package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.UserSystemNotifyRead;

/**
 * 用户系统通知阅读状态服务
 */
public interface UserSystemNotifyReadService extends IService<UserSystemNotifyRead> {

    /**
     * 检查用户是否已读指定通知
     * @param userId 用户ID
     * @param systemNotifyId 系统通知ID
     * @return 是否已读
     */
    boolean isRead(Long userId, Long systemNotifyId);

    /**
     * 标记用户已读指定通知
     * @param userId 用户ID
     * @param systemNotifyId 系统通知ID
     * @return 是否成功
     */
    boolean markAsRead(Long userId, Long systemNotifyId);

    /**
     * 获取用户未读全局通知数量
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUserUnreadGlobalNotifyCount(Long userId);
}
