package com.lumenglover.yuemupicturebackend.model.dto.favoriterecord;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收藏记录查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteRecordQueryRequest extends PageRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 是否收藏
     */
    private Boolean isFavorite;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Boolean isRead;

    private static final long serialVersionUID = 1L;
}
