package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.message.AddMessage;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Message;
import com.lumenglover.yuemupicturebackend.model.vo.MessageVO;

import java.util.List;


/**
 * @author 鹿梦
 * @description 针对表【message(留言板表)】的数据库操作Service
 * @createDate 2025-01-03 16:28:14
 */
public interface MessageService extends IService<Message> {

    /**
     * 添加留言
     * @param addMessage 添加留言请求
     * @return 是否成功
     */
    Boolean addMessage(AddMessage addMessage);

    /**
     * 获取前500条留言
     * @return 留言列表
     */
    List<MessageVO> getTop500();

    /**
     * 分页获取留言列表
     * @param messageQueryRequest 查询条件
     * @return 分页结果
     */
    Page<Message> page(MessageQueryRequest messageQueryRequest);

    /**
     * 获取查询条件
     * @param messageQueryRequest 查询条件
     * @return QueryWrapper
     */
    QueryWrapper<Message> getQueryWrapper(MessageQueryRequest messageQueryRequest);

    /**
     * 删除留言
     * @param id 留言id
     * @return 是否成功
     */
    boolean deleteMessage(long id);
}
