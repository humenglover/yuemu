package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.model.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
* @author 鹿梦
* @description 针对表【activity(活动表)】的数据库操作Service
* @createDate 2024-03-20
*/
public interface ActivityService extends IService<Activity> {
    /**
     * 创建活动（仅管理员）
     */
    Long addActivity(ActivityAddRequest activityAddRequest, User loginUser);

    /**
     * 分页获取活动列表
     */
    Page<Activity> listActivities(ActivityQueryRequest request, User loginUser);

    /**
     * 审核活动（仅管理员）
     */
    void reviewActivity(Long activityId, Integer status, String message, User loginUser);

    /**
     * 获取活动详情（带浏览量统计）
     */
    Activity getActivityDetail(Long id, User loginUser, HttpServletRequest request);

    /**
     * 获取活动浏览量
     */
    long getViewCount(Long activityId);

    /**
     * 填充活动信息（用户、附件等）
     */
    void fillActivityInfo(Activity activity);

    /**
     * 获取轮播图活动列表（未过期且已审核通过的）
     */
    Page<Activity> listCarouselActivities(ActivityQueryRequest request);
}
