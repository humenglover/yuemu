package com.lumenglover.yuemupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 设置推荐成员请求
 */
@Data
public class SpaceUserRecommendRequest implements Serializable {

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否推荐：0-否 1-是
     */
    private Integer isRecommended;

    private static final long serialVersionUID = 1L;
}
