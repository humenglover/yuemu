package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.mapper.UserSystemNotifyReadMapper;
import com.lumenglover.yuemupicturebackend.model.entity.UserSystemNotifyRead;
import com.lumenglover.yuemupicturebackend.service.UserSystemNotifyReadService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户系统通知阅读状态服务实现类
 */
@Service
public class UserSystemNotifyReadServiceImpl extends ServiceImpl<UserSystemNotifyReadMapper, UserSystemNotifyRead>
        implements UserSystemNotifyReadService {

    @Override
    public boolean isRead(Long userId, Long systemNotifyId) {
        QueryWrapper<UserSystemNotifyRead> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("systemNotifyId", systemNotifyId)
                .eq("readStatus", 1)
                .eq("isDelete", 0);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean markAsRead(Long userId, Long systemNotifyId) {
        // 检查是否已存在记录
        QueryWrapper<UserSystemNotifyRead> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("systemNotifyId", systemNotifyId)
                .eq("isDelete", 0);

        UserSystemNotifyRead existRecord = this.getOne(queryWrapper);

        if (existRecord != null) {
            // 如果已存在，更新状态
            existRecord.setReadStatus(1);
            existRecord.setReadTime(new Date());
            return this.updateById(existRecord);
        } else {
            // 如果不存在，创建新记录
            UserSystemNotifyRead userSystemNotifyRead = new UserSystemNotifyRead();
            userSystemNotifyRead.setUserId(userId);
            userSystemNotifyRead.setSystemNotifyId(systemNotifyId);
            userSystemNotifyRead.setReadStatus(1);
            userSystemNotifyRead.setReadTime(new Date());
            userSystemNotifyRead.setCreateTime(new Date());
            userSystemNotifyRead.setUpdateTime(new Date());
            return this.save(userSystemNotifyRead);
        }
    }

    @Override
    public long getUserUnreadGlobalNotifyCount(Long userId) {
        // 查询用户未读的全局通知数量
        QueryWrapper<UserSystemNotifyRead> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("readStatus", 0)
                .eq("isDelete", 0);
        return this.count(queryWrapper);
    }
}
