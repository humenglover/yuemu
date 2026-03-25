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
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
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

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SystemNotifyService systemNotifyService;

    @Resource
    private SensitiveUtil sensitiveUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActivity(ActivityAddRequest activityAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(activityAddRequest == null, ErrorCode.PARAMS_ERROR);

        String title = activityAddRequest.getTitle();
        String content = activityAddRequest.getContent();
        String coverUrl = activityAddRequest.getCoverUrl();
        Long spaceId = activityAddRequest.getSpaceId();
        Date expireTime = activityAddRequest.getExpireTime();


        // 标题校验
        ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(title.length() > 100, ErrorCode.PARAMS_ERROR, "标题最多100字");

        // 内容校验
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");

        // 封面校验
        ThrowUtils.throwIf(StrUtil.isBlank(coverUrl), ErrorCode.PARAMS_ERROR, "封面不能为空");

        // 如果没有指定空间ID，需要是系统管理员才能发布全局活动
        if (spaceId == null) {
            boolean isSystemAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isSystemAdmin, ErrorCode.NO_AUTH_ERROR, "非系统管理员无法创建全局活动");
        }

        // 过期时间校验
        ThrowUtils.throwIf(expireTime == null, ErrorCode.PARAMS_ERROR, "过期时间不能为空");
        ThrowUtils.throwIf(expireTime.before(new Date()), ErrorCode.PARAMS_ERROR, "过期时间不能早于当前时间");

        // 检查空间活动数量限制
        checkSpaceActivityLimit(spaceId);



        // 对标题和内容进行敏感词过滤
        String filteredTitle = sensitiveUtil.filter(title);
        String filteredContent = sensitiveUtil.filter(content);

        // 创建活动
        Activity activity = new Activity();
        BeanUtils.copyProperties(activityAddRequest, activity);
        activity.setTitle(filteredTitle);
        activity.setContent(filteredContent);
        activity.setUserId(loginUser.getId());
        activity.setSpaceId(spaceId);

        // 所有活动经过敏感词过滤后直接审核通过
        activity.setStatus(1); // 已审核通过
        activity.setReviewMessage("内容已通过敏感词过滤，自动审核通过");
        activity.setIsExpired(0); // 未过期

        // 保存活动
        boolean success = this.save(activity);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);



        // 检查是否为系统管理员，用于决定是否发送通知
        boolean isSystemAdmin = userService.isAdmin(loginUser);
        if (!isSystemAdmin) {
            // 发送通知给活动创建者，告知其活动已发布
            sendActivityPublishedNotification(activity, loginUser);
        }

        return activity.getId();
    }

    @Override
    public void checkSpaceActivityLimit(Long spaceId) {
        // 如果是全局活动（spaceId为null），不需要限制
        if (spaceId == null) {
            return;
        }

        // 查询该空间当前进行中的活动数量
        // 进行中的活动：status为1（已审核通过）且isExpired为0（未过期）且isDelete为0（未删除）
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId)
                .eq("status", 1)  // 已审核通过
                .eq("isExpired", 0)  // 未过期
                .eq("isDelete", 0)  // 未删除
                .ne("id", 0);  // 确保ID不为0

        long currentActivityCount = this.count(queryWrapper);

        // 如果当前活动数量达到或超过10个，抛出异常
        if (currentActivityCount >= 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该空间当前活动数量已达到上限（10个），无法创建新活动");
        }
    }

    /**
     * 发送活动发布通知给创建者
     */
    private void sendActivityPublishedNotification(Activity activity, User creator) {
        // 创建系统通知
        SystemNotify notify = new SystemNotify();
        notify.setNotifyType("ACTIVITY_PUBLISHED");
        notify.setTitle("活动发布通知");
        notify.setContent("您创建的活动《" + activity.getTitle() + "》已通过审核并成功发布！");
        notify.setReceiverType("SPECIFIC_USER");
        notify.setReceiverId(String.valueOf(creator.getId()));
        notify.setIsGlobal(0);
        notify.setIsEnabled(1);
        notify.setReadStatus(0);
        notify.setOperatorId(String.valueOf(creator.getId()));
        notify.setOperatorType("USER");
        notify.setSenderId(String.valueOf(creator.getId()));
        notify.setSenderType("USER");

        systemNotifyService.addSystemNotify(notify);
    }

    /**
     * 发送活动审核通知给创建者
     */
    private void sendActivityReviewNotification(Activity activity, User creator) {
        // 创建系统通知
        SystemNotify notify = new SystemNotify();
        notify.setNotifyType("ACTIVITY_REVIEW");
        notify.setTitle("活动审核通知");
        notify.setContent("您创建的活动《" + activity.getTitle() + "》需要管理员审核，请耐心等待。");
        notify.setReceiverType("SPECIFIC_USER");
        notify.setReceiverId(String.valueOf(creator.getId()));
        notify.setIsGlobal(0);
        notify.setIsEnabled(1);
        notify.setReadStatus(0);
        notify.setOperatorId(String.valueOf(creator.getId()));
        notify.setOperatorType("USER");
        notify.setSenderId(String.valueOf(creator.getId()));
        notify.setSenderType("USER");

        systemNotifyService.addSystemNotify(notify);
    }

    /**
     * 发送活动审核通过通知给创建者
     */
    private void sendActivityApprovedNotification(Activity activity, User reviewer) {
        // 获取活动创建者信息
        User creator = userService.getById(activity.getUserId());
        if (creator == null) {
            return; // 创建者不存在，不发送通知
        }

        // 创建系统通知
        SystemNotify notify = new SystemNotify();
        notify.setNotifyType("ACTIVITY_APPROVED");
        notify.setTitle("活动审核通过通知");
        notify.setContent("您创建的活动《" + activity.getTitle() + "》已通过审核，现在可以正常展示了！");
        notify.setReceiverType("SPECIFIC_USER");
        notify.setReceiverId(String.valueOf(creator.getId()));
        notify.setIsGlobal(0);
        notify.setIsEnabled(1);
        notify.setReadStatus(0);
        notify.setOperatorId(String.valueOf(reviewer.getId()));
        notify.setOperatorType("ADMIN");
        notify.setSenderId(String.valueOf(reviewer.getId()));
        notify.setSenderType("ADMIN");

        systemNotifyService.addSystemNotify(notify);
    }

    /**
     * 检查并更新活动过期状态
     *
     * @param activity 活动对象
     */
    private void checkAndUpdateExpiryStatus(Activity activity) {
        // 检查活动是否过期
        boolean isExpired = activity.isActivityExpired();

        // 如果活动当前未标记为过期但实际已过期，则更新状态
        if (!activity.getIsExpired().equals(1) && isExpired) {
            // 异步更新数据库中的过期状态
            updateActivityExpiryStatusAsync(activity.getId());
            // 同时更新当前对象的状态
            activity.setIsExpired(1);
        }
    }

    /**
     * 异步更新活动过期状态
     *
     * @param activityId 活动ID
     */
    @Async("asyncExecutor")
    public void updateActivityExpiryStatusAsync(Long activityId) {
        try {
            Activity updateActivity = new Activity();
            updateActivity.setId(activityId);
            updateActivity.setIsExpired(1);

            this.update()
                    .eq("id", activityId)
                    .set("isExpired", 1)
                    .update(updateActivity);
        } catch (Exception e) {
            log.error("更新活动过期状态失败，activityId: {}", activityId, e);
        }
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

        // 替换URL为自定义域名
        activityPage.getRecords().forEach(Activity::replaceUrlWithCustomDomain);
        // 检查并更新过期状态
        activityPage.getRecords().forEach(this::checkAndUpdateExpiryStatus);

        return activityPage;
    }

    @Override
    public Page<Activity> listActivitiesBySpaceId(ActivityQueryRequest request, User loginUser) {
        long current = request.getCurrent();
        long size = request.getPageSize();

        // 验证用户是否是该空间的成员
        Long spaceId = request.getSpaceId();
        if (spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        }

        // 构建查询条件
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();

        // 搜索词
        String searchText = request.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("title", searchText).or().like("content", searchText);
        }

        // 只查询指定空间的活动
        queryWrapper.eq("spaceId", spaceId);

        // 普通用户只能查看审核通过且未过期的活动
        // 空间管理员和系统管理员可以查看该空间的所有活动（包括未审核、已过期等）
        boolean isSystemAdmin = userService.isAdmin(loginUser);
        if (isSystemAdmin) {
            Integer status = request.getStatus();
            if (status != null) {
                queryWrapper.eq("status", status);
            }

            // 是否只看未过期
            if (Boolean.TRUE.equals(request.getNotExpired())) {
                queryWrapper.eq("isExpired", 0);
            }
        } else {
            // 空间管理员和普通用户只能查看审核通过且未过期的活动
            queryWrapper.eq("status", 1); // 审核通过
            queryWrapper.eq("isExpired", 0); // 未过期
        }

        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");

        // 执行查询
        Page<Activity> activityPage = this.page(new Page<>(current, size), queryWrapper);

        // 填充活动信息
        activityPage.getRecords().forEach(this::fillActivityInfo);

        // 替换URL为自定义域名
        activityPage.getRecords().forEach(Activity::replaceUrlWithCustomDomain);
        // 检查并更新过期状态
        activityPage.getRecords().forEach(this::checkAndUpdateExpiryStatus);

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

        // 如果审核通过，发送通知给活动创建者
        if (status != null && status == 1) { // 审核通过
            sendActivityApprovedNotification(activity, loginUser);
        }
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

        // 替换URL为自定义域名
        activity.replaceUrlWithCustomDomain();

        // 检查并更新过期状态
        checkAndUpdateExpiryStatus(activity);

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
                .isNull("spaceId")  // 只查询公共空间活动
                .orderByDesc("createTime")
                // 只选择需要的字段
                .select("id", "title", "coverUrl", "expireTime", "isExpired", "viewCount");

        Page<Activity> activityPage = this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);

        // 获取实时浏览量
        activityPage.getRecords().forEach(activity -> {
            long realViewCount = getViewCount(activity.getId());
            activity.setViewCount(realViewCount);
        });

        // 替换URL为自定义域名
        activityPage.getRecords().forEach(Activity::replaceUrlWithCustomDomain);
        // 检查并更新过期状态
        activityPage.getRecords().forEach(this::checkAndUpdateExpiryStatus);

        return activityPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long id, User loginUser) {
        // 参数校验
        Activity activity = this.getById(id);
        ThrowUtils.throwIf(activity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 权限检查已在Controller层处理
        boolean isSystemAdmin = userService.isAdmin(loginUser);

        // 如果是系统管理员删除空间管理员的活动，发送通知
        if (isSystemAdmin && activity.getSpaceId() != null) {
            // 获取活动创建者信息
            User activityCreator = userService.getById(activity.getUserId());
            if (activityCreator != null && !activityCreator.getId().equals(loginUser.getId())) {
                // 发送删除通知给活动创建者
                sendActivityDeletedNotification(activity, activityCreator, loginUser);
            }
        }

        // 执行删除
        boolean success = this.removeById(id);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 发送活动删除通知给创建者
     */
    private void sendActivityDeletedNotification(Activity activity, User creator, User deleter) {
        // 创建系统通知
        SystemNotify notify = new SystemNotify();
        notify.setNotifyType("ACTIVITY_DELETED");
        notify.setTitle("活动删除通知");
        notify.setContent("您创建的活动《" + activity.getTitle() + "》已被管理员删除，删除原因：违规内容。");
        notify.setReceiverType("SPECIFIC_USER");
        notify.setReceiverId(String.valueOf(creator.getId()));
        notify.setIsGlobal(0);
        notify.setIsEnabled(1);
        notify.setReadStatus(0);
        notify.setOperatorId(String.valueOf(deleter.getId()));
        notify.setOperatorType("ADMIN");
        notify.setSenderId(String.valueOf(deleter.getId()));
        notify.setSenderType("ADMIN");

        systemNotifyService.addSystemNotify(notify);
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
