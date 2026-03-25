package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareRequest;
import com.lumenglover.yuemupicturebackend.model.entity.ShareRecord;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.ShareRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareBatchRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ShareRecordService extends IService<ShareRecord> {
    /**
     * 通用分享/取消分享
     */
    CompletableFuture<Boolean> doShare(ShareRequest shareRequest, Long userId);

    /**
     * 获取并清除用户未读的分享消息
     */
    List<ShareRecordVO> getAndClearUnreadShares(Long userId);

    /**
     * 获取用户的分享历史
     */
    Page<ShareRecordVO> getUserShareHistory(ShareQueryRequest shareQueryRequest, Long userId);

    /**
     * 检查内容是否已被用户分享
     */
    boolean isContentShared(Long targetId, Integer targetType, Long userId);

    /**
     * 获取用户未读分享数
     */
    long getUnreadSharesCount(Long userId);

    /**
     * 清除用户所有未读分享状态
     */
    void clearAllUnreadShares(Long userId);

    /**
     * 获取用户自己的分享历史（分页）
     */
    Page<ShareRecordVO> getMyShareHistory(ShareQueryRequest shareQueryRequest, Long userId);

    /**
     * 分享内容
     */
    void shareContent(Long targetId, Integer targetType, Long userId);

    /**
     * 取消分享内容
     */
    void unshareContent(Long targetId, Integer targetType, Long userId);

    /**
     * 获取管理员查询条件
     */
    QueryWrapper<ShareRecord> getAdminQueryWrapper(ShareAdminRequest shareAdminRequest);

    /**
     * 批量操作分享记录
     */
    boolean batchOperationShares(ShareBatchRequest shareBatchRequest);

    /**
     * 获取用户所有的分享记录（包括已读和未读）
     * @param userId 用户ID
     * @param current 当前页
     * @param pageSize 页面大小
     * @return 分页的分享记录列表
     */
    Page<ShareRecordVO> getAllSharesByUserId(Long userId, long current, long pageSize);

    /**
     * 标记单个分享记录为已读
     * @param id 分享记录ID
     * @return 操作结果
     */
    boolean markShareAsRead(Long id);
}
