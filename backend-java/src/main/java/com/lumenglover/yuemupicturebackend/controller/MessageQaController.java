package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.rag.QaMessageAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.rag.QaMessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagSessionMessage;
import com.lumenglover.yuemupicturebackend.model.entity.RagUserSession;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.RagMessageVO;
import com.lumenglover.yuemupicturebackend.service.RagSessionMessageService;
import com.lumenglover.yuemupicturebackend.service.RagUserSessionService;
import com.lumenglover.yuemupicturebackend.service.RagService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

/**
 * 消息问答控制层
 */
@Slf4j
@RestController
@RequestMapping("/rag/qa/message")
public class MessageQaController {

    @Resource
    private RagSessionMessageService ragSessionMessageService;

    @Resource
    private RagUserSessionService ragUserSessionService;

    @Resource
    private RagService ragService;

    @Resource
    private UserService userService;

    /**
     * 安全获取用户ID，避免异常
     */
    private Long getUserIdSafely(HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            return loginUser != null ? loginUser.getId() : null;
        } catch (Exception e) {
            log.warn("无法获取登录用户ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送消息（用户提问 + 自动生成 AI 回答，一体化）
     *
     * @param messageAddRequest 消息添加请求
     * @param request HTTP请求
     * @return 用户消息和AI消息
     */
    @PostMapping("/send")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "message_qa_send", time = 60, count = 15, message = "发送消息过于频繁，请稍后再试")
    public BaseResponse<RagMessageVO> sendQaMessage(@RequestBody QaMessageAddRequest messageAddRequest, HttpServletRequest request) {
        if (messageAddRequest == null || messageAddRequest.getContent() == null || messageAddRequest.getContent().trim().isEmpty()) {
            log.warn("RAG消息发送收到空内容");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        try {
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();
            String content = messageAddRequest.getContent().trim();
            Long sessionId = messageAddRequest.getSessionId();

            log.info("开始处理RAG消息发送请求，用户ID: {}, 会话ID: {}", userId, sessionId);

            // 验证或创建会话
            RagUserSession session = validateOrCreateSession(userId, sessionId);
            Long actualSessionId = session.getId();

            // 持久化用户提问
            Long userMessageId = ragSessionMessageService.sendMessage(actualSessionId, userId, content, 1); // 1-用户提问
            RagMessageVO userMessage = RagMessageVO.objToVo(ragSessionMessageService.getById(userMessageId));

            // 更新会话时间
            ragUserSessionService.updateSessionTime(actualSessionId);

            log.info("RAG消息发送请求处理完成，用户ID: {}, 会话ID: {}, 消息ID: {}", userId, actualSessionId, userMessageId);

            return ResultUtils.success(userMessage);
        } catch (BusinessException e) {
            log.error("RAG消息发送业务异常 - 用户ID: {}, 会话ID: {}, 错误码: {}, 错误信息: {}",
                    getUserIdSafely(request), messageAddRequest != null ? messageAddRequest.getSessionId() : null, e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("RAG消息发送系统异常 - 用户ID: {}, 会话ID: {}, 异常信息: {}",
                    getUserIdSafely(request), messageAddRequest != null ? messageAddRequest.getSessionId() : null, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送消息失败");
        }
    }

    /**
     * 获取AI回答
     *
     * @param messageQueryRequest 查询条件
     * @param request HTTP请求
     * @return AI回答消息
     */
    @PostMapping("/answer")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "message_qa_answer", time = 60, count = 15, message = "获取AI回答过于频繁，请稍后再试")
    public BaseResponse<RagMessageVO> getQaAnswer(@RequestBody QaMessageQueryRequest messageQueryRequest, HttpServletRequest request) {
        if (messageQueryRequest == null || messageQueryRequest.getSessionId() == null || messageQueryRequest.getContent() == null) {
            log.warn("RAG获取AI回答收到无效参数");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID和消息内容不能为空");
        }

        try {
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();
            Long sessionId = messageQueryRequest.getSessionId();
            String content = messageQueryRequest.getContent();

            log.info("开始处理RAG获取AI回答请求，用户ID: {}, 会话ID: {}", userId, sessionId);

            // 验证会话权限
            RagUserSession session = ragUserSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId) || session.getIsDelete() == 1) {
                log.warn("RAG获取AI回答 - 会话不存在或无权限访问，sessionId: {}, userId: {}", sessionId, userId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
            }

            // 调用AI生成回答
            log.debug("RAG获取AI回答 - 开始调用ragService.chat，userId: {}, sessionId: {}, content: {}", userId, sessionId, content);
            String aiAnswer = ragService.chat(userId, sessionId, content);
            log.debug("RAG获取AI回答 - AI回答生成完成，回答长度: {}", aiAnswer != null ? aiAnswer.length() : 0);

            // 持久化AI回答
            Long aiMessageId = ragSessionMessageService.sendMessage(sessionId, userId, aiAnswer, 2); // 2-AI回答
            RagMessageVO aiMessage = RagMessageVO.objToVo(ragSessionMessageService.getById(aiMessageId));

            // 更新会话时间
            ragUserSessionService.updateSessionTime(sessionId);

            // 触发异步摘要逻辑
            ragService.checkAndGenerateSummaryAsync(sessionId, userId);

            log.info("RAG获取AI回答请求处理完成，用户ID: {}, 会话ID: {}, 消息ID: {}", userId, sessionId, aiMessageId);

            return ResultUtils.success(aiMessage);
        } catch (BusinessException e) {
            log.error("RAG获取AI回答业务异常 - 用户ID: {}, 会话ID: {}, 错误码: {}, 错误信息: {}",
                    getUserIdSafely(request), messageQueryRequest != null ? messageQueryRequest.getSessionId() : null, e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("RAG获取AI回答系统异常 - 用户ID: {}, 会话ID: {}, 异常信息: {}",
                    getUserIdSafely(request), messageQueryRequest != null ? messageQueryRequest.getSessionId() : null, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取AI回答失败");
        }
    }

    /**
     * 会话消息列表查询
     *
     * @param messageQueryRequest 查询条件
     * @param request HTTP请求
     * @return 消息列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "message_qa_list", time = 60, count = 30, message = "消息列表查询过于频繁，请稍后再试")
    public BaseResponse<IPage<RagMessageVO>> listQaMessages(QaMessageQueryRequest messageQueryRequest, HttpServletRequest request) {
        if (messageQueryRequest == null || messageQueryRequest.getSessionId() == null) {
            log.warn("RAG消息列表查询收到无效参数");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }

        try {
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();
            Long sessionId = messageQueryRequest.getSessionId();

            log.info("开始处理RAG消息列表查询请求，用户ID: {}, 会话ID: {}", userId, sessionId);

            // 验证会话权限
            RagUserSession session = ragUserSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId) || session.getIsDelete() == 1) {
                log.warn("RAG消息列表查询 - 会话不存在或无权限访问，sessionId: {}, userId: {}", sessionId, userId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
            }

            // 设置查询条件
            messageQueryRequest.setSessionId(sessionId);
            messageQueryRequest.setUserId(userId);
            // 设置按创建时间降序排序（最新的在前，最旧的在后），这样第一页返回最新的数据，触顶加载时再加载更早的数据
            messageQueryRequest.setSortField("createTime");
            messageQueryRequest.setSortOrder("descend");

            IPage<RagMessageVO> messagePage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
            IPage<RagSessionMessage> page = ragSessionMessageService.getMessagePage(messageQueryRequest);

            // 转换为VO分页对象
            messagePage.setCurrent(page.getCurrent());
            messagePage.setSize(page.getSize());
            messagePage.setTotal(page.getTotal());
            messagePage.setRecords(page.getRecords().stream()
                    .map(RagMessageVO::objToVo)
                    .collect(Collectors.toList()));

            log.info("RAG消息列表查询请求处理完成，用户ID: {}, 会话ID: {}，共查询到 {} 条记录", userId, sessionId, page.getTotal());

            return ResultUtils.success(messagePage);
        } catch (BusinessException e) {
            log.error("RAG消息列表查询业务异常 - 用户ID: {}, 会话ID: {}, 错误码: {}, 错误信息: {}",
                    getUserIdSafely(request), messageQueryRequest != null ? messageQueryRequest.getSessionId() : null, e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("RAG消息列表查询系统异常 - 用户ID: {}, 会话ID: {}, 异常信息: {}",
                    getUserIdSafely(request), messageQueryRequest != null ? messageQueryRequest.getSessionId() : null, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询消息列表失败");
        }
    }

    @GetMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "message_qa_stream", time = 60, count = 15, message = "发送流式消息过于频繁，请稍后再试")
    public SseEmitter sendQaStreamMessage(
            @RequestParam(value = "message", required = true) String content,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response) {

        // 关键：设置响应头以防止代理/中间件缓冲SSE流
        response.setHeader("X-Accel-Buffering", "no"); // 禁用Nginx缓冲
        response.setHeader("Cache-Control", "no-cache"); // 禁用缓存
        response.setHeader("Connection", "keep-alive"); // 保持连接
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        // 强制最小化响应缓冲区，确保数据尽快发出
        try {
            response.setBufferSize(0);
        } catch (Exception e) {
            log.warn("无法将响应缓冲区设置为0，项目可能已开始输出: {}", e.getMessage());
        }
        if (content == null || content.trim().isEmpty()) {
            log.warn("RAG流式消息发送收到空内容");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        String reqContent = content.trim();
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        log.info("开始处理RAG流式消息发送请求，用户ID: {}, 会话ID: {}", userId, sessionId);
        SseEmitter emitter = new SseEmitter(180000L);

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            log.info("SSE连接完成");
        });

        try {
            RagUserSession session = validateOrCreateSession(userId, sessionId);
            Long actualSessionId = session.getId();

            Long userMessageId = ragSessionMessageService.sendMessage(actualSessionId, userId, reqContent, 1);
            RagMessageVO userMessage = RagMessageVO.objToVo(ragSessionMessageService.getById(userMessageId));

            emitter.send(SseEmitter.event()
                    .name("userMsg")
                    .data(userMessage));

            ragUserSessionService.updateSessionTime(actualSessionId);
            StringBuilder fullAnswer = new StringBuilder();

            ragService.chatStream(userId, actualSessionId, reqContent, token -> {
                // 使用synchronized确保SseEmitter线程安全，防止并发冲突导致的数据丢失
                synchronized (emitter) {
                    try {
                        fullAnswer.append(token);
                        log.info("【SSE调试】准备同步发送块 | 长度: {} | 内容预览: [{}]",
                                token != null ? token.length() : 0,
                                token != null ? (token.length() > 20 ? token.substring(0, 20) + "..." : token) : "null");

                        // 这会强制 Spring 使用 Jackson 进行序列化，自动转义 \n 等特殊字符
                        java.util.Map<String, String> tokenMap = new java.util.HashMap<>();
                        tokenMap.put("token", token);

                        emitter.send(SseEmitter.event()
                                .name("aiAnswerChunk")
                                .data(tokenMap));

                        // 关键修复：显式刷新响应缓冲区，强制底层Socket输出
                        try {
                            response.flushBuffer();
                        } catch (Exception flushEx) {
                            log.debug("流式块发送后的刷新动作失败（可能连接已断开）: {}", flushEx.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("【SSE调试】同步发送流式响应块失败", e);
                        // 不要在这里直接completeWithError，因为这是流式的，一个块失败可能后续还能救，
                        // 但如果连不上则由onError处理
                    }
                }
            }, () -> {
                synchronized (emitter) {
                    try {
                        String accumulatedAnswer = fullAnswer.toString();
                        log.info("【SSE调试】流式传输正常结束 | 总长度: {} | 正在持久化并发送最终状态", accumulatedAnswer.length());

                        Long aiMessageId = ragSessionMessageService.sendMessage(actualSessionId, userId, accumulatedAnswer, 2);
                        RagMessageVO aiMessage = RagMessageVO.objToVo(ragSessionMessageService.getById(aiMessageId));

                        ragUserSessionService.updateSessionTime(actualSessionId);

                        // 触发异步摘要逻辑
                        ragService.checkAndGenerateSummaryAsync(actualSessionId, userId);

                        Map<String, Object> doneData = new HashMap<>();
                        doneData.put("aiMsgId", aiMessageId);
                        doneData.put("createTime", aiMessage.getCreateTime());

                        // 发送前最后冲刷一次，确保之前的 aiAnswerChunk 全都发出了
                        try { response.flushBuffer(); } catch (Exception ignored) {}

                        emitter.send(SseEmitter.event().comment("flush"));

                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(doneData));

                        // 最后一次强制冲刷
                        try { response.flushBuffer(); } catch (Exception ignored) {}

                        emitter.complete();
                        log.info("【SSE调试】SSE连接已正常关闭 | 消息ID: {}", aiMessageId);
                    } catch (Exception e) {
                        log.error("【SSE调试】完成流式响应失败", e);
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("处理RAG流式消息失败 - 用户ID: {}, 会话ID: {}, 异常信息: {}", userId, sessionId, e.getMessage(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 清除会话上下文
     *
     * @param sessionId 会话ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/clearContext")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "message_qa_clear_context", time = 60, count = 10, message = "清除会话上下文过于频繁，请稍后再试")
    public BaseResponse<RagMessageVO> clearSessionContext(@RequestParam Long sessionId, HttpServletRequest request) {
        if (sessionId == null) {
            log.warn("RAG清除会话上下文收到无效参数");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }

        try {
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();

            log.info("开始处理RAG清除会话上下文请求，用户ID: {}, 会话ID: {}", userId, sessionId);

            // 验证会话权限
            RagUserSession session = ragUserSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(userId) || session.getIsDelete() == 1) {
                log.warn("RAG清除会话上下文 - 会话不存在或无权限访问，sessionId: {}, userId: {}", sessionId, userId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
            }

            // 插入一条"上下文已清理"的消息到数据库
            RagSessionMessage clearMessage = new RagSessionMessage();
            clearMessage.setSessionId(sessionId);
            clearMessage.setContent("上下文已清理");
            clearMessage.setMessageType(2); // 2表示系统消息
            clearMessage.setUserId(userId); // 使用当前登录用户ID
            ragSessionMessageService.save(clearMessage);

            // 清除会话上下文
            ragService.clearSessionContext(sessionId);

            // 返回新添加的系统消息
            RagMessageVO messageVO = RagMessageVO.objToVo(clearMessage);

            log.info("RAG清除会话上下文请求处理完成，用户ID: {}, 会话ID: {}, 消息ID: {}", userId, sessionId, clearMessage.getId());

            return ResultUtils.success(messageVO);
        } catch (BusinessException e) {
            log.error("RAG清除会话上下文业务异常 - 用户ID: {}, 会话ID: {}, 错误码: {}, 错误信息: {}",
                    getUserIdSafely(request), sessionId, e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("RAG清除会话上下文系统异常 - 用户ID: {}, 会话ID: {}, 异常信息: {}",
                    getUserIdSafely(request), sessionId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "清除会话上下文失败");
        }
    }

    /**
     * 验证或创建会话
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 会话对象
     */
    private RagUserSession validateOrCreateSession(Long userId, Long sessionId) {
        RagUserSession session = null;

        // 如果提供了sessionId，验证会话有效性
        if (sessionId != null) {
            session = ragUserSessionService.getById(sessionId);
            if (session != null && session.getUserId().equals(userId) && session.getIsDelete() == 0) {
                // 会话有效，返回该会话
                return session;
            }
        }

        // 如果会话无效或未提供sessionId，尝试获取用户活跃会话
        session = ragUserSessionService.getActiveSession(userId);
        if (session == null) {
            // 没有活跃会话，创建新会话
            Long newSessionId = ragUserSessionService.createSession(userId);
            session = ragUserSessionService.getById(newSessionId);
        }

        return session;
    }

    /**
     * 管理员分页获取所有消息列表
     *
     * @param messageQueryRequest 查询条件
     * @return 消息列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<RagMessageVO>> listAllMessagesByPage(@RequestBody(required = false) QaMessageQueryRequest messageQueryRequest) {
        if (messageQueryRequest == null) {
            messageQueryRequest = new QaMessageQueryRequest();
        }

        IPage<RagMessageVO> messagePage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        IPage<RagSessionMessage> page = ragSessionMessageService.getMessagePage(messageQueryRequest);

        // 转换为VO分页对象
        messagePage.setCurrent(page.getCurrent());
        messagePage.setSize(page.getSize());
        messagePage.setTotal(page.getTotal());
        messagePage.setRecords(page.getRecords().stream()
                .map(RagMessageVO::objToVo)
                .collect(Collectors.toList()));

        return ResultUtils.success(messagePage);
    }

    /**
     * 管理员根据ID获取消息
     *
     * @param id 消息ID
     * @return 消息信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<RagMessageVO> getMessageById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        RagSessionMessage message = ragSessionMessageService.getById(id);
        ThrowUtils.throwIf(message == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(RagMessageVO.objToVo(message));
    }

    /**
     * 管理员删除消息
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteMessageById(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = ragSessionMessageService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员批量删除消息
     *
     * @param deleteRequestList 删除请求列表
     * @return 删除结果
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchMessages(@RequestBody List<DeleteRequest> deleteRequestList) {
        if (CollectionUtils.isEmpty(deleteRequestList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取要删除的消息ID列表
        List<Long> ids = deleteRequestList.stream()
                .map(DeleteRequest::getId)
                .collect(Collectors.toList());

        // 批量删除
        boolean result = ragSessionMessageService.removeByIds(ids);
        return ResultUtils.success(result);
    }

    /**
     * 根据会话ID获取所有消息（管理员）
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/list/session")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<RagMessageVO>> listMessagesBySessionId(@RequestParam Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        }

        // 查询会话下所有消息
        QaMessageQueryRequest queryRequest = new QaMessageQueryRequest();
        queryRequest.setSessionId(sessionId);
        queryRequest.setSortField("createTime");
        queryRequest.setSortOrder("asc");

        // 使用现有的查询方法获取所有消息
        IPage<RagSessionMessage> page = ragSessionMessageService.getMessagePage(queryRequest);
        List<RagSessionMessage> messages = page.getRecords();

        List<RagMessageVO> messageVOList = messages.stream()
                .map(RagMessageVO::objToVo)
                .collect(Collectors.toList());

        return ResultUtils.success(messageVOList);
    }
}
