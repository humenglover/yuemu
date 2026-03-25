package com.lumenglover.yuemupicturebackend.model.dto.like;

import lombok.Data;

@Data
public class LikeRequest {
    /**
     * 目标内容ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 是否点赞
     */
    private Boolean isLiked;
}
