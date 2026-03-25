package com.lumenglover.yuemupicturebackend.model.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Python RAG请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonRagRequest {

    /**
     * 历史对话记录
     */
    private List<Map<String, String>> history;

    /**
     * 当前问题
     */
    private String question;

    /**
     * 会话ID
     */
    private String session_id;

    /**
     * Top-K检索数量
     */
    private Integer top_k;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 超长记忆（历史摘要或相关对话）
     */
    private String longTermMemory;
}
