package com.lumenglover.yuemupicturebackend.model.dto.activity;

import com.lumenglover.yuemupicturebackend.model.dto.post.PostAttachmentRequest;
import lombok.Data;

import java.util.Date;
import java.util.List;

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
     * 活动过期时间
     */
    private Date expireTime;

    /**
     * 图片附件列表
     */
    private List<PostAttachmentRequest> attachments;
}
