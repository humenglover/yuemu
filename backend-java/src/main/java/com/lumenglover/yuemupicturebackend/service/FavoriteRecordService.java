package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.favoriterecord.FavoriteRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.favoriterecord.FavoriteRecordQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.FavoriteRecord;
import com.lumenglover.yuemupicturebackend.model.vo.FavoriteRecordVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 收藏记录服务
 */
public interface FavoriteRecordService extends IService<FavoriteRecord> {

    /**
     * 添加收藏记录
     *
     * @param favoriteRecordAddRequest 请求参数
     * @param request                  HTTP请求
     * @return 收藏记录ID
     */
    long addFavoriteRecord(FavoriteRecordAddRequest favoriteRecordAddRequest, HttpServletRequest request);

    /**
     * 检查是否已经收藏
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 是否已收藏
     */
    boolean hasFavorited(Long userId, Long targetId, Integer targetType);

    /**
     * 取消收藏
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 是否成功
     */
    boolean cancelFavorite(Long userId, Long targetId, Integer targetType);

    /**
     * 获取用户被收藏历史（分页）
     *
     * @param favoriteRecordQueryRequest 查询参数
     * @param userId                     用户ID
     * @return 分页的收藏记录列表
     */
    Page<FavoriteRecordVO> getUserFavoriteHistory(FavoriteRecordQueryRequest favoriteRecordQueryRequest, Long userId);

    /**
     * 获取用户自己的收藏历史（分页）
     *
     * @param favoriteRecordQueryRequest 查询参数
     * @param userId                     用户ID
     * @return 分页的收藏记录列表
     */
    Page<FavoriteRecordVO> getMyFavoriteHistory(FavoriteRecordQueryRequest favoriteRecordQueryRequest, Long userId);

    /**
     * 获取并清除用户未读的收藏消息
     *
     * @param userId 用户ID
     * @return 未读收藏消息列表
     */
    List<FavoriteRecordVO> getAndClearUnreadFavorites(Long userId);

    /**
     * 获取用户未读收藏数
     *
     * @param userId 用户ID
     * @return 未读收藏数
     */
    long getUnreadFavoritesCount(Long userId);
}
