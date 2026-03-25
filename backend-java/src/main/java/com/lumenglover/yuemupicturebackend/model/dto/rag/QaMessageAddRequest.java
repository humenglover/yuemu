package com.lumenglover.yuemupicturebackend.model.dto.rag;

import lombok.Data;

import java.io.Serializable;

/**
 * 问答消息添加请求
 */
@Data
public class QaMessageAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 消息内容
     */
    private String content;
}
