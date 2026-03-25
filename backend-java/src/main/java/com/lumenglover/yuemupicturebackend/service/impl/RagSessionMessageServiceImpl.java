package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.mapper.RagSessionMessageMapper;
import com.lumenglover.yuemupicturebackend.model.dto.rag.QaMessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagSessionMessage;
import com.lumenglover.yuemupicturebackend.service.RagSessionMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * RAG会话消息服务实现类
 */
@Service
@Slf4j
public class RagSessionMessageServiceImpl extends ServiceImpl<RagSessionMessageMapper, RagSessionMessage> implements RagSessionMessageService {

    @Resource
    private RagSessionMessageMapper ragSessionMessageMapper;

    @Override
    public Long sendMessage(Long sessionId, Long userId, String content, Integer messageType) {
        try {
            log.debug("RAG会话消息服务 - 开始发送消息，sessionId: {}, userId: {}, messageType: {}, content长度: {}", sessionId, userId, messageType, content != null ? content.length() : 0);

            RagSessionMessage message = new RagSessionMessage();
            message.setSessionId(sessionId);
            message.setUserId(userId);
            message.setMessageType(messageType);
            message.setContent(content);
            message.setCreateTime(new Date());
            message.setIsDelete(0);

            boolean saveResult = this.save(message);

            if (!saveResult) {
                log.error("RAG会话消息服务 - 消息保存失败，sessionId: {}, userId: {}, messageType: {}", sessionId, userId, messageType);
                throw new RuntimeException("消息保存失败");
            }

            log.debug("RAG会话消息服务 - 消息发送完成，messageId: {}", message.getId());
            return message.getId();
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 发送消息异常，sessionId: {}, userId: {}, messageType: {}, 异常信息: {}", sessionId, userId, messageType, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public IPage<RagSessionMessage> getMessagePage(QaMessageQueryRequest qaMessageQueryRequest) {
        try {
            log.debug("RAG会话消息服务 - 开始查询消息分页，current: {}, size: {}",
                    qaMessageQueryRequest.getCurrent(), qaMessageQueryRequest.getPageSize());

            long current = qaMessageQueryRequest.getCurrent();
            long size = qaMessageQueryRequest.getPageSize();
            // 限制爬取数量
            if (size > 50) {
                log.warn("RAG会话消息服务 - 分页大小超出限制，当前大小: {}，限制: 50", size);
                throw new RuntimeException("每页数量不能超过50");
            }

            QueryWrapper<RagSessionMessage> queryWrapper = new QueryWrapper<>();

            // 根据会话ID筛选
            if (qaMessageQueryRequest.getSessionId() != null) {
                queryWrapper.eq("sessionId", qaMessageQueryRequest.getSessionId());
                log.debug("RAG会话消息服务 - 添加会话ID筛选条件: {}", qaMessageQueryRequest.getSessionId());
            }

            // 根据用户ID筛选
            if (qaMessageQueryRequest.getUserId() != null) {
                queryWrapper.eq("userId", qaMessageQueryRequest.getUserId());
                log.debug("RAG会话消息服务 - 添加用户ID筛选条件: {}", qaMessageQueryRequest.getUserId());
            }

            // 根据消息类型筛选
            if (qaMessageQueryRequest.getMessageType() != null) {
                queryWrapper.eq("messageType", qaMessageQueryRequest.getMessageType());
                log.debug("RAG会话消息服务 - 添加消息类型筛选条件: {}", qaMessageQueryRequest.getMessageType());
            }

            // 根据是否删除筛选
            if (qaMessageQueryRequest.getIsDelete() != null) {
                queryWrapper.eq("isDelete", qaMessageQueryRequest.getIsDelete());
                log.debug("RAG会话消息服务 - 添加删除状态筛选条件: {}", qaMessageQueryRequest.getIsDelete());
            } else {
                // 默认查询未删除的
                queryWrapper.eq("isDelete", 0);
                log.debug("RAG会话消息服务 - 默认添加未删除筛选条件");
            }

            // 根据请求参数设置排序，如果没有指定则默认按创建时间降序排序（最新的在前）
            if (qaMessageQueryRequest.getSortField() != null && qaMessageQueryRequest.getSortOrder() != null) {
                if ("ascend".equals(qaMessageQueryRequest.getSortOrder())) {
                    queryWrapper.orderByAsc(qaMessageQueryRequest.getSortField());
                    log.debug("RAG会话消息服务 - 设置升序排序字段: {}", qaMessageQueryRequest.getSortField());
                } else {
                    queryWrapper.orderByDesc(qaMessageQueryRequest.getSortField());
                    log.debug("RAG会话消息服务 - 设置降序排序字段: {}", qaMessageQueryRequest.getSortField());
                }
            } else {
                // 默认按创建时间降序排序（最新的在前）
                queryWrapper.orderByDesc("createTime");
                log.debug("RAG会话消息服务 - 默认按创建时间降序排序");
            }

            log.debug("RAG会话消息服务 - 开始执行分页查询");
            IPage<RagSessionMessage> page = this.page(new Page<>(current, size), queryWrapper);
            log.debug("RAG会话消息服务 - 分页查询完成，共查询到 {} 条记录，总页数: {}", page.getTotal(), page.getPages());

            return page;
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 查询消息分页异常，current: {}, size: {}, 异常信息: {}",
                    qaMessageQueryRequest.getCurrent(), qaMessageQueryRequest.getPageSize(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<RagSessionMessage> getMessageListBySessionId(Long sessionId) {
        try {
            log.debug("RAG会话消息服务 - 开始根据会话ID查询消息列表，sessionId: {}", sessionId);

            QueryWrapper<RagSessionMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sessionId", sessionId)
                    .eq("isDelete", 0)
                    .orderByDesc("createTime");

            List<RagSessionMessage> messages = this.list(queryWrapper);

            log.debug("RAG会话消息服务 - 根据会话ID查询消息列表完成，sessionId: {}，查询到 {} 条消息", sessionId, messages.size());

            return messages;
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 根据会话ID查询消息列表异常，sessionId: {}，异常信息: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<RagSessionMessage> getRecentMessagesBySessionId(Long sessionId, int limit) {
        try {
            log.debug("RAG会话消息服务 - 开始查询会话最近消息，sessionId: {}，limit: {}", sessionId, limit);

            QueryWrapper<RagSessionMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sessionId", sessionId)
                    .eq("isDelete", 0)
                    .orderByDesc("createTime")
                    .last("LIMIT " + limit);

            List<RagSessionMessage> messages = this.list(queryWrapper);

            log.debug("RAG会话消息服务 - 查询会话最近消息完成，sessionId: {}，limit: {}，查询到 {} 条消息", sessionId, limit, messages.size());

            return messages;
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 查询会话最近消息异常，sessionId: {}，limit: {}，异常信息: {}", sessionId, limit, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void batchDeleteBySessionId(Long sessionId) {
        try {
            log.debug("RAG会话消息服务 - 开始批量删除会话消息，sessionId: {}", sessionId);

            QueryWrapper<RagSessionMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sessionId", sessionId);
            RagSessionMessage updateMessage = new RagSessionMessage();
            updateMessage.setIsDelete(1);

            boolean updateResult = this.update(updateMessage, queryWrapper);

            log.debug("RAG会话消息服务 - 批量删除会话消息完成，sessionId: {}，更新结果: {}", sessionId, updateResult);
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 批量删除会话消息异常，sessionId: {}，异常信息: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public RagSessionMessage getLatestMessageBySessionId(Long sessionId) {
        try {
            log.debug("RAG会话消息服务 - 开始查询会话最新消息，sessionId: {}", sessionId);

            QueryWrapper<RagSessionMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sessionId", sessionId)
                    .eq("isDelete", 0)
                    .orderByDesc("createTime")
                    .last("LIMIT 1");

            RagSessionMessage latestMessage = this.getOne(queryWrapper, false); // 使用false避免在没有结果时抛出异常

            log.debug("RAG会话消息服务 - 查询会话最新消息完成，sessionId: {}，消息ID: {}", sessionId, latestMessage != null ? latestMessage.getId() : null);

            return latestMessage;
        } catch (Exception e) {
            log.error("RAG会话消息服务 - 查询会话最新消息异常，sessionId: {}，异常信息: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }
}
