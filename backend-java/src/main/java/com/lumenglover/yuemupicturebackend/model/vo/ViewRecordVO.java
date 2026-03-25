package com.lumenglover.yuemupicturebackend.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 浏览记录视图对象
 */
@Data
public class ViewRecordVO implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型（1-图片，2-帖子等）
     */
    private Integer targetType;

    /**
     * 浏览时长（秒）
     */
    private Integer viewDuration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 目标标题（用于显示）
     */
    private String targetTitle;

    /**
     * 目标封面图（用于显示）
     */
    private String targetCover;

    /**
     * 目标作者用户名
     */
    private String targetAuthorUsername;

    /**
     * 目标作者头像
     */
    private String targetAuthorAvatar;
}
