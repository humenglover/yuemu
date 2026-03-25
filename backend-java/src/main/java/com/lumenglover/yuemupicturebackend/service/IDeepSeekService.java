package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.AiChat;
import com.lumenglover.yuemupicturebackend.model.vo.AiChatVO;

import javax.servlet.http.HttpServletRequest;

public interface IDeepSeekService extends IService<AiChat> {
    /**
     * 提问生成回复
     */
    String generateResponse(String query, HttpServletRequest request);

    /**
     * 获取用户聊天历史
     */
    Page<AiChatVO> getChatHistory(HttpServletRequest request, int current, int pageSize);

    /**
     * AI助手生成回复（用于公共空间）
     * @param query 问题内容
     * @return AI回复内容
     */
    String generateAssistantResponse(String query);

    /**
     * 管理员获取所有聊天记录
     *
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param userId 用户ID（可选）
     * @param role 角色类型（可选）
     * @return 聊天记录分页列表
     */
    Page<AiChat> listChatByPageAdmin(int current, int pageSize, Long userId, String role);
}
