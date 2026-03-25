package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通用点赞表
 */
@TableName(value ="like_record")
@Data
public class LikeRecord implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被点赞内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 被点赞内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 是否点赞
     */
    private Boolean isLiked;

    /**
     * 第一次点赞时间
     */
    private Date firstLikeTime;

    /**
     * 最近一次点赞时间
     */
    private Date lastLikeTime;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Integer isRead;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
