package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 收藏记录表
 * @TableName favorite_record
 */
@TableName(value = "favorite_record")
@Data
public class FavoriteRecord implements Serializable {
    /**
     * 收藏记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被收藏内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 被收藏内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 是否收藏
     */
    private Boolean isFavorite = false;

    /**
     * 收藏时间
     */
    private Date favoriteTime;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Boolean isRead;

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
