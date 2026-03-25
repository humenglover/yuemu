package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.manager.websocket.MessageWebSocketHandler;
import com.lumenglover.yuemupicturebackend.mapper.SystemNotifyMapper;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import com.lumenglover.yuemupicturebackend.service.UserSystemNotifyReadService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统通知服务实现类
 */
@Service
public class SystemNotifyServiceImpl extends ServiceImpl<SystemNotifyMapper, SystemNotify>
        implements SystemNotifyService {

    @Resource
    private UserSystemNotifyReadService userSystemNotifyReadService;

    @Resource
    @Lazy
    private MessageWebSocketHandler messageWebSocketHandler;

    @Override
    public long addSystemNotify(SystemNotify systemNotify) {
        this.save(systemNotify);

        // 通过WebSocket推送消息给相关用户
        sendSystemNotifyWebSocketNotification(systemNotify);

        return systemNotify.getId();
    }

    /**
     * 通过WebSocket推送消息给相关用户
     */
    private void sendSystemNotifyWebSocketNotification(SystemNotify systemNotify) {
        try {
            // 如果是全局通知，推送给所有在线用户
            if (systemNotify.getIsGlobal() == 1) {
                // 使用Java 8兼容的方式创建Map
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("type", "system_notify");
                responseData.put("data", systemNotify);
                messageWebSocketHandler.sendMessageToAllUsers(responseData);
            }
            // 如果是指定用户的通知，推送给该用户
            else if ("SPECIFIC_USER".equals(systemNotify.getReceiverType()) && systemNotify.getReceiverId() != null) {
                messageWebSocketHandler.sendUnreadCountToUser(systemNotify.getReceiverId());
            }
        } catch (Exception e) {
            // 记录错误日志，但不影响主流程
        }
    }

    @Override
    public boolean updateSystemNotify(SystemNotify systemNotify) {
        return this.updateById(systemNotify);
    }

    @Override
    public boolean deleteSystemNotify(Long id) {
        return this.removeById(id);
    }

    @Override
    public Page<SystemNotify> listSystemNotifies(Page<SystemNotify> page, QueryWrapper<SystemNotify> queryWrapper) {
        return this.page(page, queryWrapper);
    }

    @Override
    public Page<SystemNotify> listUserNotifies(Page<SystemNotify> page, String userId, Integer readStatus, String notifyType) {
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        // 查询指定用户的通知或全局通知
        queryWrapper.and(wrapper -> wrapper.eq("receiverType", "SPECIFIC_USER").eq("receiverId", userId)
                .or().eq("isGlobal", 1));
        // 未删除且有效的通知
        queryWrapper.eq("isDelete", 0).eq("isEnabled", 1);
        // 可选的通知类型筛选
        if (notifyType != null && !notifyType.isEmpty()) {
            queryWrapper.eq("notifyType", notifyType);
        }
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("createTime");

        Page<SystemNotify> resultPage = this.page(page, queryWrapper);

        // 处理用户的阅读状态（特别是全局通知）
        List<SystemNotify> records = resultPage.getRecords();
        List<SystemNotify> processedRecords = records.stream().map(notify -> {
            // 对于用户特定通知，直接使用数据库中的readStatus
            if ("SPECIFIC_USER".equals(notify.getReceiverType()) && userId.equals(notify.getReceiverId())) {
                return notify;
            }
            // 对于全局通知，检查用户个人阅读状态
            else if (notify.getIsGlobal() == 1) {
                boolean isRead = userSystemNotifyReadService.isRead(Long.valueOf(userId), notify.getId());
                // 创建一个新的副本以避免修改原始对象
                SystemNotify processedNotify = new SystemNotify();
                try {
                    // 复制所有属性
                    org.springframework.beans.BeanUtils.copyProperties(notify, processedNotify);
                } catch (Exception e) {
                    // 如果复制失败，手动设置关键属性
                    processedNotify.setId(notify.getId());
                    processedNotify.setCreateTime(notify.getCreateTime());
                    processedNotify.setUpdateTime(notify.getUpdateTime());
                    processedNotify.setOperatorId(notify.getOperatorId());
                    processedNotify.setOperatorType(notify.getOperatorType());
                    processedNotify.setNotifyType(notify.getNotifyType());
                    processedNotify.setSenderType(notify.getSenderType());
                    processedNotify.setSenderId(notify.getSenderId());
                    processedNotify.setReceiverType(notify.getReceiverType());
                    processedNotify.setReceiverId(notify.getReceiverId());
                    processedNotify.setTitle(notify.getTitle());
                    processedNotify.setContent(notify.getContent());
                    processedNotify.setNotifyIcon(notify.getNotifyIcon());
                    processedNotify.setRelatedBizType(notify.getRelatedBizType());
                    processedNotify.setRelatedBizId(notify.getRelatedBizId());
                    processedNotify.setExpireTime(notify.getExpireTime());
                    processedNotify.setIsGlobal(notify.getIsGlobal());
                    processedNotify.setIsEnabled(notify.getIsEnabled());
                    processedNotify.setIsDelete(notify.getIsDelete());
                }

                if (isRead) {
                    // 如果用户已读，更新通知的阅读状态
                    processedNotify.setReadStatus(1);
                } else {
                    // 如果用户未读，确保readStatus为0
                    processedNotify.setReadStatus(0);
                }
                return processedNotify;
            }
            return notify;
        }).collect(Collectors.toList());

        // 如果指定了readStatus过滤条件，则进行过滤
        if (readStatus != null) {
            processedRecords = processedRecords.stream()
                    .filter(notify -> notify.getReadStatus() == readStatus)
                    .collect(Collectors.toList());
        }

        resultPage.setRecords(processedRecords);
        return resultPage;
    }

    @Override
    public long getUserUnreadCount(String userId) {
        // 查询指定用户的通知
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiverType", "SPECIFIC_USER").eq("receiverId", userId);
        // 未读且未删除且有效的通知
        queryWrapper.eq("readStatus", 0).eq("isDelete", 0).eq("isEnabled", 1);

        // 查询用户特定通知的未读数量
        long userNotifyCount = this.count(queryWrapper);

        // 查询全局通知的未读数量（需要考虑用户独立状态）
        QueryWrapper<SystemNotify> globalQueryWrapper = new QueryWrapper<>();
        globalQueryWrapper.eq("isGlobal", 1);
        // 未删除且有效的通知
        globalQueryWrapper.eq("isDelete", 0).eq("isEnabled", 1);

        // 获取所有全局通知
        List<SystemNotify> globalNotifies = this.list(globalQueryWrapper);

        // 过滤出用户未读的全局通知
        long globalUnreadCount = globalNotifies.stream()
                .filter(notify -> !userSystemNotifyReadService.isRead(Long.valueOf(userId), notify.getId()))
                .count();

        return userNotifyCount + globalUnreadCount;
    }

    @Override
    public boolean markAsRead(Long id, String userId) {
        SystemNotify systemNotify = this.getById(id);
        // 检查通知是否存在且属于该用户（或为全局通知）
        if (systemNotify == null || systemNotify.getIsDelete() == 1 || systemNotify.getIsEnabled() == 0) {
            return false;
        }

        boolean isUserNotify = "SPECIFIC_USER".equals(systemNotify.getReceiverType()) &&
                userId.equals(systemNotify.getReceiverId());
        boolean isGlobalNotify = systemNotify.getIsGlobal() == 1;

        if (!isUserNotify && !isGlobalNotify) {
            return false;
        }

        // 对于全局通知，使用用户阅读状态表记录
        if (isGlobalNotify) {
            return userSystemNotifyReadService.markAsRead(Long.valueOf(userId), id);
        }

        // 对于用户特定通知，直接更新
        systemNotify.setReadStatus(1);
        systemNotify.setReadTime(new Date());
        return this.updateById(systemNotify);
    }

    @Override
    public long markAllAsRead(String userId) {
        // 查询指定用户未读的通知
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiverType", "SPECIFIC_USER").eq("receiverId", userId);
        // 未读且未删除且有效的通知
        queryWrapper.eq("readStatus", 0).eq("isDelete", 0).eq("isEnabled", 1);

        // 获取要更新的用户特定通知数量
        long userNotifyCount = this.count(queryWrapper);

        // 批量更新用户特定通知为已读状态
        if (userNotifyCount > 0) {
            SystemNotify updateEntity = new SystemNotify();
            updateEntity.setReadStatus(1);
            updateEntity.setReadTime(new Date());
            this.update(updateEntity, queryWrapper);
        }

        // 处理全局通知 - 为用户创建已读记录
        QueryWrapper<SystemNotify> globalQueryWrapper = new QueryWrapper<>();
        globalQueryWrapper.eq("isGlobal", 1);
        // 未删除且有效的通知
        globalQueryWrapper.eq("isDelete", 0).eq("isEnabled", 1);

        // 获取所有全局通知
        List<SystemNotify> globalNotifies = this.list(globalQueryWrapper);

        // 为每个全局通知创建用户的已读记录
        long globalReadCount = 0;
        for (SystemNotify notify : globalNotifies) {
            if (userSystemNotifyReadService.markAsRead(Long.valueOf(userId), notify.getId())) {
                globalReadCount++;
            }
        }

        return userNotifyCount + globalReadCount;
    }

    @Override
    public Page<SystemNotify> listGlobalNotifies(Page<SystemNotify> page) {
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        // 查询全局通知
        queryWrapper.eq("isGlobal", 1);
        // 未删除且有效的通知
        queryWrapper.eq("isDelete", 0).eq("isEnabled", 1);
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("createTime");
        return this.page(page, queryWrapper);
    }

    @Override
    public Page<SystemNotify> listUserReadNotifies(Page<SystemNotify> page, String userId, String notifyType) {
        // 创建查询条件
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();

        // 查询指定用户的通知或全局通知
        queryWrapper.and(wrapper -> wrapper.eq("receiverType", "SPECIFIC_USER").eq("receiverId", userId)
                .or().eq("isGlobal", 1));

        // 未删除且有效的通知
        queryWrapper.eq("isDelete", 0).eq("isEnabled", 1);

        // 可选的通知类型筛选
        if (notifyType != null && !notifyType.isEmpty()) {
            queryWrapper.eq("notifyType", notifyType);
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("createTime");

        Page<SystemNotify> resultPage = this.page(page, queryWrapper);

        // 处理用户的阅读状态（特别是全局通知）
        List<SystemNotify> records = resultPage.getRecords();
        List<SystemNotify> processedRecords = records.stream().map(notify -> {
            // 对于用户特定通知，直接使用数据库中的readStatus
            if ("SPECIFIC_USER".equals(notify.getReceiverType()) && userId.equals(notify.getReceiverId())) {
                return notify;
            }
            // 对于全局通知，检查用户个人阅读状态
            else if (notify.getIsGlobal() == 1) {
                boolean isRead = userSystemNotifyReadService.isRead(Long.valueOf(userId), notify.getId());
                // 创建一个新的副本以避免修改原始对象
                SystemNotify processedNotify = new SystemNotify();
                try {
                    // 复制所有属性
                    org.springframework.beans.BeanUtils.copyProperties(notify, processedNotify);
                } catch (Exception e) {
                    // 如果复制失败，手动设置关键属性
                    processedNotify.setId(notify.getId());
                    processedNotify.setCreateTime(notify.getCreateTime());
                    processedNotify.setUpdateTime(notify.getUpdateTime());
                    processedNotify.setOperatorId(notify.getOperatorId());
                    processedNotify.setOperatorType(notify.getOperatorType());
                    processedNotify.setNotifyType(notify.getNotifyType());
                    processedNotify.setSenderType(notify.getSenderType());
                    processedNotify.setSenderId(notify.getSenderId());
                    processedNotify.setReceiverType(notify.getReceiverType());
                    processedNotify.setReceiverId(notify.getReceiverId());
                    processedNotify.setTitle(notify.getTitle());
                    processedNotify.setContent(notify.getContent());
                    processedNotify.setNotifyIcon(notify.getNotifyIcon());
                    processedNotify.setRelatedBizType(notify.getRelatedBizType());
                    processedNotify.setRelatedBizId(notify.getRelatedBizId());
                    processedNotify.setExpireTime(notify.getExpireTime());
                    processedNotify.setIsGlobal(notify.getIsGlobal());
                    processedNotify.setIsEnabled(notify.getIsEnabled());
                    processedNotify.setIsDelete(notify.getIsDelete());
                }

                if (isRead) {
                    // 如果用户已读，更新通知的阅读状态
                    processedNotify.setReadStatus(1);
                } else {
                    // 如果用户未读，确保readStatus为0
                    processedNotify.setReadStatus(0);
                }
                return processedNotify;
            }
            return notify;
        }).collect(Collectors.toList());

        // 只显示已读的通知
        processedRecords = processedRecords.stream()
                .filter(notify -> notify.getReadStatus() == 1)
                .collect(Collectors.toList());

        resultPage.setRecords(processedRecords);
        return resultPage;
    }

    @Override
    public Page<SystemNotify> listUserGlobalNotifies(Page<SystemNotify> page, String userId, Integer readStatus) {
        // 先获取所有全局通知
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isGlobal", 1);
        queryWrapper.eq("isDelete", 0).eq("isEnabled", 1);
        queryWrapper.orderByDesc("createTime");

        Page<SystemNotify> resultPage = this.page(page, queryWrapper);

        // 处理用户的阅读状态
        List<SystemNotify> records = resultPage.getRecords();
        List<SystemNotify> processedRecords = records.stream().map(notify -> {
            // 检查用户是否已读此通知
            boolean isRead = userSystemNotifyReadService.isRead(Long.valueOf(userId), notify.getId());
            if (isRead) {
                // 如果用户已读，更新通知的阅读状态
                notify.setReadStatus(1);
            }
            return notify;
        }).collect(Collectors.toList());

        // 如果指定了readStatus过滤条件，则进行过滤
        if (readStatus != null) {
            if (readStatus == 0) {
                // 只显示未读的全局通知（用户未读的）
                processedRecords = processedRecords.stream()
                        .filter(notify -> notify.getReadStatus() == 0)
                        .collect(Collectors.toList());
            } else if (readStatus == 1) {
                // 只显示已读的全局通知（用户已读的）
                processedRecords = processedRecords.stream()
                        .filter(notify -> notify.getReadStatus() == 1)
                        .collect(Collectors.toList());
            }
        }

        resultPage.setRecords(processedRecords);
        return resultPage;
    }

    @Override
    public Page<SystemNotify> getAllNotifiesByUserId(String userId, long current, long pageSize) {
        // 构建查询条件
        QueryWrapper<SystemNotify> queryWrapper = new QueryWrapper<>();
        // 查询指定用户的通知或全局通知
        queryWrapper.and(wrapper -> wrapper.eq("receiverType", "SPECIFIC_USER").eq("receiverId", userId)
                .or().eq("isGlobal", 1));
        // 未删除且有效的通知
        queryWrapper.eq("isDelete", 0).eq("isEnabled", 1);
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<SystemNotify> page = new Page<>(current, pageSize);
        Page<SystemNotify> resultPage = this.page(page, queryWrapper);

        // 处理用户的阅读状态（特别是全局通知）
        List<SystemNotify> records = resultPage.getRecords();
        List<SystemNotify> processedRecords = records.stream().map(notify -> {
            // 对于用户特定通知，直接使用数据库中的readStatus
            if ("SPECIFIC_USER".equals(notify.getReceiverType()) && userId.equals(notify.getReceiverId())) {
                return notify;
            }
            // 对于全局通知，检查用户个人阅读状态
            else if (notify.getIsGlobal() == 1) {
                boolean isRead = userSystemNotifyReadService.isRead(Long.valueOf(userId), notify.getId());
                // 创建一个新的副本以避免修改原始对象
                SystemNotify processedNotify = new SystemNotify();
                try {
                    // 复制所有属性
                    org.springframework.beans.BeanUtils.copyProperties(notify, processedNotify);
                } catch (Exception e) {
                    // 如果复制失败，手动设置关键属性
                    processedNotify.setId(notify.getId());
                    processedNotify.setCreateTime(notify.getCreateTime());
                    processedNotify.setUpdateTime(notify.getUpdateTime());
                    processedNotify.setOperatorId(notify.getOperatorId());
                    processedNotify.setOperatorType(notify.getOperatorType());
                    processedNotify.setNotifyType(notify.getNotifyType());
                    processedNotify.setSenderType(notify.getSenderType());
                    processedNotify.setSenderId(notify.getSenderId());
                    processedNotify.setReceiverType(notify.getReceiverType());
                    processedNotify.setReceiverId(notify.getReceiverId());
                    processedNotify.setTitle(notify.getTitle());
                    processedNotify.setContent(notify.getContent());
                    processedNotify.setNotifyIcon(notify.getNotifyIcon());
                    processedNotify.setRelatedBizType(notify.getRelatedBizType());
                    processedNotify.setRelatedBizId(notify.getRelatedBizId());
                    processedNotify.setExpireTime(notify.getExpireTime());
                    processedNotify.setIsGlobal(notify.getIsGlobal());
                    processedNotify.setIsEnabled(notify.getIsEnabled());
                    processedNotify.setIsDelete(notify.getIsDelete());
                }

                if (isRead) {
                    // 如果用户已读，更新通知的阅读状态
                    processedNotify.setReadStatus(1);
                } else {
                    // 如果用户未读，确保readStatus为0
                    processedNotify.setReadStatus(0);
                }
                return processedNotify;
            }
            return notify;
        }).collect(Collectors.toList());

        resultPage.setRecords(processedRecords);
        return resultPage;
    }
}
