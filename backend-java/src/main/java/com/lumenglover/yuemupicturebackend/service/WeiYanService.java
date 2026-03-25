package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.WeiYan;

import java.util.List;

/**
 * 微言服务
 */
public interface WeiYanService extends IService<WeiYan> {

    /**
     * 创建微言
     * @param weiYan 微言信息
     * @param loveBoardId 恋爱板id
     * @param userId 发布用户id
     * @return 新创建的微言id
     */
    long addWeiYan(WeiYan weiYan, long loveBoardId, long userId);

    /**
     * 删除微言
     * @param id 微言id
     * @param loveBoardId 恋爱板id
     * @param userId 当前用户id
     * @return 是否成功
     */
    boolean deleteWeiYan(long id, long loveBoardId, long userId);

    /**
     * 修改微言
     * @param weiYan 微言信息
     * @param loveBoardId 恋爱板id
     * @param userId 当前用户id
     * @return 是否成功
     */
    boolean updateWeiYan(WeiYan weiYan, long loveBoardId, long userId);

    /**
     * 根据id获取微言
     * @param id 微言id
     * @param loveBoardId 恋爱板id
     * @param userId 当前用户id（可为null）
     * @return 微言信息
     */
    WeiYan getWeiYanById(long id, long loveBoardId, Long userId);

    /**
     * 获取恋爱板的微言列表
     * @param loveBoardId 恋爱板id
     * @param userId 当前用户id（可为null）
     * @return 微言列表
     */
    List<WeiYan> listLoveBoardWeiYan(long loveBoardId, Long userId);

    /**
     * 获取公开的微言列表
     * @return 公开的微言列表
     */
    List<WeiYan> listPublicWeiYan();

    /**
     * 获取微言列表（分页）
     * @param loveBoardId 恋爱板id
     * @param userId 用户id（如果是恋爱板主人则可以看到所有内容）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 分页后的微言列表
     */
    Page<WeiYan> listWeiYan(Long loveBoardId, Long userId, long current, long pageSize);

    /**
     * 点赞微言
     * @param id 微言id
     * @return 是否成功
     */
    boolean likeWeiYan(long id);
}
