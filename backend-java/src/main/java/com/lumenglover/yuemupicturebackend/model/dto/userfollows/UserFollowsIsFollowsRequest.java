package com.lumenglover.yuemupicturebackend.model.dto.userfollows;

import lombok.Data;

@Data
public class UserFollowsIsFollowsRequest {
    /**
     * 关注者的用户 ID
     */
    private Long followerId;

    /**
     * 被关注者的用户 ID
     */
    private Long followingId;
}
