package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片权限设置请求 DTO
 *
 * @author 鹿梦
 * @date 2024/12/20
 */
@Data
public class PicturePermissionRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 是否允许收藏：1-允许、0-禁止
     */
    private Integer allowCollect;

    /**
     * 是否允许点赞：1-允许、0-禁止
     */
    private Integer allowLike;

    /**
     * 是否允许评论：1-允许、0-禁止
     */
    private Integer allowComment;

    /**
     * 是否允许分享：1-允许、0-禁止
     */
    private Integer allowShare;

    private static final long serialVersionUID = 1L;
}
