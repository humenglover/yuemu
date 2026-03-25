package com.lumenglover.yuemupicturebackend.model.dto.userfollows;

import lombok.Data;

@Data
public class UserFollowsAddRequest {
    /**
     * 关注者的用户 ID
     */
    private Long followerId;


    /**
     * 被关注者的用户 ID
     */
    private Long followingId;


    /**
     * 关注状态，0 表示取消关注，1 表示关注
     */
    private Integer followStatus;
}
