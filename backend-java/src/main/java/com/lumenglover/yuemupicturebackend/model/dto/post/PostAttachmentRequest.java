package com.lumenglover.yuemupicturebackend.model.dto.post;

import lombok.Data;

@Data
public class PostAttachmentRequest {
    private Integer type; // 1-图片 2-文件
    private String url;
    private String name;
    private Long size;
    private Integer sort;
}
