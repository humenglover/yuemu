package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.mapper.RagUserSessionMapper;
import com.lumenglover.yuemupicturebackend.model.dto.rag.SessionQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagUserSession;
import com.lumenglover.yuemupicturebackend.service.RagUserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * RAG用户会话服务实现类
 * 修复：MyBatis-Plus 更新语法错误 + 条件封装优化
 */
@Slf4j
@Service
public class RagUserSessionServiceImpl extends ServiceImpl<RagUserSessionMapper, RagUserSession> implements RagUserSessionService {

    @Resource
    private RagUserSessionMapper ragUserSessionMapper;

    @Override
    public Long createSession(Long userId) {
        // 使用默认会话名称
        return createSession(userId, null);
    }

    @Override
    public Long createSession(Long userId, String sessionName) {
        try {
            log.debug("RAG用户会话服务 - 开始创建新会话，userId: {}, sessionName: {}", userId, sessionName);

            // 1. 创建新会话（设置所有必要字段）
            RagUserSession session = new RagUserSession();
            session.setUserId(userId);
            // 如果提供了自定义名称则使用，否则使用默认格式
            if (sessionName != null && !sessionName.trim().isEmpty()) {
                session.setSessionName(sessionName.trim());
                log.debug("RAG用户会话服务 - 使用自定义会话名称: {}", sessionName.trim());
            } else {
                String defaultSessionName = "会话-" + new java.text.SimpleDateFormat("MM-dd HH:mm").format(new Date());
                session.setSessionName(defaultSessionName);
                log.debug("RAG用户会话服务 - 使用默认会话名称: {}", defaultSessionName);
            }
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());
            session.setIsActive(1);
            session.setIsDelete(0);

            log.debug("RAG用户会话服务 - 新会话对象构建完成，userId: {}", userId);

            // 2. 先将用户当前活跃会话设置为非活跃状态（修复：使用LambdaUpdateWrapper）
            log.debug("RAG用户会话服务 - 开始将用户当前活跃会话设置为非活跃，userId: {}", userId);
            deactivateActiveSessions(userId);
            log.debug("RAG用户会话服务 - 用户当前活跃会话已设置为非活跃，userId: {}", userId);

            // 3. 保存新会话
            log.debug("RAG用户会话服务 - 开始保存新会话，userId: {}", userId);
            boolean saveResult = this.save(session);

            if (!saveResult) {
                log.error("RAG用户会话服务 - 保存新会话失败，userId: {}", userId);
                throw new RuntimeException("创建会话失败");
            }

            log.info("RAG用户会话服务 - 成功创建新会话，userId: {}，sessionId: {}", userId, session.getId());
            return session.getId();
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 创建会话异常，userId: {}，sessionName: {}，异常信息: {}", userId, sessionName, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public IPage<RagUserSession> getSessionPage(SessionQueryRequest sessionQueryRequest) {
        try {
            log.debug("RAG用户会话服务 - 开始查询会话分页，current: {}, size: {}",
                    sessionQueryRequest.getCurrent(), sessionQueryRequest.getPageSize());

            long current = sessionQueryRequest.getCurrent();
            long size = sessionQueryRequest.getPageSize();

            if (size > 50) {
                log.warn("RAG用户会话服务 - 分页大小超出限制，当前大小: {}，限制: 50", size);
                throw new RuntimeException("每页数量不能超过50");
            }

            LambdaQueryWrapper<RagUserSession> queryWrapper = new LambdaQueryWrapper<>();
            // 修复：使用Lambda表达式避免硬编码字段名
            if (sessionQueryRequest.getUserId() != null) {
                queryWrapper.eq(RagUserSession::getUserId, sessionQueryRequest.getUserId());
                log.debug("RAG用户会话服务 - 添加用户ID筛选条件: {}", sessionQueryRequest.getUserId());
            }
            if (sessionQueryRequest.getIsActive() != null) {
                queryWrapper.eq(RagUserSession::getIsActive, sessionQueryRequest.getIsActive());
                log.debug("RAG用户会话服务 - 添加活跃状态筛选条件: {}", sessionQueryRequest.getIsActive());
            }
            // 修复：正确处理删除状态查询条件
            if (sessionQueryRequest.getIsDelete() != null) {
                queryWrapper.eq(RagUserSession::getIsDelete, sessionQueryRequest.getIsDelete());
                log.debug("RAG用户会话服务 - 添加删除状态筛选条件: {}", sessionQueryRequest.getIsDelete());
            } else {
                // 默认只查询未删除的记录
                queryWrapper.eq(RagUserSession::getIsDelete, 0);
                log.debug("RAG用户会话服务 - 默认添加未删除筛选条件");
            }
            queryWrapper.orderByDesc(RagUserSession::getUpdateTime);
            log.debug("RAG用户会话服务 - 设置按更新时间降序排序");

            log.debug("RAG用户会话服务 - 开始执行分页查询");
            IPage<RagUserSession> page = this.page(new Page<>(current, size), queryWrapper);
            log.debug("RAG用户会话服务 - 分页查询完成，共查询到 {} 条记录，总页数: {}", page.getTotal(), page.getPages());

            return page;
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 查询会话分页异常，current: {}, size: {}，异常信息: {}",
                    sessionQueryRequest != null ? sessionQueryRequest.getCurrent() : 0,
                    sessionQueryRequest != null ? sessionQueryRequest.getPageSize() : 0,
                    e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public synchronized void switchSession(Long userId, Long sessionId) {
        try {
            log.debug("RAG用户会话服务 - 开始切换会话，userId: {}, sessionId: {}", userId, sessionId);

            // 1. 将用户所有活跃会话置为非活跃（修复：使用LambdaUpdateWrapper）
            log.debug("RAG用户会话服务 - 开始将用户所有活跃会话置为非活跃，userId: {}", userId);
            LambdaUpdateWrapper<RagUserSession> inactiveWrapper = new LambdaUpdateWrapper<>();
            inactiveWrapper.eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsActive, 1)
                    .eq(RagUserSession::getIsDelete, 0)
                    .set(RagUserSession::getIsActive, 0) // 显式设置更新字段
                    .set(RagUserSession::getUpdateTime, new Date());
            boolean inactiveResult = this.update(inactiveWrapper);
            log.debug("RAG用户会话服务 - 用户活跃会话置为非活跃操作完成，userId: {}，结果: {}", userId, inactiveResult);

            // 2. 将目标会话置为活跃（修复：使用LambdaUpdateWrapper）
            log.debug("RAG用户会话服务 - 开始将目标会话置为活跃，userId: {}, sessionId: {}", userId, sessionId);
            LambdaUpdateWrapper<RagUserSession> activeWrapper = new LambdaUpdateWrapper<>();
            activeWrapper.eq(RagUserSession::getId, sessionId)
                    .eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsDelete, 0)
                    .set(RagUserSession::getIsActive, 1) // 显式设置更新字段
                    .set(RagUserSession::getUpdateTime, new Date());
            boolean activeResult = this.update(activeWrapper);

            if (!activeResult) {
                log.error("RAG用户会话服务 - 将目标会话置为活跃失败，userId: {}, sessionId: {}", userId, sessionId);
                throw new RuntimeException("切换会话失败");
            }

            log.info("RAG用户会话服务 - 成功切换会话，userId: {}，sessionId: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 切换会话失败，userId: {}, sessionId: {}，异常信息: {}", userId, sessionId, e.getMessage(), e);
            throw new RuntimeException("切换会话失败", e);
        }
    }

    @Override
    public synchronized void deleteSession(Long userId, Long sessionId) {
        try {
            log.debug("RAG用户会话服务 - 开始删除会话，userId: {}, sessionId: {}", userId, sessionId);

            // 1. 逻辑删除会话（核心修复：使用LambdaUpdateWrapper设置更新字段）
            log.debug("RAG用户会话服务 - 开始逻辑删除会话，sessionId: {}", sessionId);
            LambdaUpdateWrapper<RagUserSession> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.eq(RagUserSession::getId, sessionId)
                    .eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsDelete, 0)
                    .set(RagUserSession::getIsDelete, 1) // 显式设置更新字段
                    .set(RagUserSession::getUpdateTime, new Date()); // 补充更新时间
            boolean deleteResult = this.update(deleteWrapper);

            if (!deleteResult) {
                log.warn("RAG用户会话服务 - 会话删除操作未影响任何记录，sessionId: {}", sessionId);
            }

            log.debug("RAG用户会话服务 - 会话逻辑删除完成，sessionId: {}，结果: {}", sessionId, deleteResult);

            // 2. 检查是否删除的是活跃会话
            log.debug("RAG用户会话服务 - 检查是否删除的是活跃会话，sessionId: {}", sessionId);
            RagUserSession deletedSession = this.getById(sessionId);
            if (deletedSession != null && deletedSession.getIsActive() == 1) {
                log.debug("RAG用户会话服务 - 检测到删除的是活跃会话，开始查找并设置新的活跃会话，userId: {}", userId);

                // 查询用户最新的未删除会话
                LambdaQueryWrapper<RagUserSession> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(RagUserSession::getUserId, userId)
                        .eq(RagUserSession::getIsDelete, 0)
                        .ne(RagUserSession::getId, sessionId) // 排除已删除会话
                        .orderByDesc(RagUserSession::getUpdateTime)
                        .last("LIMIT 1");
                RagUserSession latestSession = this.getOne(queryWrapper);

                if (latestSession != null) {
                    log.debug("RAG用户会话服务 - 找到最新会话，设置为活跃状态，latestSessionId: {}", latestSession.getId());
                    // 将最新会话置为活跃（修复：使用LambdaUpdateWrapper）
                    LambdaUpdateWrapper<RagUserSession> activeWrapper = new LambdaUpdateWrapper<>();
                    activeWrapper.eq(RagUserSession::getId, latestSession.getId())
                            .set(RagUserSession::getIsActive, 1)
                            .set(RagUserSession::getUpdateTime, new Date());
                    boolean activeResult = this.update(activeWrapper);

                    if (!activeResult) {
                        log.warn("RAG用户会话服务 - 设置新活跃会话失败，latestSessionId: {}", latestSession.getId());
                    }
                } else {
                    log.debug("RAG用户会话服务 - 未找到其他会话，用户 {} 将没有活跃会话", userId);
                }
            } else {
                log.debug("RAG用户会话服务 - 删除的不是活跃会话，无需设置新活跃会话");
            }

            log.info("RAG用户会话服务 - 成功删除会话，userId: {}，sessionId: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 删除会话失败，userId: {}, sessionId: {}，异常信息: {}", userId, sessionId, e.getMessage(), e);
            throw new RuntimeException("删除会话失败", e);
        }
    }

    @Override
    public RagUserSession getActiveSession(Long userId) {
        try {
            log.debug("RAG用户会话服务 - 开始查询用户活跃会话，userId: {}", userId);

            LambdaQueryWrapper<RagUserSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsActive, 1)
                    .eq(RagUserSession::getIsDelete, 0);

            RagUserSession activeSession = this.getOne(queryWrapper);

            log.debug("RAG用户会话服务 - 查询用户活跃会话完成，userId: {}，找到会话: {}", userId, activeSession != null ? activeSession.getId() : null);

            return activeSession;
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 查询用户活跃会话异常，userId: {}，异常信息: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<RagUserSession> getUserSessionList(Long userId) {
        try {
            log.debug("RAG用户会话服务 - 开始查询用户会话列表，userId: {}", userId);

            LambdaQueryWrapper<RagUserSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsDelete, 0)
                    .orderByDesc(RagUserSession::getUpdateTime);

            List<RagUserSession> sessionList = this.list(queryWrapper);

            log.debug("RAG用户会话服务 - 查询用户会话列表完成，userId: {}，查询到 {} 个会话", userId, sessionList.size());

            return sessionList;
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 查询用户会话列表异常，userId: {}，异常信息: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public RagUserSession getLatestSession(Long userId) {
        try {
            log.debug("RAG用户会话服务 - 开始查询用户最新会话，userId: {}", userId);

            LambdaQueryWrapper<RagUserSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsDelete, 0)
                    .orderByDesc(RagUserSession::getUpdateTime)
                    .last("LIMIT 1");

            RagUserSession latestSession = this.getOne(queryWrapper);

            log.debug("RAG用户会话服务 - 查询用户最新会话完成，userId: {}，找到会话: {}", userId, latestSession != null ? latestSession.getId() : null);

            return latestSession;
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 查询用户最新会话异常，userId: {}，异常信息: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void updateSessionTime(Long sessionId) {
        try {
            log.debug("RAG用户会话服务 - 开始更新会话时间，sessionId: {}", sessionId);

            // 修复：使用LambdaUpdateWrapper设置更新字段
            LambdaUpdateWrapper<RagUserSession> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RagUserSession::getId, sessionId)
                    .set(RagUserSession::getUpdateTime, new Date()); // 显式设置更新字段

            boolean updateResult = this.update(updateWrapper);

            if (!updateResult) {
                log.warn("RAG用户会话服务 - 更新会话时间未影响任何记录，sessionId: {}", sessionId);
            }

            log.debug("RAG用户会话服务 - 更新会话时间完成，sessionId: {}，结果: {}", sessionId, updateResult);
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 更新会话时间异常，sessionId: {}，异常信息: {}", sessionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 将用户当前活跃会话设置为非活跃状态（修复：使用LambdaUpdateWrapper）
     */
    private void deactivateActiveSessions(Long userId) {
        try {
            log.debug("RAG用户会话服务 - 开始将用户活跃会话设置为非活跃，userId: {}", userId);

            LambdaUpdateWrapper<RagUserSession> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RagUserSession::getUserId, userId)
                    .eq(RagUserSession::getIsActive, 1)
                    .eq(RagUserSession::getIsDelete, 0)
                    .set(RagUserSession::getIsActive, 0) // 显式设置更新字段
                    .set(RagUserSession::getUpdateTime, new Date()); // 补充更新时间

            boolean updateResult = this.update(updateWrapper);

            log.debug("RAG用户会话服务 - 用户活跃会话设置为非活跃完成，userId: {}，结果: {}", userId, updateResult);
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 将用户活跃会话设置为非活跃异常，userId: {}，异常信息: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean updateSessionName(Long sessionId, String sessionName, Long userId) {
        try {
            log.debug("RAG用户会话服务 - 开始更新会话名称，sessionId: {}，userId: {}，sessionName: {}", sessionId, userId, sessionName);

            // 验证会话是否属于当前用户
            RagUserSession session = this.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                log.warn("RAG用户会话服务 - 会话不存在或不属于当前用户，sessionId: {}，userId: {}", sessionId, userId);
                return false;
            }

            log.debug("RAG用户会话服务 - 会话验证通过，开始更新会话名称");

            // 更新会话名称
            LambdaUpdateWrapper<RagUserSession> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RagUserSession::getId, sessionId)
                    .eq(RagUserSession::getUserId, userId)
                    .set(RagUserSession::getSessionName, sessionName)
                    .set(RagUserSession::getUpdateTime, new Date());

            boolean updateResult = this.update(updateWrapper);

            log.info("RAG用户会话服务 - 更新会话名称完成，sessionId: {}，userId: {}，sessionName: {}，结果: {}", sessionId, userId, sessionName, updateResult);

            return updateResult;
        } catch (Exception e) {
            log.error("RAG用户会话服务 - 更新会话名称异常，sessionId: {}，userId: {}，sessionName: {}，异常信息: {}", sessionId, userId, sessionName, e.getMessage(), e);
            throw e;
        }
    }
}
