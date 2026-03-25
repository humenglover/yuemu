package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户权限更新请求
 */
@Data
public class UserUpdatePermissionsRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否允许私聊：1-允许、0-禁止
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注：1-允许、0-禁止
     */
    private Integer allowFollow;

    /**
     * 是否展示关注列表：1-展示、0-隐藏
     */
    private Integer showFollowList;

    /**
     * 是否展示粉丝列表：1-展示、0-隐藏
     */
    private Integer showFansList;

    private static final long serialVersionUID = 1L;
}
