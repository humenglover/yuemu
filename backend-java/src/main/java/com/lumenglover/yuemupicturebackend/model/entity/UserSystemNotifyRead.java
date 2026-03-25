package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户系统通知阅读状态
 * 用于记录用户对全局系统通知的阅读状态
 * @TableName user_system_notify_read
 */
@TableName(value = "user_system_notify_read")
@Data
public class UserSystemNotifyRead implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 系统通知ID
     */
    private Long systemNotifyId;

    /**
     * 阅读状态[0:未读, 1:已读]
     */
    private Integer readStatus;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
