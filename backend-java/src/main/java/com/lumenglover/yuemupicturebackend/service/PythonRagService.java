package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.dto.rag.PythonRagResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Python RAG服务接口
 */
public interface PythonRagService {

    /**
     * 调用Python RAG同步接口
     *
     * @param question      问题
     * @param history       历史对话
     * @param sessionId     会话ID
     * @param ltmContext    超长记忆上下文
     * @return Python RAG响应
     */
    PythonRagResponse callPythonRagSync(String question, List<Map<String, String>> history, String sessionId, String ltmContext);

    /**
     * 调用Python RAG流式接口
     *
     * @param question      问题
     * @param history       历史对话
     * @param sessionId     会话ID
     * @param ltmContext    超长记忆上下文
     * @param tokenConsumer 流式响应消费者
     * @param onComplete    完成回调
     */
    void callPythonRagStream(String question, List<Map<String, String>> history, String sessionId, String ltmContext, Consumer<String> tokenConsumer, Runnable onComplete);

    /**
     * 调用专用摘要生成接口（绕过 RAG 约束）
     *
     * @param text 待总结的原始文本
     * @return 摘要结果
     */
    PythonRagResponse callPythonSummarize(String text);
}
