package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

@Data
public class PictureFeatureRequest {
    /**
     * 图片id
     */
    private Long id;

    /**
     * 是否精选 0-非精选 1-精选
     */
    private Integer isFeature;
}
