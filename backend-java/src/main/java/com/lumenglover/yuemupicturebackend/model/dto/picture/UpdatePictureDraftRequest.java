package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新图片草稿状态请求
 */
@Data
public class UpdatePictureDraftRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 是否为草稿状态：0-非草稿 1-草稿
     */
    private Integer isDraft;

    private static final long serialVersionUID = 1L;
}
