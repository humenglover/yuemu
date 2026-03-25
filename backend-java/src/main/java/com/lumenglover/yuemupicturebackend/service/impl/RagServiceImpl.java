package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.lumenglover.yuemupicturebackend.config.RagConfig;
import com.lumenglover.yuemupicturebackend.model.dto.rag.PythonRagResponse;
import com.lumenglover.yuemupicturebackend.model.entity.RagSessionMessage;

import com.lumenglover.yuemupicturebackend.esdao.EsRagMemoryDao;
import com.lumenglover.yuemupicturebackend.model.entity.RagSessionSummary;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsRagMemory;
import com.lumenglover.yuemupicturebackend.service.PythonRagService;
import com.lumenglover.yuemupicturebackend.service.RagService;
import com.lumenglover.yuemupicturebackend.service.RagSessionMessageService;
import com.lumenglover.yuemupicturebackend.service.RagSessionSummaryService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private static final String KEY_CHAT_CONTEXT_SESSION = "rag:chat:context:session:%s";
    private static final String KEY_CHAT_CONTEXT_CLEARED = "rag:chat:context:session:cleared:%s";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        private Long timestamp;

        public String toPromptString() {
            return (role.equals("user") ? "用户：" : "客服：") + content;
        }
    }

    @Autowired
    private RagConfig ragConfig;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RagSessionMessageService ragSessionMessageService;
    @Autowired
    private PythonRagService pythonRagService;
    @Autowired
    private RagSessionSummaryService ragSessionSummaryService;
    @Autowired
    private EsRagMemoryDao esRagMemoryDao;
    @Autowired
    private org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    private List<ChatMessage> getSessionContext(Long sessionId) {
        if (sessionId == null) return new ArrayList<>();
        String contextRedisKey = String.format(KEY_CHAT_CONTEXT_SESSION, sessionId);
        String clearedRedisKey = String.format(KEY_CHAT_CONTEXT_CLEARED, sessionId);

        try {
            Object clearedMarker = redisTemplate.opsForValue().get(clearedRedisKey);
            if (clearedMarker != null) {
                log.info("【会话上下文】会话已清除，返回空上下文 | 会话ID: {}", sessionId);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("【会话上下文】检查清除标记失败 | 会话ID: {}", sessionId, e);
        }

        try {
            String jsonStr = (String) redisTemplate.opsForValue().get(contextRedisKey);
            if (StrUtil.isNotBlank(jsonStr)) {
                List<ChatMessage> chatMessages = JSONUtil.toList(jsonStr, ChatMessage.class);
                log.info("【会话上下文】从Redis加载成功 | 会话ID: {} | 消息数: {}", sessionId, chatMessages.size());
                return chatMessages;
            }
        } catch (Exception e) {
            log.error("【会话上下文】从Redis加载失败 | 会话ID: {}", sessionId, e);
        }

        log.info("【会话上下文】Redis无缓存，从数据库加载 | 会话ID: {}", sessionId);
        List<ChatMessage> dbContext = getContextFromDatabase(sessionId, 8);
        if (dbContext != null && !dbContext.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(contextRedisKey, JSONUtil.toJsonStr(dbContext), 24, TimeUnit.HOURS);
                log.info("【会话上下文】数据库结果存入Redis | 会话ID: {} | 消息数: {}", sessionId, dbContext.size());
            } catch (Exception e) {
                log.error("【会话上下文】存入Redis失败 | 会话ID: {}", sessionId, e);
            }
        }
        return dbContext;
    }

    private void saveSessionContext(Long sessionId, List<ChatMessage> messageList) {
        if (sessionId == null || messageList == null || messageList.isEmpty()) return;
        String contextRedisKey = String.format(KEY_CHAT_CONTEXT_SESSION, sessionId);
        String clearedRedisKey = String.format(KEY_CHAT_CONTEXT_CLEARED, sessionId);

        try {
            List<ChatMessage> filteredMessages = filterRecentRounds(messageList, 8);
            redisTemplate.opsForValue().set(contextRedisKey, JSONUtil.toJsonStr(filteredMessages), 24, TimeUnit.HOURS);
            redisTemplate.delete(clearedRedisKey);
            log.info("【会话上下文】保存Redis成功 | 会话ID: {} | 过滤后消息数: {}", sessionId, filteredMessages.size());
        } catch (Exception e) {
            log.error("【会话上下文】保存Redis失败 | 会话ID: {}", sessionId, e);
        }
    }

    @Override
    public void clearSessionContext(Long sessionId) {
        if (sessionId == null) return;
        String contextRedisKey = String.format(KEY_CHAT_CONTEXT_SESSION, sessionId);
        String clearedRedisKey = String.format(KEY_CHAT_CONTEXT_CLEARED, sessionId);
        try {
            redisTemplate.delete(contextRedisKey);
            redisTemplate.opsForValue().set(clearedRedisKey, String.valueOf(System.currentTimeMillis()), 24, TimeUnit.HOURS);
            RagSessionMessage clearMessage = new RagSessionMessage();
            clearMessage.setSessionId(sessionId);
            clearMessage.setContent("上下文已清理");
            clearMessage.setMessageType(2);
            clearMessage.setUserId(0L);
            ragSessionMessageService.save(clearMessage);
            log.info("【会话上下文】清除成功 | 会话ID: {}", sessionId);
        } catch (Exception e) {
            log.error("【会话上下文】清除失败 | 会话ID: {}", sessionId, e);
        }
    }

    @Override
    public String chat(Long userId, Long sessionId, String question) {
        if (StrUtil.isBlank(question)) {
            log.warn("【RAG问答】问题为空 | 用户ID: {} | 会话ID: {}", userId, sessionId);
            return "请输入您想咨询的问题。";
        }

        try {
            List<ChatMessage> context = getSessionContext(sessionId);

            // 将ChatMessage转换为Map格式的历史记录
            List<Map<String, String>> history = convertChatMessagesToHistory(context);

            // 获取超长记忆（LTM）
            String ltmContext = getRelevantLongTermMemory(userId, question);

            // 调用Python RAG服务
            String sessionStr = sessionId != null ? sessionId.toString() : null;
            PythonRagResponse response = pythonRagService.callPythonRagSync(question, history, sessionStr, ltmContext);

            String answer = response.getAnswer();

            List<ChatMessage> updatedContext = new ArrayList<>(context);
            updatedContext.add(new ChatMessage("user", question, System.currentTimeMillis()));
            updatedContext.add(new ChatMessage("assistant", answer, System.currentTimeMillis()));
            saveSessionContext(sessionId, updatedContext);

            log.info("【RAG问答】完成 | 用户ID: {} | 会话ID: {} | 问题: {}", userId, sessionId, question);
            return answer;
        } catch (Exception e) {
            log.error("【RAG问答】失败 | 用户ID: {} | 会话ID: {} | 问题: {}", userId, sessionId, question, e);
            return "抱歉，客服暂时无法回答您的问题，请稍后再试。";
        }
    }

    @Override
    public void chatStream(Long userId, Long sessionId, String question, Consumer<String> tokenConsumer, Runnable onComplete) {
        if (StrUtil.isBlank(question)) {
            tokenConsumer.accept("请输入您想咨询的问题。");
            onComplete.run();
            return;
        }

        try {
            List<ChatMessage> context = getSessionContext(sessionId);

            // 将ChatMessage转换为Map格式的历史记录
            List<Map<String, String>> history = convertChatMessagesToHistory(context);

            // 调用Python RAG流式接口
            String sessionStr = sessionId != null ? sessionId.toString() : null;

            // 使用StringBuilder来收集完整的回答
            StringBuilder fullAnswer = new StringBuilder();

            // 创建一个新的token consumer，用于收集完整答案
            Consumer<String> collectingTokenConsumer = token -> {
                fullAnswer.append(token);
                tokenConsumer.accept(token);
            };

            // 获取超长记忆 (LTM)
            String ltmContext = getRelevantLongTermMemory(userId, question);

            log.info("【RAG流式问答】发送上下文 | 问题: {} | 短期记忆轮数: {} | LTM长度: {}", question, history.size() / 2, ltmContext.length());
            if (StrUtil.isNotBlank(ltmContext)) {
                log.info("【RAG流式问答】LTM全文:\n{}", ltmContext);
            }

            // 使用新的流式处理方法
            pythonRagService.callPythonRagStream(question, history, sessionStr, ltmContext, collectingTokenConsumer, () -> {
                // 在流式处理完成后，保存完整的上下文
                List<ChatMessage> currentContext = getSessionContext(sessionId);
                List<ChatMessage> updatedContext = new ArrayList<>(currentContext);
                updatedContext.add(new ChatMessage("user", question, System.currentTimeMillis()));
                updatedContext.add(new ChatMessage("assistant", fullAnswer.toString(), System.currentTimeMillis()));
                saveSessionContext(sessionId, updatedContext);

                log.info("【RAG流式问答】流式处理完成 | 用户ID: {} | 会话ID: {} | 问题: {} | 最终答案长度: {}",
                        userId, sessionId, question, fullAnswer.length());
                onComplete.run();
            });
        } catch (Exception e) {
            log.error("【RAG流式问答】失败 | 用户ID: {} | 会话ID: {} | 问题: {}", userId, sessionId, question, e);
            tokenConsumer.accept("抱歉，客服暂时无法回答您的问题，请稍后再试。");
            onComplete.run();
        }
    }



    @Override
    public void clearUserContext(Long userId) {
        log.warn("【会话上下文】仅支持按sessionId清除，请调用clearSessionContext | 用户ID: {}", userId);
    }

    private List<ChatMessage> filterRecentRounds(List<ChatMessage> messageList, int maxRounds) {
        if (messageList == null || messageList.isEmpty() || maxRounds <= 0) return new ArrayList<>();
        List<ChatMessage> filtered = new ArrayList<>();
        List<ChatMessage> reversed = new ArrayList<>(messageList);
        Collections.reverse(reversed);

        int roundCount = 0;
        for (int i = 0; i < reversed.size() && roundCount < maxRounds; i++) {
            ChatMessage msg = reversed.get(i);
            if (msg.getRole().equals("assistant")) {
                filtered.add(msg);
                if (i + 1 < reversed.size() && reversed.get(i + 1).getRole().equals("user")) {
                    filtered.add(reversed.get(i + 1));
                    i++;
                }
                roundCount++;
            }
        }
        Collections.reverse(filtered);
        return filtered;
    }



    private List<ChatMessage> getContextFromDatabase(Long sessionId, int limit) {
        try {
            List<RagSessionMessage> dbMessages = ragSessionMessageService.getRecentMessagesBySessionId(sessionId, limit);
            if (dbMessages == null || dbMessages.isEmpty()) {
                log.info("【会话上下文】数据库无历史消息 | 会话ID: {}", sessionId);
                return new ArrayList<>();
            }
            dbMessages.sort(Comparator.comparing(RagSessionMessage::getCreateTime));
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (RagSessionMessage dbMessage : dbMessages) {
                String role = dbMessage.getMessageType() == 1 ? "user" : "assistant";
                chatMessages.add(new ChatMessage(role, dbMessage.getContent(), dbMessage.getCreateTime().getTime()));
            }
            return chatMessages;
        } catch (Exception e) {
            log.error("【会话上下文】数据库加载失败 | 会话ID: {}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从 ES 中检索相关的历史记忆
     */
    private String getRelevantLongTermMemory(Long userId, String question) {
        if (userId == null || StrUtil.isBlank(question)) return "";
        try {
            log.info("【超长记忆-检索】开始 | 用户ID: {} | 问题: {}", userId, question);

            // 在 ES 中搜索相关记忆，按相似度排序，优先返回高层级摘要
            org.elasticsearch.index.query.BoolQueryBuilder boolQuery = org.elasticsearch.index.query.QueryBuilders.boolQuery()
                    .must(org.elasticsearch.index.query.QueryBuilders.termQuery("userId", userId))
                    .must(org.elasticsearch.index.query.QueryBuilders.multiMatchQuery(question, "content"));

            org.springframework.data.elasticsearch.core.SearchHits<EsRagMemory> searchHits =
                    elasticsearchOperations.search(new org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder()
                            .withQuery(boolQuery)
                            .withPageable(org.springframework.data.domain.PageRequest.of(0, 5))
                            .build(), EsRagMemory.class);

            if (searchHits.isEmpty()) {
                log.info("【超长记忆-检索】无命中结果");
                return "";
            }

            log.info("【超长记忆-检索】命中 {} 条", searchHits.getSearchHits().size());

            StringBuilder ltm = new StringBuilder("【相关历史记忆（跨度越大越宏观）】\n");
            int i = 1;
            for (org.springframework.data.elasticsearch.core.SearchHit<EsRagMemory> hit : searchHits) {
                EsRagMemory memory = hit.getContent();
                String prefix = (memory.getSummaryLevel() != null && memory.getSummaryLevel() == 1) ? "[全局摘要] " : "[局部摘要] ";
                String contentPreview = memory.getContent() != null && memory.getContent().length() > 50
                        ? memory.getContent().substring(0, 50) + "..."
                        : memory.getContent();
                log.info("【超长记忆-检索】第{}条 | 分数: {} | 级别: {} | 内容: {}",
                        i, hit.getScore(), memory.getSummaryLevel(), contentPreview);
                ltm.append(i++).append(". ").append(prefix).append(memory.getContent()).append("\n");
            }

            log.info("【超长记忆-检索】最终LTM长度: {}", ltm.length());
            return ltm.toString();
        } catch (Exception e) {
            log.error("【超长记忆】检索失败", e);
            return "";
        }
    }
    private static final String KEY_RAG_MSG_COUNT = "rag:session:msg:count:%s";

    /**
     * 异步检查并生成会话摘要（公开给 Controller 调用）
     * 核心逻辑：用 Redis 计数，每轮对话+2，到 10 的倍数时总结最近 10 条
     */
    @Override
    public void checkAndGenerateSummaryAsync(Long sessionId, Long userId) {
        if (sessionId == null) return;
        new Thread(() -> {
            try {
                // 1. 等待数据库持久化完成
                Thread.sleep(1000);

                // 2. Redis 原子计数，每轮对话产生 2 条消息（用户+AI）
                String countKey = String.format(KEY_RAG_MSG_COUNT, sessionId);
                Long totalCount = redisTemplate.opsForValue().increment(countKey, 2);
                // 首次设置过期时间 7 天
                if (totalCount != null && totalCount == 2) {
                    redisTemplate.expire(countKey, 7, TimeUnit.DAYS);
                }

                // 3. 只有总数是 10 的倍数时才触发总结
                if (totalCount == null || totalCount < 10 || totalCount % 10 != 0) {
                    return;
                }

                log.info("【超长记忆】Redis计数达到 {} 条，触发一级摘要 | 会话ID: {}", totalCount, sessionId);

                // 4. 取最近 10 条消息进行总结
                List<RagSessionMessage> latestMessages = ragSessionMessageService.list(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RagSessionMessage>()
                                .eq("sessionId", sessionId)
                                .eq("isDelete", 0)
                                .orderByDesc("id")
                                .last("LIMIT 10")
                );

                // 按时间正序排列
                java.util.Collections.reverse(latestMessages);

                // 5. 拼接待总结的对话文本
                StringBuilder toSummarize = new StringBuilder();
                for (RagSessionMessage msg : latestMessages) {
                    toSummarize.append(msg.getMessageType() == 1 ? "用户: " : "助手: ")
                            .append(msg.getContent()).append("\n");
                }

                log.info("【超长记忆】触发一级摘要 | 会话ID: {} | 最近10条消息", sessionId);

                // 6. 调用 Python 专用摘要接口
                PythonRagResponse response = pythonRagService.callPythonSummarize(toSummarize.toString());
                String summaryContent = response.getAnswer();

                if (StrUtil.isNotBlank(summaryContent)) {
                    // 保存摘要到数据库
                    RagSessionSummary newSummary = new RagSessionSummary();
                    newSummary.setSessionId(sessionId);
                    newSummary.setUserId(userId);
                    newSummary.setContent(summaryContent);
                    newSummary.setSummaryLevel(0);
                    newSummary.setLastMessageId(latestMessages.get(latestMessages.size() - 1).getId());
                    ragSessionSummaryService.save(newSummary);

                    // 同步到 ES
                    saveToEs(sessionId, userId, summaryContent, 0);
                    log.info("【超长记忆】一级摘要保存成功 | 长度: {}", summaryContent.length());

                    // 7. 检查一级摘要数量是否达到 10 条，触发二级摘要
                    checkAndGenerateLevel1Summary(sessionId, userId);
                } else {
                    log.warn("【超长记忆】一级摘要返回空，跳过本次总结");
                }
            } catch (Exception e) {
                log.error("【超长记忆】摘要生成任务异常", e);
            }
        }).start();
    }

    /**
     * 检查并生成二级摘要 (Level 1)
     * 当一级摘要数量达到 10 的倍数时，将最近 10 条一级摘要聚合为一条二级摘要
     */
    private void checkAndGenerateLevel1Summary(Long sessionId, Long userId) {
        try {
            long level0Count = ragSessionSummaryService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RagSessionSummary>()
                            .eq("sessionId", sessionId)
                            .eq("summaryLevel", 0)
            );

            if (level0Count < 10 || level0Count % 10 != 0) {
                return;
            }

            log.info("【超长记忆】一级摘要达到 {} 条，触发二级摘要 | 会话ID: {}", level0Count, sessionId);

            // 取最近 10 条一级摘要
            List<RagSessionSummary> latestLevel0 = ragSessionSummaryService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RagSessionSummary>()
                            .eq("sessionId", sessionId)
                            .eq("summaryLevel", 0)
                            .orderByDesc("id")
                            .last("LIMIT 10")
            );

            StringBuilder toSummarize = new StringBuilder();
            for (RagSessionSummary s : latestLevel0) {
                toSummarize.append("- ").append(s.getContent()).append("\n");
            }

            PythonRagResponse response = pythonRagService.callPythonSummarize(
                    "这是几个阶段性总结，请进一步浓缩：\n" + toSummarize.toString());
            String megaSummary = response.getAnswer();

            if (StrUtil.isNotBlank(megaSummary)) {
                RagSessionSummary newMega = new RagSessionSummary();
                newMega.setSessionId(sessionId);
                newMega.setUserId(userId);
                newMega.setContent(megaSummary);
                newMega.setSummaryLevel(1);
                newMega.setLastMessageId(latestLevel0.get(0).getLastMessageId());
                ragSessionSummaryService.save(newMega);

                saveToEs(sessionId, userId, megaSummary, 1);
                log.info("【超长记忆】二级摘要保存成功 | 深度覆盖已达 100+ 轮");
            }
        } catch (Exception e) {
            log.error("【超长记忆】二级摘要生成失败", e);
        }
    }

    private void saveToEs(Long sessionId, Long userId, String content, Integer level) {
        try {
            EsRagMemory esMemory = new EsRagMemory();
            esMemory.setUserId(userId);
            esMemory.setSessionId(sessionId);
            esMemory.setContent(content);
            esMemory.setMemoryType(0); // 摘要类型
            esMemory.setSummaryLevel(level);
            esMemory.setCreateTime(new java.util.Date());
            esRagMemoryDao.save(esMemory);
        } catch (Exception e) {
            log.error("【超长记忆】保存到 ES 失败", e);
        }
    }

    /**
     * 将ChatMessage列表转换为Map格式的历史记录
     */
    private List<Map<String, String>> convertChatMessagesToHistory(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return CollUtil.newArrayList();
        }

        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            Map<String, String> message = new HashMap<>();
            message.put("role", chatMessage.getRole());
            message.put("content", chatMessage.getContent());
            history.add(message);
        }
        return history;
    }
}
