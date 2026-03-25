package com.lumenglover.yuemupicturebackend.model.dto.like;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LikeAdminRequest extends PageRequest {
    /**
     * 点赞记录ID
     */
    private Long id;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 目标内容ID
     */
    private Long targetId;

    /**
     * 目标类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 目标内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 是否点赞
     */
    private Boolean isLiked;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder;
}
