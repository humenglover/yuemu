package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.rag.SessionQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagUserSession;

import java.util.List;

/**
 * RAG用户会话服务接口
 */
public interface RagUserSessionService extends IService<RagUserSession> {

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    Long createSession(Long userId);

    /**
     * 创建新会话（带自定义名称）
     *
     * @param userId 用户ID
     * @param sessionName 会话名称
     * @return 会话ID
     */
    Long createSession(Long userId, String sessionName);

    /**
     * 查询用户会话列表
     *
     * @param sessionQueryRequest 查询条件
     * @return 会话分页列表
     */
    IPage<RagUserSession> getSessionPage(SessionQueryRequest sessionQueryRequest);

    /**
     * 切换活跃会话
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void switchSession(Long userId, Long sessionId);

    /**
     * 删除会话
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void deleteSession(Long userId, Long sessionId);

    /**
     * 获取用户活跃会话
     *
     * @param userId 用户ID
     * @return 活跃会话
     */
    RagUserSession getActiveSession(Long userId);

    /**
     * 获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<RagUserSession> getUserSessionList(Long userId);

    /**
     * 获取用户最新会话
     *
     * @param userId 用户ID
     * @return 最新会话
     */
    RagUserSession getLatestSession(Long userId);

    /**
     * 更新会话时间
     *
     * @param sessionId 会话ID
     */
    void updateSessionTime(Long sessionId);

    /**
     * 更新会话名称
     *
     * @param sessionId 会话ID
     * @param sessionName 新会话名称
     * @param userId 用户ID
     */
    boolean updateSessionName(Long sessionId, String sessionName, Long userId);
}
