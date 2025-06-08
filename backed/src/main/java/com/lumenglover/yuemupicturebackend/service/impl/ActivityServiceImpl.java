package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.mapper.ActivityMapper;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.activity.ActivityQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostAttachmentRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Resource
    private PostAttachmentService postAttachmentService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    @Lazy
    private ShareRecordService shareRecordService;

    @Resource
    private CrawlerManager crawlerManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActivity(ActivityAddRequest activityAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(activityAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        String title = activityAddRequest.getTitle();
        String content = activityAddRequest.getContent();
        String coverUrl = activityAddRequest.getCoverUrl();
        Date expireTime = activityAddRequest.getExpireTime();
        List<PostAttachmentRequest> attachments = activityAddRequest.getAttachments();

        // 标题校验
        ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(title.length() > 100, ErrorCode.PARAMS_ERROR, "标题最多100字");

        // 内容校验
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");

        // 封面校验
        ThrowUtils.throwIf(StrUtil.isBlank(coverUrl), ErrorCode.PARAMS_ERROR, "封面不能为空");

        // 过期时间校验
        ThrowUtils.throwIf(expireTime == null, ErrorCode.PARAMS_ERROR, "过期时间不能为空");
        ThrowUtils.throwIf(expireTime.before(new Date()), ErrorCode.PARAMS_ERROR, "过期时间不能早于当前时间");

        // 处理内容中的图片标记
        if (CollUtil.isNotEmpty(attachments)) {
            for (int i = 0; i < attachments.size(); i++) {
                PostAttachmentRequest attach = attachments.get(i);
                if (attach.getType() == 1) { // 图片类型
                    String marker = "{img-" + (i + 1) + "}";
                    // 确保 marker 在内容中存在
                    ThrowUtils.throwIf(!content.contains(marker),
                            ErrorCode.PARAMS_ERROR, "图片标记 " + marker + " 未在内容中找到");
                }
            }
        }

        // 创建活动
        Activity activity = new Activity();
        BeanUtils.copyProperties(activityAddRequest, activity);
        activity.setUserId(loginUser.getId());
        // 管理员创建的活动自动通过审核
        activity.setStatus(1); // 已审核通过
        activity.setIsExpired(0); // 未过期
        activity.setReviewMessage("管理员创建自动通过");

        // 保存活动
        boolean success = this.save(activity);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 处理附件时，对于图片类型，使用缩略图URL
        if (CollUtil.isNotEmpty(attachments)) {
            List<PostAttachment> postAttachments = attachments.stream()
                    .map(attach -> {
                        PostAttachment attachment = new PostAttachment();
                        BeanUtils.copyProperties(attach, attachment);
                        attachment.setPostId(activity.getId());
                        if (attach.getType() == 1) { // 图片类型
                            String marker = "{img-" + (attachments.indexOf(attach) + 1) + "}";
                            attachment.setMarker(marker);
                            attachment.setPosition(content.indexOf(marker));
                            // 将原始URL转换为缩略图URL
                            String thumbnailUrl = attach.getUrl().replace("/public/", "/thumbnail/");
                            attachment.setUrl(thumbnailUrl);
                        }
                        return attachment;
                    }).collect(Collectors.toList());
            postAttachmentService.saveBatch(postAttachments);
        }

        return activity.getId();
    }

    @Override
    public Page<Activity> listActivities(ActivityQueryRequest request, User loginUser) {
        long current = request.getCurrent();
        long size = request.getPageSize();

        // 构建查询条件
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();

        // 搜索词
        String searchText = request.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("title", searchText).or().like("content", searchText);
        }

        // 处理查询范围
        boolean isPublic = request.getIsPublic();
        boolean isAdmin = loginUser != null && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

        if (isPublic || !isAdmin) {
            // 公共查询或非管理员，只显示已发布的活动
            queryWrapper.eq("status", 1);
        } else {
            // 管理员查询所有状态的活动
            Integer status = request.getStatus();
            if (status != null) {
                queryWrapper.eq("status", status);
            }
        }

        // 是否只看未过期
        if (Boolean.TRUE.equals(request.getNotExpired())) {
            queryWrapper.eq("isExpired", 0);
        }

        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");

        // 执行查询
        Page<Activity> activityPage = this.page(new Page<>(current, size), queryWrapper);

        // 填充活动信息
        activityPage.getRecords().forEach(this::fillActivityInfo);

        return activityPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewActivity(Long activityId, Integer status, String message, User loginUser) {
        // 参数校验
        Activity activity = this.getById(activityId);
        ThrowUtils.throwIf(activity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 校验权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新审核状态
        Activity updateActivity = new Activity();
        updateActivity.setId(activityId);
        updateActivity.setStatus(status);
        updateActivity.setReviewMessage(message);

        boolean success = this.updateById(updateActivity);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Activity getActivityDetail(Long id, User loginUser, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 检测爬虫
        crawlerManager.detectNormalRequest(request);

        // 获取活动信息
        Activity activity = this.getById(id);
        ThrowUtils.throwIf(activity == null, ErrorCode.NOT_FOUND_ERROR);

        // 增加浏览量
        incrementViewCount(id, request);

        // 获取附件并按位置排序
        List<PostAttachment> attachments = postAttachmentService.list(new QueryWrapper<PostAttachment>()
                .eq("postId", id)
                .orderByAsc("position"));

        // 替换内容中的图片标记为缩略图URL
        String content = activity.getContent();
        for (PostAttachment attachment : attachments) {
            if (attachment.getType() == 1 && StrUtil.isNotBlank(attachment.getMarker())) {
                content = content.replace(attachment.getMarker(),
                        String.format("![%s](%s)", attachment.getName(), attachment.getUrl()));
            }
        }
        activity.setContent(content);
        activity.setAttachments(attachments);

        // 填充用户信息
        User user = userService.getById(activity.getUserId());
        activity.setUser(userService.getUserVO(user));

        // 设置点赞和分享状态
        if (loginUser != null) {
            boolean isLiked = likeRecordService.isContentLiked(activity.getId(), 2, loginUser.getId());
            activity.setIsLiked(isLiked ? 1 : 0);
            boolean isShared = shareRecordService.isContentShared(activity.getId(), 2, loginUser.getId());
            activity.setIsShared(isShared ? 1 : 0);
        } else {
            activity.setIsLiked(0);
            activity.setIsShared(0);
        }

        // 获取最新的浏览量
        long realViewCount = getViewCount(id);
        activity.setViewCount(realViewCount);

        return activity;
    }

    @Override
    public long getViewCount(Long activityId) {
        // 先从 Redis 获取增量
        String viewCountKey = String.format("activity:viewCount:%d", activityId);
        String incrementCount = stringRedisTemplate.opsForValue().get(viewCountKey);

        // 从数据库获取基础浏览量
        Activity activity = this.getById(activityId);
        if (activity == null) {
            return 0L;
        }

        // 合并数据库和 Redis 的浏览量
        long baseCount = activity.getViewCount() != null ? activity.getViewCount() : 0L;
        long increment = incrementCount != null ? Long.parseLong(incrementCount) : 0L;

        return baseCount + increment;
    }

    @Override
    public void fillActivityInfo(Activity activity) {
        // 填充用户信息
        User user = userService.getById(activity.getUserId());
        if (user != null) {
            activity.setUser(userService.getUserVO(user));
        }

        // 获取实时浏览量（合并 Redis 中的增量）
        long realViewCount = getViewCount(activity.getId());
        activity.setViewCount(realViewCount);

        // 清空内容，只在详情页显示
        activity.setContent(null);
    }

    @Override
    public Page<Activity> listCarouselActivities(ActivityQueryRequest request) {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)  // 已发布
                .eq("isExpired", 0)   // 未过期
                .eq("isDelete", 0)
                .orderByDesc("createTime")
                // 只选择需要的字段
                .select("id", "title", "coverUrl", "expireTime", "isExpired", "viewCount");

        Page<Activity> activityPage = this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);

        // 获取实时浏览量
        activityPage.getRecords().forEach(activity -> {
            long realViewCount = getViewCount(activity.getId());
            activity.setViewCount(realViewCount);
        });

        return activityPage;
    }

    /**
     * 异步增加活动浏览量
     */
    @Async("asyncExecutor")
    public void incrementViewCount(Long activityId, HttpServletRequest request) {
        // 检查是否需要增加浏览量
        if (!crawlerManager.detectViewRequest(request, activityId)) {
            return;
        }

        // 使用 Redis 进行计数
        String viewCountKey = String.format("activity:viewCount:%d", activityId);
        String lockKey = String.format("activity:viewCount:lock:%d", activityId);

        try {
            // 获取分布式锁
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 增加浏览量
                stringRedisTemplate.opsForValue().increment(viewCountKey);

                // 当浏览量达到一定阈值时，更新数据库
                String viewCountStr = stringRedisTemplate.opsForValue().get(viewCountKey);
                if (viewCountStr != null && Long.parseLong(viewCountStr) % 100 == 0) {
                    this.update()
                            .setSql("viewCount = viewCount + " + viewCountStr)
                            .eq("id", activityId)
                            .update();
                    // 更新后重置 Redis 计数
                    stringRedisTemplate.delete(viewCountKey);
                }
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }
}
