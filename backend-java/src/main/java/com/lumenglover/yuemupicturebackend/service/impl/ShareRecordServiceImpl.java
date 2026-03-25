package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import com.lumenglover.yuemupicturebackend.esdao.EsPictureDao;
import com.lumenglover.yuemupicturebackend.esdao.EsPostDao;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.websocket.MessageWebSocketHandler;
import com.lumenglover.yuemupicturebackend.mapper.ShareRecordMapper;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.ShareRecord;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.model.vo.ShareRecordVO;
import com.lumenglover.yuemupicturebackend.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareBatchRequest;
import com.lumenglover.yuemupicturebackend.utils.PicturePermissionUtils;
import org.apache.commons.lang3.StringUtils;

@Service
@Slf4j
public class ShareRecordServiceImpl extends ServiceImpl<ShareRecordMapper, ShareRecord>
        implements ShareRecordService {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;

    @Resource
    private PicturePermissionUtils picturePermissionUtils;


    @Resource
    @Lazy
    private MessageWebSocketHandler messageWebSocketHandler;

    @Override
    @Async("asyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Boolean> doShare(ShareRequest shareRequest, Long userId) {
        try {
            Long targetId = shareRequest.getTargetId();
            Integer targetType = shareRequest.getTargetType();
            Boolean isShared = shareRequest.getIsShared();

            // 参数校验
            if (targetId == null || targetType == null || isShared == null || userId == null) {
                log.error("Invalid parameters: targetId={}, targetType={}, isShared={}, userId={}",
                        targetId, targetType, isShared, userId);
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

            // 权限检查：如果目标是图片或帖子，需要验证用户是否有权限分享该内容
            if (targetType == 1) { // 图片类型
                Picture picture = pictureService.getById(targetId);
                if (picture == null) {
                    log.error("图片不存在: targetId={}", targetId);
                    return CompletableFuture.completedFuture(false);
                }

                // 获取用户对象
                User user = userService.getById(userId);

                // 检查用户是否有权限分享此图片
                boolean canShare = picturePermissionUtils.canSharePicture(picture, user);
                if (!canShare) {
                    log.info("用户 {} 没有权限分享图片 {}", userId, targetId);
                    return CompletableFuture.completedFuture(false);
                }
            } else if (targetType == 2) { // 帖子类型
                Post post = postService.getById(targetId);
                if (post == null) {
                    log.error("帖子不存在: targetId={}", targetId);
                    return CompletableFuture.completedFuture(false);
                }

                // 检查帖子分享权限
                boolean hasSharePermission = postService.checkPostPermission(post, "share");
                if (!hasSharePermission) {
                    log.info("用户 {} 没有权限分享帖子 {}", userId, targetId);
                    return CompletableFuture.completedFuture(false);
                }
            }

            // 查询当前分享状态
            QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                    .eq("targetId", targetId)
                    .eq("targetType", targetType);
            ShareRecord oldShareRecord = this.getOne(queryWrapper);

            if (oldShareRecord == null) {
                // 首次分享
                ShareRecord shareRecord = new ShareRecord();
                shareRecord.setUserId(userId);
                shareRecord.setTargetId(targetId);
                shareRecord.setTargetType(targetType);
                shareRecord.setTargetUserId(targetUserId); // 设置目标内容所属用户ID
                shareRecord.setShareTime(new Date());
                shareRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
                this.save(shareRecord);
                updateShareCount(targetId, targetType, 1);
            } else {
                // 更新分享状态
                if (oldShareRecord.getIsShared() != isShared) {
                    oldShareRecord.setIsShared(isShared);
                    oldShareRecord.setShareTime(new Date());
                    if (isShared) {
                        oldShareRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
                    }
                    this.updateById(oldShareRecord);
                    updateShareCount(targetId, targetType, isShared ? 1 : -1);
                }
            }

            // 通过WebSocket推送消息给目标用户
            if (isShared && targetUserId != null && !targetUserId.equals(userId)) {
                sendShareWebSocketNotification(targetUserId.toString());
            }

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Error in doShare: ", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 通过WebSocket推送消息给目标用户
     */
    private void sendShareWebSocketNotification(String targetUserId) {
        try {
            messageWebSocketHandler.sendUnreadCountToUser(targetUserId);
        } catch (Exception e) {
            log.error("通过WebSocket推送分享通知给用户 {} 失败", targetUserId, e);
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

    /**
     * 更新分享数
     */
    private void updateShareCount(Long targetId, Integer targetType, int delta) {
        switch (targetType) {
            case 1: // 图片
                pictureService.update()
                        .setSql("shareCount = shareCount + " + delta)
                        .eq("id", targetId)
                        .ge("shareCount", -delta)
                        .update();
                break;
            case 2: // 帖子
                postService.update()
                        .setSql("shareCount = shareCount + " + delta)
                        .eq("id", targetId)
                        .ge("shareCount", -delta)
                        .update();
                break;
            default:
                log.error("Unsupported target type: {}", targetType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShareRecordVO> getAndClearUnreadShares(Long userId) {
        // 1. 获取未读分享记录
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isRead", 0)
                .ne("userId", userId)  // 排除自己分享自己的记录
                .orderByDesc("shareTime")
                .last("LIMIT 50");  // 限制最多返回50条数据

        List<ShareRecord> unreadShares = this.list(queryWrapper);
        if (CollUtil.isEmpty(unreadShares)) {
            return new ArrayList<>();
        }

        // 注意：这里不再自动更新为已读状态
        // 该操作应该由用户明确操作来完成

        return convertToVOList(unreadShares);
    }


    @Override
    public Page<ShareRecordVO> getUserShareHistory(ShareQueryRequest shareQueryRequest, Long userId) {
        long current = shareQueryRequest.getCurrent();
        long size = shareQueryRequest.getPageSize();

        // 创建分页对象
        Page<ShareRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .ne("userId", userId);  // 排除自己分享自己的记录

        // 处理目标类型查询
        Integer targetType = shareQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("shareTime");

        // 执行分页查询
        Page<ShareRecord> sharePage = this.page(page, queryWrapper);

        // 转换结果
        List<ShareRecordVO> records = convertToVOList(sharePage.getRecords());

        // 构建返回结果
        Page<ShareRecordVO> voPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    public boolean isContentShared(Long targetId, Integer targetType, Long userId) {
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("userId", userId)
                .eq("isShared", true);
        return this.count(queryWrapper) > 0;
    }

    private List<ShareRecordVO> convertToVOList(List<ShareRecord> shareRecords) {
        if (CollUtil.isEmpty(shareRecords)) {
            return new ArrayList<>();
        }

        return shareRecords.stream().map(share -> {
            ShareRecordVO vo = new ShareRecordVO();
            BeanUtils.copyProperties(share, vo);

            // 设置分享用户信息
            User user = userService.getById(share.getUserId());
            vo.setUser(userService.getUserVO(user));

            // 根据类型获取目标内容
            switch (share.getTargetType()) {
                case 1: // 图片
                    Picture picture = pictureService.getById(share.getTargetId());
                    if (picture != null) {
                        PictureVO pictureVO = PictureVO.objToVo(picture);
                        // 设置用户信息
                        User pictureUser = userService.getById(picture.getUserId());
                        pictureVO.setUser(userService.getUserVO(pictureUser));
                        vo.setTarget(pictureVO);
                    }
                    break;
                case 2: // 帖子
                    Post post = postService.getById(share.getTargetId());
                    if (post != null) {
                        // 设置用户信息
                        User postUser = userService.getById(post.getUserId());
                        post.setUser(userService.getUserVO(postUser));
                        vo.setTarget(post);
                    }
                    break;
                default:
                    log.error("Unsupported target type: {}", share.getTargetType());
                    break;
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long getUnreadSharesCount(Long userId) {
        return this.count(new QueryWrapper<ShareRecord>()
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isShared", true)  // 只统计仍处于分享状态的记录
                .ne("userId", userId));  // 排除自己分享自己的记录
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadShares(Long userId) {
        this.update(new UpdateWrapper<ShareRecord>()
                .set("isRead", 1)
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isShared", true));  // 只标记仍处于分享状态的记录
    }

    @Override
    public Page<ShareRecordVO> getMyShareHistory(ShareQueryRequest shareQueryRequest, Long userId) {
        long current = shareQueryRequest.getCurrent();
        long size = shareQueryRequest.getPageSize();

        // 创建分页对象
        Page<ShareRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)  // 查询用户自己的分享记录
                .eq("isShared", true);  // 只查询分享状态为true的记录

        // 处理目标类型查询
        Integer targetType = shareQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("shareTime");

        // 执行分页查询
        Page<ShareRecord> sharePage = this.page(page, queryWrapper);

        // 转换结果
        List<ShareRecordVO> records = convertToVOList(sharePage.getRecords());

        // 构建返回结果
        Page<ShareRecordVO> voPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareContent(Long targetId, Integer targetType, Long userId) {
        // 获取目标内容所属用户ID
        Long targetUserId = getTargetUserId(targetId, targetType);
        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标内容不存在");
        }

        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setUserId(userId);
        shareRecord.setTargetId(targetId);
        shareRecord.setTargetType(targetType);
        shareRecord.setTargetUserId(targetUserId);
        shareRecord.setShareTime(new Date());
        shareRecord.setIsRead(userId.equals(targetUserId) ? 1 : 0);
        shareRecord.setIsShared(true);
        this.save(shareRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unshareContent(Long targetId, Integer targetType, Long userId) {
        this.update(new UpdateWrapper<ShareRecord>()
                .set("isShared", false)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("userId", userId));
    }

    @Override
    public QueryWrapper<ShareRecord> getAdminQueryWrapper(ShareAdminRequest shareAdminRequest) {
        if (shareAdminRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = shareAdminRequest.getId();
        Long userId = shareAdminRequest.getUserId();
        Long targetId = shareAdminRequest.getTargetId();
        Integer targetType = shareAdminRequest.getTargetType();
        Long targetUserId = shareAdminRequest.getTargetUserId();
        Boolean isShared = shareAdminRequest.getIsShared();
        Integer isRead = shareAdminRequest.getIsRead();
        String sortField = shareAdminRequest.getSortField();
        String sortOrder = shareAdminRequest.getSortOrder();

        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();

        // 拼接查询条件
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(targetId != null, "targetId", targetId);
        queryWrapper.eq(targetType != null, "targetType", targetType);
        queryWrapper.eq(targetUserId != null, "targetUserId", targetUserId);
        queryWrapper.eq(isShared != null, "isShared", isShared);
        queryWrapper.eq(isRead != null, "isRead", isRead);

        // 排序
        if (StringUtils.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        } else {
            queryWrapper.orderByDesc("shareTime");
        }

        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationShares(ShareBatchRequest shareBatchRequest) {
        List<Long> ids = shareBatchRequest.getIds();
        String operation = shareBatchRequest.getOperation();

        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分享记录id列表为空");
        }

        // 查询所有分享记录
        List<ShareRecord> shareRecords = this.listByIds(ids);
        if (shareRecords.size() != ids.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "部分分享记录不存在");
        }

        boolean result = false;
        switch (operation) {
            case "delete":
                // 删除分享记录
                result = this.update()
                        .set("isShared", false)
                        .in("id", ids)
                        .update();

                // 更新目标内容的分享数
                for (ShareRecord share : shareRecords) {
                    if (share.getIsShared()) {
                        updateShareCount(share.getTargetId(), share.getTargetType(), -1);
                    }
                }
                break;
            case "restore":
                // 恢复分享记录
                result = this.update()
                        .set("isShared", true)
                        .in("id", ids)
                        .update();

                // 更新目标内容的分享数
                for (ShareRecord share : shareRecords) {
                    if (!share.getIsShared()) {
                        updateShareCount(share.getTargetId(), share.getTargetType(), 1);
                    }
                }
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的操作");
        }

        return result;
    }

    @Override
    public Page<ShareRecordVO> getAllSharesByUserId(Long userId, long current, long pageSize) {
        // 构建查询条件：用户分享的记录或者被分享的记录
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq("targetUserId", userId));
        queryWrapper.eq("isShared", true);  // 只查询仍处于分享状态的记录
        queryWrapper.orderByDesc("shareTime");

        // 分页查询
        Page<ShareRecord> sharePage = this.page(new Page<>(current, pageSize), queryWrapper);

        // 转换为VO对象
        List<ShareRecordVO> shareRecordVOList = convertToVOList(sharePage.getRecords());

        // 构建返回的Page对象
        Page<ShareRecordVO> shareRecordVOPage = new Page<>(current, pageSize, sharePage.getTotal());
        shareRecordVOPage.setRecords(shareRecordVOList);

        return shareRecordVOPage;
    }

    @Override
    public boolean markShareAsRead(Long id) {
        // 更新分享记录为已读状态
        return this.update(new UpdateWrapper<ShareRecord>()
                .eq("id", id)
                .eq("isRead", 0)  // 只有未读的记录才需要更新
                .set("isRead", 1));
    }
}
