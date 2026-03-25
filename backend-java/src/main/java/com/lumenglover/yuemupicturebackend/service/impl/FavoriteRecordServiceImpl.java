package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.FavoriteRecordMapper;
import com.lumenglover.yuemupicturebackend.model.dto.favoriterecord.FavoriteRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.favoriterecord.FavoriteRecordQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.FavoriteRecord;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.FavoriteRecordVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.SqlUtils;
import com.lumenglover.yuemupicturebackend.utils.PicturePermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FavoriteRecordServiceImpl extends ServiceImpl<FavoriteRecordMapper, FavoriteRecord>
        implements FavoriteRecordService {

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PictureService pictureService;

    @Lazy
    @Resource
    private PostService postService;

    @Lazy
    @Resource
    private SpaceService spaceService;

    @Resource
    private PicturePermissionUtils picturePermissionUtils;

    @Override
    public long addFavoriteRecord(FavoriteRecordAddRequest favoriteRecordAddRequest, HttpServletRequest request) {
        if (favoriteRecordAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = favoriteRecordAddRequest.getUserId();
        Long targetId = favoriteRecordAddRequest.getTargetId();
        Integer targetType = favoriteRecordAddRequest.getTargetType();

        if (userId == null || targetId == null || targetType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 权限检查：如果目标是图片或帖子，需要验证用户是否有权限收藏该内容
        if (targetType == 1) { // 图片类型
            Picture picture = pictureService.getById(targetId);
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }

            // 获取用户对象
            User user = userService.getById(userId);

            // 检查用户是否有权限收藏此图片
            boolean canCollect = picturePermissionUtils.canCollectPicture(picture, user);
            if (!canCollect) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限收藏此图片");
            }
        } else if (targetType == 2) { // 帖子类型
            Post post = postService.getById(targetId);
            if (post == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
            }

            // 检查帖子收藏权限
            boolean hasCollectPermission = postService.checkPostPermission(post, "collect");
            if (!hasCollectPermission) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该帖子不允许收藏");
            }
        }

        // 检查是否已存在相同的收藏记录
        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("isDelete", 0); // 未删除的记录

        FavoriteRecord existingRecord = this.getOne(queryWrapper, false);
        boolean wasFavorite = false;
        if (existingRecord != null) {
            wasFavorite = existingRecord.getIsFavorite();
            // 更新收藏状态
            existingRecord.setIsFavorite(favoriteRecordAddRequest.getIsFavorite() != null ? favoriteRecordAddRequest.getIsFavorite() : true);
            existingRecord.setFavoriteTime(new Date());
            existingRecord.setIsRead(false); // 重置为未读状态
            this.updateById(existingRecord);

            // 如果状态改变，更新收藏量
            if (wasFavorite != existingRecord.getIsFavorite()) {
                updateFavoriteCount(targetId, targetType, existingRecord.getIsFavorite() ? 1 : -1);
            }

            return existingRecord.getId();
        } else {
            // 创建新的收藏记录
            FavoriteRecord favoriteRecord = new FavoriteRecord();
            favoriteRecord.setUserId(userId);
            favoriteRecord.setTargetId(targetId);
            favoriteRecord.setTargetType(targetType);
            favoriteRecord.setTargetUserId(favoriteRecordAddRequest.getTargetUserId());
            favoriteRecord.setIsFavorite(favoriteRecordAddRequest.getIsFavorite() != null ? favoriteRecordAddRequest.getIsFavorite() : true);
            favoriteRecord.setFavoriteTime(new Date());
            favoriteRecord.setIsRead(false); // 新收藏默认未读

            this.save(favoriteRecord);

            // 如果是收藏操作，更新收藏量
            if (favoriteRecord.getIsFavorite()) {
                updateFavoriteCount(targetId, targetType, 1);
            }

            return favoriteRecord.getId();
        }
    }

    @Override
    public boolean hasFavorited(Long userId, Long targetId, Integer targetType) {
        if (userId == null || targetId == null || targetType == null) {
            return false;
        }

        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("isFavorite", true) // 确保是收藏状态
                .eq("isDelete", 0); // 未删除的记录

        long count = this.count(queryWrapper);
        return count > 0;
    }

    @Override
    public boolean cancelFavorite(Long userId, Long targetId, Integer targetType) {
        if (userId == null || targetId == null || targetType == null) {
            return false;
        }

        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("isFavorite", true) // 确保是收藏状态
                .eq("isDelete", 0); // 未删除的记录

        List<FavoriteRecord> records = this.list(queryWrapper);
        if (!CollUtil.isEmpty(records)) {
            for (FavoriteRecord record : records) {
                record.setIsFavorite(Boolean.FALSE);
                record.setFavoriteTime(new Date());
                this.updateById(record);
                // 取消收藏时减少收藏量
                updateFavoriteCount(targetId, targetType, -1);
            }
            return true;
        }
        return false;
    }

    @Override
    public Page<FavoriteRecordVO> getUserFavoriteHistory(FavoriteRecordQueryRequest favoriteRecordQueryRequest, Long userId) {
        long current = favoriteRecordQueryRequest.getCurrent();
        long size = favoriteRecordQueryRequest.getPageSize();

        // 创建分页对象
        Page<FavoriteRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)  // 查询被收藏的记录
                .ne("userId", userId);  // 排除自己收藏自己的记录

        // 处理目标类型查询
        Integer targetType = favoriteRecordQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("favoriteTime");

        // 执行分页查询
        Page<FavoriteRecord> favoritePage = this.page(page, queryWrapper);

        // 转换结果
        List<FavoriteRecordVO> records = convertToVOList(favoritePage.getRecords());

        // 构建返回结果
        Page<FavoriteRecordVO> voPage = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    public Page<FavoriteRecordVO> getMyFavoriteHistory(FavoriteRecordQueryRequest favoriteRecordQueryRequest, Long userId) {
        long current = favoriteRecordQueryRequest.getCurrent();
        long size = favoriteRecordQueryRequest.getPageSize();

        // 创建分页对象
        Page<FavoriteRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)  // 查询用户自己的收藏记录
                .eq("isFavorite", true);  // 只查询收藏状态为true的记录

        // 处理目标类型查询
        Integer targetType = favoriteRecordQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("favoriteTime");

        // 执行分页查询
        Page<FavoriteRecord> favoritePage = this.page(page, queryWrapper);

        // 转换结果
        List<FavoriteRecordVO> records = convertToVOList(favoritePage.getRecords());

        // 构建返回结果
        Page<FavoriteRecordVO> voPage = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    public List<FavoriteRecordVO> getAndClearUnreadFavorites(Long userId) {
        // 1. 获取未读收藏记录
        QueryWrapper<FavoriteRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isFavorite", true)
                .ne("userId", userId)
                .orderByDesc("favoriteTime")
                .last("LIMIT 50");  // 限制最多返回50条数据

        List<FavoriteRecord> unreadFavorites = this.list(queryWrapper);
        if (CollUtil.isEmpty(unreadFavorites)) {
            return new ArrayList<>();
        }

        // 注意：这里不再自动更新为已读状态
        // 该操作应该由用户明确操作来完成

        return convertToVOList(unreadFavorites);
    }

    @Override
    public long getUnreadFavoritesCount(Long userId) {
        return this.count(new QueryWrapper<FavoriteRecord>()
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isFavorite", true)
                .ne("userId", userId));
    }

    /**
     * 更新收藏数
     */
    private void updateFavoriteCount(Long targetId, Integer targetType, int delta) {
        switch (targetType) {
            case 1: // 图片
                pictureService.update()
                        .setSql("favoriteCount = favoriteCount + " + delta)
                        .eq("id", targetId)
                        .ge("favoriteCount", -delta)
                        .update();
                break;
            case 2: // 帖子
                postService.update()
                        .setSql("favoriteCount = favoriteCount + " + delta)
                        .eq("id", targetId)
                        .ge("favoriteCount", -delta)
                        .update();
                break;
            default:
                log.error("Unsupported target type: {}", targetType);
        }
    }

    private List<FavoriteRecordVO> convertToVOList(List<FavoriteRecord> favoriteRecords) {
        if (CollUtil.isEmpty(favoriteRecords)) {
            return new ArrayList<>();
        }

        return favoriteRecords.stream().map(favorite -> {
            FavoriteRecordVO vo = new FavoriteRecordVO();
            vo.setId(favorite.getId());
            vo.setLastFavoriteTime(favorite.getFavoriteTime());
            vo.setTargetType(favorite.getTargetType());
            vo.setTargetUserId(favorite.getTargetUserId());
            vo.setIsRead(favorite.getIsRead() ? 1 : 0);

            // 设置收藏用户信息
            User favoriteUser = userService.getById(favorite.getUserId());
            if (favoriteUser != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(favoriteUser, userVO);
                vo.setUser(userVO);
            }

            // 根据类型获取目标内容
            switch (favorite.getTargetType()) {
                case 1: // 图片
                    Picture picture = pictureService.getById(favorite.getTargetId());
                    if (picture != null) {
                        vo.setTarget(picture);
                    }
                    break;
                case 2: // 帖子
                    Post post = postService.getById(favorite.getTargetId());
                    if (post != null) {
                        vo.setTarget(post);
                    }
                    break;
                case 3: // 空间
                    Object space = spaceService.getById(favorite.getTargetId());
                    if (space != null) {
                        vo.setTarget(space);
                    }
                    break;
                default:
                    log.error("Unsupported target type: {}", favorite.getTargetType());
                    break;
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
