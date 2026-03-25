package com.lumenglover.yuemupicturebackend.model.dto.share;

import lombok.Data;

@Data
public class ShareRequest {
    /**
     * 目标内容ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 是否分享
     */
    private Boolean isShared;
}
