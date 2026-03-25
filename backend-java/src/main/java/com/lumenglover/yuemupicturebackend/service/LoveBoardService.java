package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardBatchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoard;

/**
 * 恋爱画板服务接口
 */
public interface LoveBoardService extends IService<LoveBoard> {
    /**
     * 创建恋爱画板
     * @param loveBoard 恋爱画板信息
     * @param loginUserId 当前登录用户ID
     * @return 创建的恋爱画板ID
     */
    long createLoveBoard(LoveBoard loveBoard, long loginUserId);

    /**
     * 更新恋爱画板
     * @param loveBoard 恋爱画板信息
     * @param loginUserId 当前登录用户ID
     * @return 是否更新成功
     */
    boolean updateLoveBoard(LoveBoard loveBoard, long loginUserId);

    /**
     * 删除恋爱画板
     * @param id 恋爱画板ID
     * @param loginUserId 当前登录用户ID
     * @return 是否删除成功
     */
    boolean deleteLoveBoard(long id, long loginUserId);

    /**
     * 获取恋爱画板详情
     * @param id 恋爱画板ID
     * @param loginUserId 当前登录用户ID，未登录则为null
     * @return 恋爱画板信息
     */
    LoveBoard getLoveBoardById(long id, Long loginUserId);

    /**
     * 检查用户是否已经创建过恋爱画板
     * @param userId 用户ID
     * @return 是否已创建
     */
    boolean hasLoveBoard(long userId);

    /**
     * 检查用户是否是恋爱板的所有者
     * @param loveBoardId 恋爱板ID
     * @param userId 用户ID
     * @return 是否是所有者
     */
    boolean isLoveBoardOwner(long loveBoardId, long userId);

    /**
     * 增加浏览量
     * @param loveBoardId 恋爱板ID
     * @return 增加后的浏览量
     */
    Long incrementViewCount(Long loveBoardId);

    /**
     * 获取浏览量
     * @param loveBoardId 恋爱板ID
     * @return 浏览量
     */
    Long getViewCount(Long loveBoardId);

    void syncViewCountToDb(Long loveBoardId);

    /**
     * 获取管理员查询条件构造器
     * @param loveBoardAdminRequest 管理员查询请求
     * @return 查询条件构造器
     */
    QueryWrapper<LoveBoard> getAdminQueryWrapper(LoveBoardAdminRequest loveBoardAdminRequest);

    /**
     * 批量操作恋爱画板
     * @param loveBoardBatchRequest 批量操作请求
     * @return 是否操作成功
     */
    boolean batchOperationLoveBoards(LoveBoardBatchRequest loveBoardBatchRequest);

    /**
     * 分页获取公共恋爱画板列表（仅显示 status=1 且未删除的）
     * @param current 当前页
     * @param size 每页条数
     * @param manName 男方姓名（可选模糊查询）
     * @param womanName 女方姓名（可选模糊查询）
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @return 分页结果
     */
    Page<LoveBoard> listPublicLoveBoards(long current, long size, String manName, String womanName, String sortField, String sortOrder);
}
