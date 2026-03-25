package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.RagService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * RAG智能客服控制器
 */
@Slf4j
@RestController
@RequestMapping("/rag")
public class RagController {

    @Resource
    private RagService ragService;

    @Resource
    private UserService userService;

    /**
     * 智能客服对话接口
     *
     * @param question 用户问题
     * @param request HTTP请求
     * @return 智能客服回答
     */
    @PostMapping("/chat")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "rag_chat", time = 60, count = 10, message = "RAG客服对话过于频繁，请稍后再试")
    public BaseResponse<RagChatResponse> chat(@RequestParam String question, HttpServletRequest request) {
        if (question == null || question.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "问题不能为空");
        }

        try {
            // 获取当前登录用户ID
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();

            // 调用RAG服务获取回答（sessionId=userId，保持会话一致性）
            String answer = ragService.chat(userId, userId, question);

            // 构造响应
            RagChatResponse response = new RagChatResponse();
            response.setAnswer(answer);
            response.setCacheHit(false);

            return ResultUtils.success(response);
        } catch (Exception e) {
            log.error("RAG客服对话失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "客服暂时无法回答您的问题");
        }
    }

    /**
     * 清除用户对话上下文
     *
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/clearContext")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "rag_clear_context", time = 60, count = 5, message = "清除RAG上下文过于频繁，请稍后再试")
    public BaseResponse<Boolean> clearContext(HttpServletRequest request) {
        try {
            // 获取当前登录用户ID
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();

            // 清除用户对话上下文（sessionId=userId）
            ragService.clearSessionContext(userId);

            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("清除对话上下文失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "清除对话上下文失败");
        }
    }

    /**
     * 智能客服流式对话接口
     *
     * @param question 用户问题
     * @param request HTTP请求
     * @return SseEmitter 流式响应
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "rag_chat_stream", time = 60, count = 10, message = "RAG流式对话过于频繁，请稍后再试")
    public SseEmitter chatStream(@RequestParam String question, HttpServletRequest request) {
        if (question == null || question.trim().isEmpty()) {
            // 在抛出异常前先关闭连接，避免资源泄露
            SseEmitter emitter = new SseEmitter(1L); // 设置为1ms超时，快速关闭
            emitter.complete();
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "问题不能为空");
        }

        // 创建SseEmitter，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        try {
            // 获取当前登录用户ID
            User loginUser = userService.getLoginUser(request);
            Long userId = loginUser.getId();

            // 调用RAG服务的流式方法
            // 在新线程中处理，避免阻塞主线程
            new Thread(() -> {
                try {
                    ragService.chatStream(userId, userId, question, token -> {
                        try {
                            emitter.send(SseEmitter.event().data(token));
                        } catch (Exception e) {
                            log.error("发送流式响应失败", e);
                            emitter.complete();
                        }
                    }, () -> {
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("完成流式响应失败", e);
                        }
                    });
                } catch (Exception e) {
                    log.error("处理流式响应失败", e);
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ex) {
                        log.error("完成流式响应时出错", ex);
                        emitter.complete();
                    }
                }
            }).start();
        } catch (Exception e) {
            log.error("创建流式响应失败", e);
            emitter.complete();
        }

        return emitter;
    }

    /**
     * RAG客服响应类
     */
    public static class RagChatResponse {
        private String answer;
        private Boolean cacheHit;

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public Boolean getCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(Boolean cacheHit) {
            this.cacheHit = cacheHit;
        }
    }
}
