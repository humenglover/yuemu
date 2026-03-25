package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.websocket.MessageWebSocketHandler;
import com.lumenglover.yuemupicturebackend.mapper.LikeRecordMapper;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.LikeRecord;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.LikeRecordVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.SqlUtils;
import com.lumenglover.yuemupicturebackend.utils.PicturePermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeBatchRequest;
import org.apache.commons.lang3.StringUtils;

@Service
@Slf4j
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord>
        implements LikeRecordService {


    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PictureService pictureService;

    @Resource
    private PicturePermissionUtils picturePermissionUtils;

    @Lazy
    @Resource
    private PostService postService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Lazy
    private MessageWebSocketHandler messageWebSocketHandler;

    @Override
    @Async("asyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Boolean> doLike(LikeRequest likeRequest, Long userId) {
        try {
            Long targetId = likeRequest.getTargetId();
            Integer targetType = likeRequest.getTargetType();
            Boolean isLiked = likeRequest.getIsLiked();

            // 参数校验
            if (targetId == null || targetType == null || isLiked == null || userId == null) {
                log.error("Invalid parameters: targetId={}, targetType={}, isLiked={}, userId={}",
                        targetId, targetType, isLiked, userId);
                return CompletableFuture.completedFuture(false);
            }

            // 校验目标类型
            if (targetType != 1 && targetType != 2) { // 只允许图片(1)和帖子(2)
                log.error("Invalid target type: {}", targetType);
                return CompletableFuture.completedFuture(false);
            }

            // 获取目标内容所属用户ID
            Long targetUserId = getTargetUserId(targetId, targetType);
            if (targetUserId == null) {
                log.error("Target content not found: targetId={}, targetType={}", targetId, targetType);
                return CompletableFuture.completedFuture(false);
            }

            // 权限检查：如果目标是图片或帖子，需要验证用户是否有权限点赞该内容
            if (targetType == 1) { // 图片类型
                Picture picture = pictureService.getById(targetId);
                if (picture == null) {
                    log.error("图片不存在: targetId={}", targetId);
                    return CompletableFuture.completedFuture(false);
                }

                // 获取用户对象
                User user = userService.getById(userId);

                // 检查用户是否有权限点赞此图片
                boolean canLike = picturePermissionUtils.canLikePicture(picture, user);
                if (!canLike) {
                    log.info("用户 {} 没有权限点赞图片 {}", userId, targetId);
                    return CompletableFuture.completedFuture(false);
                }
            } else if (targetType == 2) { // 帖子类型
                Post post = postService.getById(targetId);
                if (post == null) {
                    log.error("帖子不存在: targetId={}", targetId);
                    return CompletableFuture.completedFuture(false);
                }

                // 检查帖子点赞权限
                boolean hasLikePermission = postService.checkPostPermission(post, "like");
                if (!hasLikePermission) {
                    log.info("用户 {} 没有权限点赞帖子 {}", userId, targetId);
                    return CompletableFuture.completedFuture(false);
                }
            }

            // 查询当前点赞状态
            QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                    .eq("targetId", targetId)
                    .eq("targetType", targetType);
            LikeRecord oldLikeRecord = this.getOne(queryWrapper);

            // 处理点赞记录
            if (oldLikeRecord == null) {
                if (isLiked) {
                    // 首次点赞
                    LikeRecord likeRecord = new LikeRecord();
                    likeRecord.setUserId(userId);
                    likeRecord.setTargetId(targetId);
                    likeRecord.setTargetType(targetType);
                    likeRecord.setTargetUserId(targetUserId); // 设置目标内容所属用户ID
                    likeRecord.setIsLiked(true);
                    likeRecord.setFirstLikeTime(new Date());
                    likeRecord.setLastLikeTime(new Date());
                    likeRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
                    this.save(likeRecord);
                    updateLikeCount(targetId, targetType, 1);
                }
            } else {
                if (isLiked != oldLikeRecord.getIsLiked()) {
                    // 更新点赞状态
                    oldLikeRecord.setIsLiked(isLiked);
                    oldLikeRecord.setLastLikeTime(new Date());
                    oldLikeRecord.setTargetUserId(targetUserId); // 更新目标内容所属用户ID
                    // 如果是重新点赞，设置为未读
                    if (isLiked) {
                        oldLikeRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
                    }
                    this.updateById(oldLikeRecord);
                    updateLikeCount(targetId, targetType, isLiked ? 1 : -1);
                }
            }

            // 通过WebSocket推送消息给目标用户
            if (isLiked && targetUserId != null && !targetUserId.equals(userId)) {
                sendLikeWebSocketNotification(targetUserId.toString());
            }

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Error in doLike: ", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 通过WebSocket推送消息给目标用户
     */
    private void sendLikeWebSocketNotification(String targetUserId) {
        try {
            messageWebSocketHandler.sendUnreadCountToUser(targetUserId);
        } catch (Exception e) {
            log.error("通过WebSocket推送点赞通知给用户 {} 失败", targetUserId, e);
        }
    }

    /**
     * 获取目标内容所属用户ID
     */
    private Long getTargetUserId(Long targetId, Integer targetType) {
        try {
            switch (targetType) {
                case 1: // 图片
                    Picture picture = pictureService.getById(targetId);
                    return picture != null ? picture.getUserId() : null;
                case 2: // 帖子
                    Post post = postService.getById(targetId);
                    return post != null ? post.getUserId() : null;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error getting target user id: ", e);
            return null;
        }
    }

    @Override
    @Transactional
    public List<LikeRecordVO> getAndClearUnreadLikes(Long userId) {
        // 1. 获取未读点赞记录
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isLiked", true)
                .ne("userId", userId)
                .orderByDesc("lastLikeTime")
                .last("LIMIT 50");  // 限制最多返回50条数据

        List<LikeRecord> unreadLikes = this.list(queryWrapper);
        if (CollUtil.isEmpty(unreadLikes)) {
            return new ArrayList<>();
        }

        // 注意：这里不再自动更新为已读状态
        // 该操作应该由用户明确操作来完成

        return convertToVOList(unreadLikes);
    }

    @Override
    public Page<LikeRecordVO> getUserLikeHistory(LikeQueryRequest likeQueryRequest, Long userId) {
        long current = likeQueryRequest.getCurrent();
        long size = likeQueryRequest.getPageSize();

        // 创建分页对象
        Page<LikeRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)  // 查询被点赞的记录
                .ne("userId", userId);  // 排除自己点赞自己的记录;

        // 处理目标类型查询
        Integer targetType = likeQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("lastLikeTime");

        // 执行分页查询
        Page<LikeRecord> likePage = this.page(page, queryWrapper);

        // 转换结果
        List<LikeRecordVO> records = convertToVOList(likePage.getRecords());

        // 构建返回结果
        Page<LikeRecordVO> voPage = new Page<>(likePage.getCurrent(), likePage.getSize(), likePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    public boolean isContentLiked(Long targetId, Integer targetType, Long userId) {
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("userId", userId)
                .eq("isLiked", true);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 更新点赞数
     */
    private void updateLikeCount(Long targetId, Integer targetType, int delta) {
        switch (targetType) {
            case 1: // 图片
                pictureService.update()
                        .setSql("likeCount = likeCount + " + delta)
                        .eq("id", targetId)
                        .ge("likeCount", -delta)
                        .update();
                break;
            case 2: // 帖子
                postService.update()
                        .setSql("likeCount = likeCount + " + delta)
                        .eq("id", targetId)
                        .ge("likeCount", -delta)
                        .update();
                break;
            default:
                log.error("Unsupported target type: {}", targetType);
        }
    }

    private List<LikeRecordVO> convertToVOList(List<LikeRecord> likeRecords) {
        if (CollUtil.isEmpty(likeRecords)) {
            return new ArrayList<>();
        }

        return likeRecords.stream().map(like -> {
            LikeRecordVO vo = new LikeRecordVO();
            BeanUtils.copyProperties(like, vo);

            // 设置点赞用户信息
            User likeUser = userService.getById(like.getUserId());
            if (likeUser != null) {
                vo.setUser(userService.getUserVO(likeUser));
            }

            // 根据类型获取目标内容
            switch (like.getTargetType()) {
                case 1: // 图片
                    Picture picture = pictureService.getById(like.getTargetId());
                    if (picture != null) {
                        PictureVO pictureVO = PictureVO.objToVo(picture);
                        // 设置图片作者信息
                        User pictureUser = userService.getById(picture.getUserId());
                        if (pictureUser != null) {
                            pictureVO.setUser(userService.getUserVO(pictureUser));
                        }
                        vo.setTarget(pictureVO);
                    }
                    break;
                case 2: // 帖子
                    Post post = postService.getById(like.getTargetId());
                    if (post != null) {
                        // 设置帖子作者信息
                        User postUser = userService.getById(post.getUserId());
                        if (postUser != null) {
                            post.setUser(userService.getUserVO(postUser));
                        }
                        vo.setTarget(post);
                    }
                    break;
                default:
                    log.error("Unsupported target type: {}", like.getTargetType());
                    break;
            }
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    public long getUnreadLikesCount(Long userId) {
        return this.count(new QueryWrapper<LikeRecord>()
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isLiked", true)
                .ne("userId", userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadLikes(Long userId) {
        this.update(new UpdateWrapper<LikeRecord>()
                .set("isRead", 1)
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isLiked", true));
    }

    @Override
    public Page<LikeRecordVO> getMyLikeHistory(LikeQueryRequest likeQueryRequest, Long userId) {
        long current = likeQueryRequest.getCurrent();
        long size = likeQueryRequest.getPageSize();

        // 创建分页对象
        Page<LikeRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)  // 查询用户自己的点赞记录
                .eq("isLiked", true);  // 只查询点赞状态为true的记录

        // 处理目标类型查询
        Integer targetType = likeQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("lastLikeTime");

        // 执行分页查询
        Page<LikeRecord> likePage = this.page(page, queryWrapper);

        // 转换结果
        List<LikeRecordVO> records = convertToVOList(likePage.getRecords());

        // 构建返回结果
        Page<LikeRecordVO> voPage = new Page<>(likePage.getCurrent(), likePage.getSize(), likePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeContent(Long targetId, Integer targetType, Long userId) {
        // 获取目标内容所属用户ID
        Long targetUserId = getTargetUserId(targetId, targetType);
        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标内容不存在");
        }

        // 保存点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetId(targetId);
        likeRecord.setTargetType(targetType);
        likeRecord.setTargetUserId(targetUserId);
        likeRecord.setLastLikeTime(new Date());
        likeRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
        likeRecord.setIsLiked(true);
        this.save(likeRecord);

        // 更新点赞计数
        if (targetType == 1) { // 图片
            Picture picture = pictureService.getById(targetId);
            if (picture != null && (picture.getSpaceId() == null || picture.getSpaceId() == 0)) {
                // 只对公共图库的图片使用缓存
                incrementPictureLikeCount(targetId);
            } else {
                // 非公共图库直接更新数据库
                pictureService.update()
                        .setSql("likeCount = likeCount + 1")
                        .eq("id", targetId)
                        .update();
            }
        } else if (targetType == 2) {
            postService.update()
                    .setSql("likeCount = likeCount + 1")
                    .eq("id", targetId)
                    .update();
        }
    }

    /**
     * 增加图片点赞计数
     */
    private void incrementPictureLikeCount(Long pictureId) {
        String key = String.format("picture:like:%d", pictureId);
        String lockKey = String.format("picture:like:lock:%d", pictureId);

        try {
            // 获取分布式锁
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 增加点赞计数
                    Long count = stringRedisTemplate.opsForValue().increment(key);

                    // 当点赞数达到阈值时，更新数据库
                    if (count != null && count % 100 == 0) {
                        pictureService.update()
                                .setSql("likeCount = likeCount + " + count)
                                .eq("id", pictureId)
                                .update();
                        // 重置Redis计数
                        stringRedisTemplate.delete(key);
                    }
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            }
        } catch (Exception e) {
            log.error("增加图片点赞计数失败", e);
        }
    }

    /**
     * 获取图片的实时点赞数
     */
    public long getRealTimeLikeCount(Long pictureId) {
        String key = String.format("picture:like:%d", pictureId);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        return countStr != null ? Long.parseLong(countStr) : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeContent(Long targetId, Integer targetType, Long userId) {
        this.update(new UpdateWrapper<LikeRecord>()
                .set("isLiked", false)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("userId", userId));

        // 更新数据库中的点赞数
        if (targetType == 1) { // 图片
            pictureService.update()
                    .setSql("likeCount = likeCount - 1")
                    .eq("id", targetId)
                    .update();
        } else if (targetType == 2) { // 帖子
            postService.update()
                    .setSql("likeCount = likeCount - 1")
                    .eq("id", targetId)
                    .update();
        }
    }

    @Override
    public QueryWrapper<LikeRecord> getAdminQueryWrapper(LikeAdminRequest likeAdminRequest) {
        if (likeAdminRequest == null) {
            return null;
        }
        Long id = likeAdminRequest.getId();
        Long userId = likeAdminRequest.getUserId();
        Long targetId = likeAdminRequest.getTargetId();
        Integer targetType = likeAdminRequest.getTargetType();
        Long targetUserId = likeAdminRequest.getTargetUserId();
        Boolean isLiked = likeAdminRequest.getIsLiked();
        Integer isRead = likeAdminRequest.getIsRead();
        String sortField = likeAdminRequest.getSortField();
        String sortOrder = likeAdminRequest.getSortOrder();

        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(targetId != null, "targetId", targetId);
        queryWrapper.eq(targetType != null, "targetType", targetType);
        queryWrapper.eq(targetUserId != null, "targetUserId", targetUserId);
        queryWrapper.eq(isLiked != null, "isLiked", isLiked);
        queryWrapper.eq(isRead != null, "isRead", isRead);
        // 排序字段
        if (StringUtils.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        } else {
            queryWrapper.orderBy(true, true, "firstLikeTime");
        }
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationLikes(LikeBatchRequest likeBatchRequest) {
        List<Long> ids = likeBatchRequest.getIds();
        String operation = likeBatchRequest.getOperation();

        if (operation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 根据操作类型执行相应的批量操作
        boolean result = false;
        switch (operation) {
            case "delete":
                result = this.update()
                        .set("isDelete", 1)
                        .in("id", ids)
                        .update();
                break;
            case "recover":
                result = this.update()
                        .set("isDelete", 0)
                        .in("id", ids)
                        .update();
                break;
            case "physical":
                result = this.removeBatchByIds(ids);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return result;
    }

    @Override
    public Page<LikeRecordVO> getAllLikesByUserId(Long userId, long current, long pageSize) {
        // 构建查询条件：用户点赞的记录或者被点赞的记录
        QueryWrapper<LikeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq("targetUserId", userId));
        queryWrapper.eq("isLiked", true);  // 只查询仍处于点赞状态的记录
        queryWrapper.orderByDesc("lastLikeTime");

        // 分页查询
        Page<LikeRecord> likePage = this.page(new Page<>(current, pageSize), queryWrapper);

        // 转换为VO对象
        List<LikeRecordVO> likeRecordVOList = convertToVOList(likePage.getRecords());

        // 构建返回的Page对象
        Page<LikeRecordVO> likeRecordVOPage = new Page<>(current, pageSize, likePage.getTotal());
        likeRecordVOPage.setRecords(likeRecordVOList);

        return likeRecordVOPage;
    }

    @Override
    public boolean markLikeAsRead(Long id) {
        // 更新点赞记录为已读状态
        return this.update(new UpdateWrapper<LikeRecord>()
                .eq("id", id)
                .eq("isRead", 0)  // 只有未读的记录才需要更新
                .eq("isLiked", true)  // 只标记仍处于点赞状态的记录
                .set("isRead", 1));
    }
}
