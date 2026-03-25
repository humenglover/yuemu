package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 *
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 简介
     */
    private String introduction;

    private static final long serialVersionUID = 1L;
}
