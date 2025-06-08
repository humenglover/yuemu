package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}
