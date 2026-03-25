package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeRequest;
import com.lumenglover.yuemupicturebackend.model.entity.LikeRecord;
import com.lumenglover.yuemupicturebackend.model.vo.LikeRecordVO;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeBatchRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LikeRecordService extends IService<LikeRecord> {
    /**
     * 通用点赞/取消点赞
     */
    CompletableFuture<Boolean> doLike(LikeRequest likeRequest, Long userId);

    /**
     * 获取并清除用户未读的点赞消息
     */
    List<LikeRecordVO> getAndClearUnreadLikes(Long userId);

    /**
     * 获取用户的点赞历史（分页）
     */
    Page<LikeRecordVO> getUserLikeHistory(LikeQueryRequest likeQueryRequest, Long userId);

    /**
     * 检查内容是否已被用户点赞
     */
    boolean isContentLiked(Long targetId, Integer targetType, Long userId);

    /**
     * 获取用户未读点赞数
     */
    long getUnreadLikesCount(Long userId);

    /**
     * 清除用户所有未读点赞状态
     */
    void clearAllUnreadLikes(Long userId);

    /**
     * 获取用户自己的点赞历史（分页）
     */
    Page<LikeRecordVO> getMyLikeHistory(LikeQueryRequest likeQueryRequest, Long userId);

    /**
     * 点赞内容
     */
    void likeContent(Long targetId, Integer targetType, Long userId);

    /**
     * 取消点赞内容
     */
    void unlikeContent(Long targetId, Integer targetType, Long userId);

    long getRealTimeLikeCount(Long pictureId);

    /**
     * 获取管理员查询条件构造器
     */
    QueryWrapper<LikeRecord> getAdminQueryWrapper(LikeAdminRequest likeAdminRequest);

    /**
     * 批量操作点赞记录
     */
    boolean batchOperationLikes(LikeBatchRequest likeBatchRequest);

    /**
     * 获取用户所有的点赞记录（包括已读和未读）
     * @param userId 用户ID
     * @param current 当前页
     * @param pageSize 页面大小
     * @return 分页的点赞记录列表
     */
    Page<LikeRecordVO> getAllLikesByUserId(Long userId, long current, long pageSize);

    /**
     * 标记单个点赞记录为已读
     * @param id 点赞记录ID
     * @return 操作结果
     */
    boolean markLikeAsRead(Long id);
}
