package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.rag.QaMessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagSessionMessage;

import java.util.List;

/**
 * RAG会话消息服务接口
 */
public interface RagSessionMessageService extends IService<RagSessionMessage> {

    /**
     * 发送消息
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     * @param messageType 消息类型 1-用户提问 2-AI回答
     * @return 消息ID
     */
    Long sendMessage(Long sessionId, Long userId, String content, Integer messageType);

    /**
     * 查询会话消息列表
     *
     * @param messageQueryRequest 查询条件
     * @return 消息分页列表
     */
    IPage<RagSessionMessage> getMessagePage(QaMessageQueryRequest messageQueryRequest);

    /**
     * 获取会话消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<RagSessionMessage> getMessageListBySessionId(Long sessionId);

    /**
     * 获取会话最近消息
     *
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 最近消息列表
     */
    List<RagSessionMessage> getRecentMessagesBySessionId(Long sessionId, int limit);

    /**
     * 批量删除会话消息
     *
     * @param sessionId 会话ID
     */
    void batchDeleteBySessionId(Long sessionId);

    /**
     * 获取会话最新消息
     *
     * @param sessionId 会话ID
     * @return 最新消息
     */
    RagSessionMessage getLatestMessageBySessionId(Long sessionId);
}
