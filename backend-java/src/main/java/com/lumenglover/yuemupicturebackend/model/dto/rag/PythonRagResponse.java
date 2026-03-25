package com.lumenglover.yuemupicturebackend.model.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Python RAG响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonRagResponse {

    /**
     * AI回答
     */
    private String answer;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Top-K检索数量
     */
    private Integer top_k;

    /**
     * 温度参数
     */
    private Double temperature;
}
