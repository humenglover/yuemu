package com.lumenglover.yuemupicturebackend.model.dto.favoriterecord;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加收藏记录请求
 */
@Data
public class FavoriteRecordAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private Boolean isFavorite;
}