package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.mapper.MessageBoardMapper;
import com.lumenglover.yuemupicturebackend.model.entity.MessageBoard;
import com.lumenglover.yuemupicturebackend.service.MessageBoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 祝福板服务实现类
 */
@Service
public class MessageBoardServiceImpl extends ServiceImpl<MessageBoardMapper, MessageBoard> implements MessageBoardService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMessage(MessageBoard messageBoard) {
        // 验证必填字段
        if (messageBoard.getOwnerId() == null) {
            throw new RuntimeException("祝福板主人ID不能为空");
        }
        // 设置默认值
        if (messageBoard.getStatus() == null) {
            messageBoard.setStatus(1);
        }
        if (messageBoard.getLikeCount() == null) {
            messageBoard.setLikeCount(0);
        }
        return save(messageBoard);
    }

    @Override
    public Page<MessageBoard> listMessagesByPage(long current, long size, Long ownerId) {
        QueryWrapper<MessageBoard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ownerId", ownerId)
                .eq("status", 1)
                .orderByDesc("createTime");
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeMessage(Long id) {
        return update().setSql("likeCount = likeCount + 1")
                .eq("id", id)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMessageStatus(Long id, Integer status) {
        MessageBoard messageBoard = new MessageBoard();
        messageBoard.setId(id);
        messageBoard.setStatus(status);
        return updateById(messageBoard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long id, Long ownerId) {
        // 验证是否是祝福板主人
        MessageBoard messageBoard = getById(id);
        if (messageBoard == null) {
            throw new RuntimeException("祝福不存在");
        }
        if (!messageBoard.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该祝福");
        }
        return removeById(id);
    }
}
