package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能客服会话消息表
 * @TableName rag_session_message
 */
@TableName(value = "rag_session_message")
@Data
public class RagSessionMessage implements Serializable {
    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联会话ID
     */
    @TableField(value = "sessionId")
    private Long sessionId;

    /**
     * 发送用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 消息类型 1-用户提问 2-AI回答
     */
    @TableField(value = "messageType")
    private Integer messageType;

    /**
     * 消息内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 是否删除 0-未删 1-已删
     */
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
