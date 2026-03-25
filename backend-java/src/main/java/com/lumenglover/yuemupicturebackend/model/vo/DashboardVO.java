package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据看板视图对象
 */
@Data
public class DashboardVO implements Serializable {

    /**
     * 当天新增用户数
     */
    private Long newUsers;

    /**
     * 当天新增图片数
     */
    private Long newPictures;

    /**
     * 当天新增帖子数
     */
    private Long newPosts;

    /**
     * 当天新增空间数
     */
    private Long newSpaces;

    /**
     * 当天新增活动数
     */
    private Long newActivities;

    /**
     * 当天新增恋爱空间数
     */
    private Long newLoveBoards;

    /**
     * 当天新增友链数
     */
    private Long newFriendLinks;

    /**
     * 当天新增留言板数
     */
    private Long newMessages;

    /**
     * 网站总访问量
     */
    private Long totalViews;

    /**
     * 当天新增音频数
     */
    private Long newAudioFiles;

    /**
     * 当天新增举报数
     */
    private Long newReports;

    /**
     * 当天新增会话数
     */
    private Long newChatMessages;

    /**
     * 当天新增bug报告数
     */
    private Long newBugReports;

    /**
     * bug报告总数
     */
    private Long totalBugReports;

    private static final long serialVersionUID = 1L;
}
