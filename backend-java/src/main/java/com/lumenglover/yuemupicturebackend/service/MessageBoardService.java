package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.MessageBoard;

/**
 * 祝福板服务接口
 */
public interface MessageBoardService extends IService<MessageBoard> {

    /**
     * 添加祝福
     *
     * @param messageBoard 祝福信息
     * @return 是否成功
     */
    boolean addMessage(MessageBoard messageBoard);

    /**
     * 分页获取祝福列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @param ownerId 祝福板主人ID
     * @return 祝福列表
     */
    Page<MessageBoard> listMessagesByPage(long current, long size, Long ownerId);

    /**
     * 点赞祝福
     *
     * @param id 祝福ID
     * @return 是否成功
     */
    boolean likeMessage(Long id);

    /**
     * 修改祝福状态
     *
     * @param id     祝福ID
     * @param status 状态 0-隐藏 1-显示
     * @return 是否成功
     */
    boolean updateMessageStatus(Long id, Integer status);

    /**
     * 删除祝福
     *
     * @param id      祝福ID
     * @param ownerId 祝福板主人ID
     * @return 是否成功
     */
    boolean deleteMessage(Long id, Long ownerId);
}
