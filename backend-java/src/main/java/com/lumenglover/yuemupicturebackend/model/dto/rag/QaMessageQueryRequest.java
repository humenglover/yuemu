package com.lumenglover.yuemupicturebackend.model.dto.rag;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 问答消息查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QaMessageQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 消息类型 1-用户提问 2-AI回答
     */
    private Integer messageType;

    /**
     * 是否删除 0-未删 1-已删
     */
    private Integer isDelete;
}
