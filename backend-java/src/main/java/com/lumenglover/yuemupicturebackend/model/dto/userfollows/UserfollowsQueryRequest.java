package com.lumenglover.yuemupicturebackend.model.dto.userfollows;

import lombok.Data;

@Data
public class UserfollowsQueryRequest {
    /**
     * 当前页号
     */
    private long current;

    /**
     * 页面大小
     */
    private long pageSize;

    /**
     * 关注者id
     */
    private Long followerId;

    /**
     * 被关注者id
     */
    private Long followingId;

    /**
     * 搜索类型 0-关注列表 1-粉丝列表
     */
    private int searchType;

    /**
     * 用户名搜索关键词
     */
    private String userNameKeyword;

    /**
     * 用户账号搜索关键词
     */
    private String userAccountKeyword;

    /**
     * 用户简介搜索关键词
     */
    private String userProfileKeyword;
}
