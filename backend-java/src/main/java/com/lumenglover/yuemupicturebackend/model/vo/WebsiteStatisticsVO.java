package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

/**
 * 网站统计数据视图对象
 */
@Data
public class WebsiteStatisticsVO {

    /**
     * 今日浏览量
     */
    private Long dailyViews;

    /**
     * 今日新增图片数
     */
    private Long dailyNewPictures;

    /**
     * 今日新增空间数
     */
    private Long dailyNewSpaces;

    /**
     * 今日新增帖子数
     */
    private Long dailyNewPosts;

    /**
     * 今日新增友链数
     */
    private Long dailyNewFriendLinks;

    /**
     * 今日新增用户数
     */
    private Long dailyNewUsers;
}
