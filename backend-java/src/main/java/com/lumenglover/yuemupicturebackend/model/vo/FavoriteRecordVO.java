package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 收藏记录视图对象
 */
@Data
public class FavoriteRecordVO {
    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 最近收藏时间
     */
    private Date lastFavoriteTime;

    /**
     * 收藏用户信息
     */
    private UserVO user;

    /**
     * 内容类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 被收藏内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 被收藏的内容（根据targetType可能是PictureVO/Post/SpaceVO）
     */
    private Object target;

    /**
     * 是否已读
     */
    private Integer isRead;
}
