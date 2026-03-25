package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;

import javax.servlet.http.HttpServletRequest;

/**
 * 私聊服务
 */
public interface PrivateChatService extends IService<PrivateChat> {

    /**
     * 获取查询条件
     */
    QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser);

    /**
     * 分页获取私聊列表（HTTP请求用）
     */
    Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, HttpServletRequest request);

    /**
     * 分页获取私聊列表（WebSocket用）
     */
    Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, User loginUser);

    /**
     * 获取用户的私聊列表（分页）
     */
    Page<PrivateChat> getUserPrivateChats(long userId, long current, long size);

    /**
     * 创建或更新私聊
     */
    PrivateChat createOrUpdatePrivateChat(long userId, long targetUserId, String content);

    /**
     * 增加未读消息数
     */
    void incrementUnreadCount(long userId, long targetUserId, boolean isUser);

    /**
     * 清空未读消息数
     */
    void clearUnreadCount(long userId, long targetUserId, boolean isSender);

    /**
     * 检查是否是好友
     */
    boolean checkIsFriend(long userId, long targetUserId);

    /**
     * 更新聊天类型
     */
    void updateChatType(long userId, long targetUserId, boolean isFriend);

    /**
     * 获取私聊历史记录
     */
    Page<ChatMessage> getPrivateChatHistory(Long userId, Long targetUserId, Long page, Long size);

    /**
     * 处理私聊消息
     */
    void handlePrivateChatMessage(ChatMessage chatMessage, Long privateChatId, User sender);

    /**
     * 删除私聊
     */
    boolean deletePrivateChat(Long privateChatId, User loginUser);

    /**
     * 修改私聊名称
     */
    void updateChatName(Long privateChatId, String chatName, User loginUser);

    /**
     * 获取用户所有未读消息总数
     */
    int getTotalUnreadCount(Long userId);

    /**
     * 获取用户好友未读消息总数
     */
    int getFriendUnreadCount(Long userId);

    /**
     * 清除用户所有未读消息
     */
    void clearAllUnreadCount(Long userId);
}
