package com.lumenglover.yuemupicturebackend.model.dto.activity;

import lombok.Data;

import java.util.Date;

/**
 * 创建活动请求
 */
@Data
public class ActivityAddRequest {
    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 活动过期时间
     */
    private Date expireTime;


}
