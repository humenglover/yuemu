package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.config.DeepSeekConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.AiChatMapper;
import com.lumenglover.yuemupicturebackend.model.aichat.ChatRequest;
import com.lumenglover.yuemupicturebackend.model.entity.AiChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.AiChatVO;
import com.lumenglover.yuemupicturebackend.model.vo.ChatResponse;
import com.lumenglover.yuemupicturebackend.service.IDeepSeekService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeepSeekServiceImpl implements IDeepSeekService {

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private AiChatMapper aiChatMapper;

    private static final String CHAT_CACHE_KEY = "chat:messages:%s";
    private static final String DAILY_MESSAGE_COUNT_KEY = "chat:daily:count:%s:%s";
    private static final int DAILY_MESSAGE_LIMIT = 100;

    @Override
    public String generateResponse(String query, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 检查是否超过每日限制
        String today = java.time.LocalDate.now().toString();
        String countKey = String.format(DAILY_MESSAGE_COUNT_KEY, userId, today);
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            ThrowUtils.throwIf(count >= DAILY_MESSAGE_LIMIT,
                    ErrorCode.OPERATION_ERROR, "已达到今日消息上限(100条)，请明天再来");
        }

        try {
            // 构建 deepseek 请求对象
            ChatRequest chatRequest = buildRequest(query);

            // 执行 HTTP 请求
            HttpResponse execute = HttpUtil.createRequest(Method.POST, deepSeekConfig.getApiUrl())
                    .body(JSONUtil.toJsonStr(chatRequest))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .header("Accept", "application/json")
                    .execute();

            // 获取并日志记录响应内容
            String resp = execute.body();
            log.info("deepseek response: {}", resp);

            // 检查响应是否有效
            ThrowUtils.throwIf(!resp.startsWith("{"),
                    ErrorCode.SYSTEM_ERROR, "Invalid JSON response: " + resp);

            // 解析响应并提取内容
            ChatResponse chatResponse = JSONUtil.toBean(resp, ChatResponse.class);
            String response = extractResponse(chatResponse);

            // 增加用户今日消息计数
            incrementDailyMessageCount(userId);

            // 保存对话到 Redis
            saveChatMessage(userId, query, response);

            // 检查是否需要同步到 MySQL
            syncChatMessagesIfNeeded(userId);

            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating response for query: {}", query, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to generate response");
        }
    }

    @Override
    public Page<AiChatVO> getChatHistory(HttpServletRequest request, int current, int pageSize) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return new Page<>();
        }

        try {
            // 1. 检查并同步Redis数据到MySQL
            syncRedisToMySql(userId);

            // 2. 直接从MySQL分页查询
            Page<AiChat> dbPage = new Page<>(current, pageSize);
            QueryWrapper<AiChat> queryWrapper = new QueryWrapper<AiChat>()
                    .eq("userId", userId)
                    .orderByDesc("createTime");
            Page<AiChat> dbChats = aiChatMapper.selectPage(dbPage, queryWrapper);

            // 3. 转换为VO对象并按对话对重新排序
            List<AiChatVO> voList = new ArrayList<>();
            List<AiChat> records = dbChats.getRecords();

            for (int i = 0; i < records.size(); i += 2) {
                // 确保有成对的消息
                if (i + 1 < records.size()) {
                    AiChat userMessage = records.get(i);
                    AiChat aiMessage = records.get(i + 1);

                    // 确保 AI 的回答在前
                    if ("assistant".equals(aiMessage.getRole())) {
                        // 添加AI的回答
                        AiChatVO aiVo = new AiChatVO();
                        aiVo.setContent(aiMessage.getContent());
                        aiVo.setRole(aiMessage.getRole());
                        aiVo.setCreateTime(aiMessage.getCreateTime());
                        voList.add(aiVo);

                        // 添加用户的问题
                        AiChatVO userVo = new AiChatVO();
                        userVo.setContent(userMessage.getContent());
                        userVo.setRole(userMessage.getRole());
                        userVo.setCreateTime(userMessage.getCreateTime());
                        voList.add(userVo);
                    } else {
                        // 如果顺序不对，交换
                        AiChatVO userVo = new AiChatVO();
                        userVo.setContent(userMessage.getContent());
                        userVo.setRole(userMessage.getRole());
                        userVo.setCreateTime(userMessage.getCreateTime());
                        voList.add(userVo);

                        AiChatVO aiVo = new AiChatVO();
                        aiVo.setContent(aiMessage.getContent());
                        aiVo.setRole(aiMessage.getRole());
                        aiVo.setCreateTime(aiMessage.getCreateTime());
                        voList.add(aiVo);
                    }
                }
            }

            // 4. 构建分页结果
            Page<AiChatVO> result = new Page<>(current, pageSize);
            result.setRecords(voList);
            result.setTotal(dbChats.getTotal());

            return result;
        } catch (Exception e) {
            log.error("Failed to get chat history", e);
            return new Page<>();
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 构建 deepseek 请求对象
     */
    private ChatRequest buildRequest(String query) {
        return ChatRequest.builder()
                .model(deepSeekConfig.getModel())
                .temperature(deepSeekConfig.getTemperature())
                .max_tokens(deepSeekConfig.getMaxTokens())
                .messages(Collections.singletonList(new ChatRequest.Message("user", query)))
                .build();
    }

    /**
     * 提取回复内容
     */
    private String extractResponse(ChatResponse response) {
        return Optional.ofNullable(response)
                .map(ChatResponse::getChoices)
                .filter(choices -> !choices.isEmpty())
                .map(choices -> choices.get(0))
                .map(choice -> choice.getMessage().getContent())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "Empty response"));
    }

    private void saveChatMessage(Long userId, String query, String response) {
        if (userId == null) {
            return;
        }

        String cacheKey = String.format(CHAT_CACHE_KEY, userId);

        try {
            // 保存用户的问题
            AiChat userMessage = new AiChat(userId, query, "user", new Date());
            String userMessageJson = JSONUtil.toJsonStr(userMessage);
            stringRedisTemplate.opsForList().rightPush(cacheKey, userMessageJson);

            // 保存AI的回复
            AiChat aiMessage = new AiChat(userId, response, "assistant", new Date());
            String aiMessageJson = JSONUtil.toJsonStr(aiMessage);
            stringRedisTemplate.opsForList().rightPush(cacheKey, aiMessageJson);

            // 检查是否需要同步到MySQL
            syncChatMessagesIfNeeded(userId);
        } catch (Exception e) {
            log.error("Failed to save chat message", e);
        }
    }

    private void syncChatMessagesIfNeeded(Long userId) {
        String cacheKey = String.format(CHAT_CACHE_KEY, userId);
        Long messageCount = stringRedisTemplate.opsForList().size(cacheKey);

        if (messageCount != null && messageCount >= 100) {
            try {
                List<String> messages = stringRedisTemplate.opsForList().range(cacheKey, 0, -1);
                if (messages != null && !messages.isEmpty()) {
                    List<AiChat> chatMessages = messages.stream()
                            .map(msg -> JSONUtil.toBean(msg, AiChat.class))
                            .collect(Collectors.toList());

                    // 批量插入到MySQL
                    aiChatMapper.batchInsert(chatMessages);

                    // 清空Redis中的消息
                    stringRedisTemplate.delete(cacheKey);
                }
            } catch (Exception e) {
                log.error("Failed to sync chat messages to MySQL", e);
            }
        }
    }

    /**
     * 检查用户今日消息数量是否超限
     */
    private boolean isOverDailyLimit(Long userId) {
        String today = java.time.LocalDate.now().toString();
        String countKey = String.format(DAILY_MESSAGE_COUNT_KEY, userId, today);

        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        if (countStr == null) {
            return false;
        }

        int count = Integer.parseInt(countStr);
        return count >= DAILY_MESSAGE_LIMIT;
    }

    /**
     * 增加用户今日消息计数
     */
    private void incrementDailyMessageCount(Long userId) {
        String today = java.time.LocalDate.now().toString();
        String countKey = String.format(DAILY_MESSAGE_COUNT_KEY, userId, today);

        // 增加计数
        stringRedisTemplate.opsForValue().increment(countKey);

        // 设置过期时间（如果key不存在）
        stringRedisTemplate.expire(countKey,
                java.time.Duration.between(
                        java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
                )
        );
    }

    /**
     * 同步Redis数据到MySQL
     */
    private void syncRedisToMySql(Long userId) {
        String cacheKey = String.format(CHAT_CACHE_KEY, userId);
        List<String> messages = stringRedisTemplate.opsForList().range(cacheKey, 0, -1);

        if (messages != null && !messages.isEmpty()) {
            try {
                // 转换消息
                List<AiChat> chatMessages = messages.stream()
                        .map(msg -> JSONUtil.toBean(msg, AiChat.class))
                        .collect(Collectors.toList());

                // 批量插入到MySQL
                aiChatMapper.batchInsert(chatMessages);

                // 清空Redis中的消息
                stringRedisTemplate.delete(cacheKey);

                log.info("Successfully synced {} messages from Redis to MySQL for user {}",
                        messages.size(), userId);
            } catch (Exception e) {
                log.error("Failed to sync Redis messages to MySQL for user {}", userId, e);
            }
        }
    }
}
