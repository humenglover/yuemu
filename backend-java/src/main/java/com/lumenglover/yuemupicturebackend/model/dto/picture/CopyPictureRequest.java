package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 复制图片请求参数
 *
 * @author 鹿梦
 * @date 2026/1/22
 */
@Data
public class CopyPictureRequest implements Serializable {

    /**
     * 原图片ID
     */
    private Long pictureId;

    /**
     * 目标空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
