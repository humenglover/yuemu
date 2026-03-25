package com.lumenglover.yuemupicturebackend.model.dto.viewrecord;

import lombok.Data;

import java.io.Serializable;

/**
 * 上报浏览时长请求
 */
@Data
public class ReportViewDurationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被浏览内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间 4-用户
     */
    private Integer targetType;

    /**
     * 浏览时长(毫秒)
     */
    private Long duration;

    /**
     * 客户端时间戳
     */
    private Long clientTimestamp;
}
