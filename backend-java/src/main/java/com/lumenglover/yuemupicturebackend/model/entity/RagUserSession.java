package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能客服用户会话表
 * @TableName rag_user_session
 */
@TableName(value = "rag_user_session")
@Data
public class RagUserSession implements Serializable {
    /**
     * 会话ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 会话名称（默认：新会话+时间戳）
     */
    @TableField(value = "sessionName")
    private String sessionName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 最后消息时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否为当前活跃会话 0-否 1-是
     */
    @TableField(value = "isActive")
    private Integer isActive;

    /**
     * 是否删除 0-未删 1-已删
     */
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
