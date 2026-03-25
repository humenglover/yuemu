package com.lumenglover.yuemupicturebackend.model.dto.rag;

import lombok.Data;

import java.io.Serializable;

/**
 * 会话删除请求
 */
@Data
public class SessionDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private Long sessionId;
}