package com.lumenglover.yuemupicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * 图片搜索结果
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
