package com.lumenglover.yuemupicturebackend.service;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * RAG服务接口
 */
@Service
public interface RagService {

    /**
     * RAG问答处理
     */
    String chat(Long userId, Long sessionId, String question);

    /**
     * RAG流式问答处理
     */
    void chatStream(Long userId, Long sessionId, String question, Consumer<String> tokenConsumer, Runnable onComplete);

    /**
     * 清除用户对话上下文
     */
    void clearUserContext(Long userId);

    /**
     * 清除指定会话的上下文
     */
    void clearSessionContext(Long sessionId);

    /**
     * 异步检查并生成会话摘要
     * @param sessionId
     * @param userId
     */
    void checkAndGenerateSummaryAsync(Long sessionId, Long userId);
}
