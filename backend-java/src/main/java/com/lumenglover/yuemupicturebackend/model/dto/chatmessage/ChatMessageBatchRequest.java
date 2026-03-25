package com.lumenglover.yuemupicturebackend.model.dto.chatmessage;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量操作聊天消息请求
 */
@Data
public class ChatMessageBatchRequest implements Serializable {

    /**
     * 需要操作的消息ID列表
     */
    private List<Long> ids;

    /**
     * 操作类型（delete-逻辑删除，recover-恢复，physical-物理删除）
     */
    private String operation;

    private static final long serialVersionUID = 1L;
} 