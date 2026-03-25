package com.lumenglover.yuemupicturebackend.manager.websocket;

import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.manager.websocket.disruptor.ChatEventProducer;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.ChatMessageService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.PrivateChatService;
import com.lumenglover.yuemupicturebackend.service.IDeepSeekService;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.utils.ServletUtils;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import com.lumenglover.yuemupicturebackend.utils.ip.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatWebSocketServer extends TextWebSocketHandler {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private UserService userService;

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    @Lazy
    private ChatEventProducer chatEventProducer;

    @Resource
    private IDeepSeekService deepSeekService;

    /**
     * 用户ID -> WebSocket session
     */
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 图片ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 空间ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> spaceSessions = new ConcurrentHashMap<>();

    /**
     * 私聊ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> privateChatSessions = new ConcurrentHashMap<>();

    /**
     * WebSocket session -> 最后活跃时间
     */
    private static final Map<WebSocketSession, Long> lastActivityTime = new ConcurrentHashMap<>();
    private static final long ACTIVITY_TIMEOUT = 10 * 60 * 1000; // 10分钟超时
    private static final long HEARTBEAT_INTERVAL = 13 * 1000; // 13秒心跳间隔

    /**
     * AI消息处理线程池
     */
    private final ThreadPoolExecutor aiMessageExecutor;

    /**
     * AI消息处理队列
     */
    private final LinkedBlockingQueue<AIMessageTask> aiMessageQueue;

    public ChatWebSocketServer() {
        // 创建AI消息处理队列
        this.aiMessageQueue = new LinkedBlockingQueue<>(1000);
        // 创建单线程线程池，确保消息按顺序处理
        this.aiMessageExecutor = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ai-message-processor");
                    return thread;
                }
        );

        // 启动AI消息处理线程
        startAIMessageProcessor();

        // 启动心跳检查线程
        Thread heartbeatThread = new Thread(this::checkHeartbeats);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * AI消息任务类
     */
    private static class AIMessageTask {
        private final ChatMessage userMessage;
        private final String question;
        private final WebSocketSession session;

        public AIMessageTask(ChatMessage userMessage, String question, WebSocketSession session) {
            this.userMessage = userMessage;
            this.question = question;
            this.session = session;
        }
    }

    /**
     * 启动AI消息处理线程
     */
    private void startAIMessageProcessor() {
        aiMessageExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从队列中获取消息任务
                    AIMessageTask task = aiMessageQueue.take();
                    processAIMessage(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理AI消息任务失败", e);
                }
            }
        });
    }

    /**
     * 处理AI消息任务
     */
    private void processAIMessage(AIMessageTask task) {
        try {
            // 创建AI助手的回复消息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSenderId(0L);
            aiMessage.setSpaceId(-2L);
            aiMessage.setType(3);
            aiMessage.setCreateTime(new Date(System.currentTimeMillis() + 1000));
            aiMessage.setReplyId(task.userMessage.getId());
            aiMessage.setRootId(task.userMessage.getRootId() != null ?
                    task.userMessage.getRootId() : task.userMessage.getId());

            try {
                // 调用AI服务获取回复
                String aiResponse = deepSeekService.generateAssistantResponse(task.question);
                aiMessage.setContent(aiResponse);

                // 保存AI回复消息
                chatMessageService.save(aiMessage);
                chatMessageService.fillMessageInfo(aiMessage);

                // 设置AI助手的信息
                User aiUser = new User();
                aiUser.setId(0L);
                aiUser.setUserName("悦木小助手");
                aiUser.setUserAccount("ai_assistant");
                aiUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl("https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com//public/1866450683272450049/2025-05-26_AOTA5tKU2w6ci7hH.png"));
                aiMessage.setSender(aiUser);

                // 发送AI回复消息
                sendToSpaceRoom(aiMessage);
            } catch (BusinessException e) {
                // 如果AI服务出错，发送更友好的错误消息
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setSenderId(0L);
                errorMessage.setSpaceId(-2L);
                errorMessage.setType(3);
                errorMessage.setCreateTime(new Date(System.currentTimeMillis() + 1000));
                errorMessage.setReplyId(task.userMessage.getId());
                errorMessage.setRootId(task.userMessage.getRootId() != null ?
                        task.userMessage.getRootId() : task.userMessage.getId());
                errorMessage.setContent("非常抱歉，我暂时遇到了一些技术问题 😅\n\n请稍后再试，或者换个方式提问。如果问题持续存在，请联系管理员。");

                // 设置AI助手的信息
                User aiUser = new User();
                aiUser.setId(0L);
                aiUser.setUserName("悦木小助手");
                aiUser.setUserAccount("ai_assistant");
                aiUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl("https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com//public/1866450683272450049/2025-05-26_AOTA5tKU2w6ci7hH.png"));
                errorMessage.setSender(aiUser);

                // 保存并发送错误消息
                chatMessageService.save(errorMessage);
                chatMessageService.fillMessageInfo(errorMessage);
                sendToSpaceRoom(errorMessage);
            }
        } catch (Exception e) {
            log.error("处理AI消息失败", e);
            try {
                sendErrorMessage(task.session, "AI助手消息处理失败");
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    private void checkHeartbeats() {
        while (true) {
            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
                long now = System.currentTimeMillis();

                // 检查所有会话的最后活跃时间
                lastActivityTime.entrySet().removeIf(entry -> {
                    WebSocketSession session = entry.getKey();
                    long lastActivity = entry.getValue();
                    long inactiveTime = now - lastActivity;

                    // 记录非活跃时间超过5分钟的会话
                    if (inactiveTime > ACTIVITY_TIMEOUT / 2) {
                        User user = (User) session.getAttributes().get("user");
                        String userName = user != null ? user.getUserName() : "未知用户";
                        log.info("用户 {} 已无活动 {} 分钟（会话ID：{}）",
                                userName,
                                inactiveTime / (60 * 1000),
                                session.getId()
                        );
                    }

                    if (inactiveTime > ACTIVITY_TIMEOUT) {
                        try {
                            User user = (User) session.getAttributes().get("user");
                            String userName = user != null ? user.getUserName() : "未知用户";
                            log.info("关闭不活跃会话 - 用户：{}，会话ID：{}（已无活动 {} 分钟）",
                                    userName,
                                    session.getId(),
                                    inactiveTime / (60 * 1000)
                            );
                            session.close();
                            return true;
                        } catch (IOException e) {
                            log.error("关闭不活跃会话时发生错误：{}", session.getId(), e);
                        }
                    }
                    return false;
                });
            } catch (InterruptedException e) {
                log.warn("心跳检测线程被中断", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            User user = (User) session.getAttributes().get("user");
            Long pictureId = (Long) session.getAttributes().get("pictureId");
            Long spaceId = (Long) session.getAttributes().get("spaceId");
            Long privateChatId = (Long) session.getAttributes().get("privateChatId");

            if (user == null) {
                log.error("会话属性中未找到用户信息");
                session.close();
                return;
            }

            log.info("WebSocket连接已建立 - 会话ID：{}，用户：{}，客户端IP：{}",
                    session.getId(),
                    user.getUserName(),
                    session.getAttributes().get("clientIp")
            );

            // 保存用户session
            userSessions.put(user.getId(), session);
            lastActivityTime.put(session, System.currentTimeMillis());
            log.debug("会话 {} 的活动时间已初始化", session.getId());

            // 如果是私聊
            if (privateChatId != null) {
                log.info("用户 {} 加入私聊 {}", user.getId(), privateChatId);
                privateChatSessions.computeIfAbsent(privateChatId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sendUserChatHistory(session, privateChatId);
                broadcastOnlineUsers(null, null, privateChatId);
            }
            // 如果是图片聊天室
            else if (pictureId != null) {
                pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sendPictureChatHistory(session, pictureId);
                broadcastOnlineUsers(pictureId, null, null);  // 修改这里
            }
            // 如果是空间聊天
            else if (spaceId != null) {
                // 如果是公共空间(-2)，直接允许加入
                if (spaceId == -2) {
                    spaceSessions.computeIfAbsent(spaceId, k -> ConcurrentHashMap.newKeySet()).add(session);
                    sendSpaceChatHistory(session, spaceId);
                    broadcastOnlineUsers(null, spaceId, null);
                } else {
                    // 检查用户是否有权限加入空间聊天
                    if (!chatMessageService.canUserChatInSpace(user.getId(), spaceId)) {
                        log.error("用户不是空间成员");
                        session.close();
                        return;
                    }
                    spaceSessions.computeIfAbsent(spaceId, k -> ConcurrentHashMap.newKeySet()).add(session);
                    sendSpaceChatHistory(session, spaceId);
                    broadcastOnlineUsers(null, spaceId, null);
                }
            }
        } catch (Exception e) {
            log.error("建立连接时发生错误", e);
            throw e;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            // 使用 ObjectMapper 解析消息
            Map<String, Object> messageMap = webSocketObjectMapper.readValue(message.getPayload(), Map.class);

            // 安全地获取消息类型
            Object typeObj = messageMap.get("type");
            String type = typeObj != null ? typeObj.toString() : null;

            // 更新最后活跃时间
            lastActivityTime.put(session, System.currentTimeMillis());

            // 处理心跳消息
            if ("HEARTBEAT".equals(type)) {
                User user = (User) session.getAttributes().get("user");
                String userName = user != null ? user.getUserName() : "未知用户";
                log.info("收到心跳信号 <<< 会话ID：{}，用户：{}", session.getId(), userName);

                // 响应心跳
                Map<String, Object> response = new HashMap<>();
                response.put("type", "HEARTBEAT");
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
                log.info("发送心跳响应 >>> 会话ID：{}，用户：{}", session.getId(), userName);
                return;
            }

            if ("loadMore".equals(type)) {
                handleLoadMoreMessage(session, messageMap);
                return;
            }

            if ("getOnlineUsers".equals(type)) {
                Long pictureId = (Long) session.getAttributes().get("pictureId");
                Long spaceId = (Long) session.getAttributes().get("spaceId");
                Long privateChatId = (Long) session.getAttributes().get("privateChatId");
                broadcastOnlineUsers(pictureId, spaceId, privateChatId);
                return;
            }

            // 处理消息撤回
            if ("RECALL".equals(type)) {
                handleRecallMessage(session, messageMap);
                return;
            }

            // 处理普通消息
            ChatMessage chatMessage = new ChatMessage();
            User user = (User) session.getAttributes().get("user");

            // 检查必要字段
            String content = (String) messageMap.get("content");
            if (content == null) {
                sendErrorMessage(session, "消息内容不能为空");
                return;
            }
            chatMessage.setContent(content);

            // 设置消息类型和资源信息
            String messageType = (String) messageMap.get("messageType");
            if (messageType == null || messageType.isEmpty()) {
                messageType = "text"; // 默认设置为文本类型
            }
            chatMessage.setMessageType(messageType);

            // 优先使用前端传递的位置信息，不再强制后端 IP 定位
            String messageLocation = (String) messageMap.get("messageLocation");
            if (messageLocation == null || messageLocation.trim().isEmpty()) {
                // 前端未传位置则留空，不再调用 IP 解析
                messageLocation = "";
            }
            chatMessage.setMessageLocation(messageLocation);

            // 如果是非文本类型消息，处理资源信息
            if (!"text".equals(messageType)) {
                String messageUrl = (String) messageMap.get("messageUrl");
                if (messageUrl == null || messageUrl.isEmpty()) {
                    sendErrorMessage(session, "非文本消息必须提供资源地址");
                    return;
                }
                chatMessage.setMessageUrl(messageUrl);

                // 设置资源大小
                Object messageSizeObj = messageMap.get("messageSize");
                if (messageSizeObj != null) {
                    if (messageSizeObj instanceof Integer) {
                        chatMessage.setMessageSize(((Integer) messageSizeObj).longValue());
                    } else if (messageSizeObj instanceof Long) {
                        chatMessage.setMessageSize((Long) messageSizeObj);
                    } else {
                        try {
                            chatMessage.setMessageSize(Long.valueOf(messageSizeObj.toString()));
                        } catch (NumberFormatException e) {
                            log.warn("消息大小格式不正确，设置为0");
                            chatMessage.setMessageSize(0L);
                        }
                    }
                } else {
                    chatMessage.setMessageSize(0L);
                }
            } else {
                // 文本消息默认设置
                chatMessage.setMessageUrl(null);
                chatMessage.setMessageSize(0L);
            }

            // 确定消息类型和目标ID
            Integer messageTypeInt;
            Long targetId;

            if (session.getAttributes().get("privateChatId") != null) {
                messageTypeInt = 1;  // 私聊
                targetId = (Long) session.getAttributes().get("privateChatId");
                chatMessage.setPrivateChatId(targetId);
            } else if (session.getAttributes().get("pictureId") != null) {
                messageTypeInt = 2;  // 图片聊天室
                targetId = (Long) session.getAttributes().get("pictureId");
                chatMessage.setPictureId(targetId);
            } else if (session.getAttributes().get("spaceId") != null) {
                messageTypeInt = 3;  // 空间聊天
                targetId = (Long) session.getAttributes().get("spaceId");
                chatMessage.setSpaceId(targetId);
            } else {
                log.error("无法确定消息类型");
                sendErrorMessage(session, "无法确定消息类型");
                return;
            }
            chatMessage.setType(messageTypeInt);

            // 处理回复消息
            Object replyIdObj = messageMap.get("replyId");
            if (replyIdObj != null) {
                // 安全地转换 replyId 为 Long 类型
                Long replyId;
                if (replyIdObj instanceof Integer) {
                    replyId = ((Integer) replyIdObj).longValue();
                } else if (replyIdObj instanceof Long) {
                    replyId = (Long) replyIdObj;
                } else if (replyIdObj instanceof String) {
                    replyId = Long.parseLong((String) replyIdObj);
                } else {
                    replyId = Long.valueOf(replyIdObj.toString());
                }
                chatMessage.setReplyId(replyId);

                // 安全地转换 rootId
                Object rootIdObj = messageMap.get("rootId");
                Long rootId = null;
                if (rootIdObj != null) {
                    if (rootIdObj instanceof Integer) {
                        rootId = ((Integer) rootIdObj).longValue();
                    } else if (rootIdObj instanceof Long) {
                        rootId = (Long) rootIdObj;
                    } else if (rootIdObj instanceof String) {
                        rootId = Long.parseLong((String) rootIdObj);
                    } else {
                        rootId = Long.valueOf(rootIdObj.toString());
                    }
                }
                chatMessage.setRootId(rootId != null ? rootId : replyId);
            }

            chatMessage.setSenderId(user.getId());
            chatMessage.setCreateTime(new Date());

            // 使用 Disruptor 异步处理消息
            chatEventProducer.publishEvent(chatMessage, session, user, targetId, messageTypeInt);

        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendErrorMessage(session, "消息处理失败");
        }
    }

    /**
     * 处理消息撤回
     */
    private void handleRecallMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        try {
            User user = (User) session.getAttributes().get("user");
            Object messageIdObj = messageMap.get("messageId");

            if (messageIdObj == null) {
                sendErrorMessage(session, "消息ID不能为空");
                return;
            }

            Long messageId = Long.valueOf(messageIdObj.toString());
            ChatMessage chatMessage = chatMessageService.getById(messageId);

            if (chatMessage == null) {
                sendErrorMessage(session, "消息不存在");
                return;
            }

            // 检查是否是消息发送者
            if (!chatMessage.getSenderId().equals(user.getId())) {
                sendErrorMessage(session, "只能撤回自己的消息");
                return;
            }

            // 检查消息是否在60秒内
            long messageTime = chatMessage.getCreateTime().getTime();
            long currentTime = System.currentTimeMillis();
            if (currentTime - messageTime > 60000) {
                sendErrorMessage(session, "只能撤回60秒内的消息");
                return;
            }

            // 修改消息内容为"消息已撤回"
            chatMessage.setContent("消息已撤回");
            chatMessageService.updateById(chatMessage);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 构建撤回通知消息
            Map<String, Object> recallNotification = new HashMap<>();
            recallNotification.put("type", "RECALL");
            recallNotification.put("messageId", messageId);
            recallNotification.put("message", chatMessage);
            String recallNotificationStr = webSocketObjectMapper.writeValueAsString(recallNotification);

            // 根据消息类型广播撤回通知
            if (chatMessage.getPrivateChatId() != null) {
                Set<WebSocketSession> sessions = privateChatSessions.get(chatMessage.getPrivateChatId());
                if (sessions != null) {
                    for (WebSocketSession s : sessions) {
                        if (s.isOpen()) {
                            s.sendMessage(new TextMessage(recallNotificationStr));
                        }
                    }
                }
            } else if (chatMessage.getPictureId() != null) {
                Set<WebSocketSession> sessions = pictureSessions.get(chatMessage.getPictureId());
                if (sessions != null) {
                    for (WebSocketSession s : sessions) {
                        if (s.isOpen()) {
                            s.sendMessage(new TextMessage(recallNotificationStr));
                        }
                    }
                }
            } else if (chatMessage.getSpaceId() != null) {
                Set<WebSocketSession> sessions = spaceSessions.get(chatMessage.getSpaceId());
                if (sessions != null) {
                    for (WebSocketSession s : sessions) {
                        if (s.isOpen()) {
                            s.sendMessage(new TextMessage(recallNotificationStr));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理消息撤回失败", e);
            sendErrorMessage(session, "消息撤回失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            User user = (User) session.getAttributes().get("user");
            String userName = user != null ? user.getUserName() : "未知用户";

            log.info("WebSocket连接已关闭 - 用户：{}，会话ID：{}，状态码：{}，原因：{}",
                    userName,
                    session.getId(),
                    status.getCode(),
                    status.getReason()
            );

            Long pictureId = (Long) session.getAttributes().get("pictureId");
            Long spaceId = (Long) session.getAttributes().get("spaceId");
            Long privateChatId = (Long) session.getAttributes().get("privateChatId");

            // 移除用户session
            if (user != null) {
                userSessions.remove(user.getId());
            }

            // 如果是私聊，移除私聊session
            if (privateChatId != null) {
                Set<WebSocketSession> sessions = privateChatSessions.get(privateChatId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        privateChatSessions.remove(privateChatId);
                    } else {
                        broadcastOnlineUsers(null, null, privateChatId);
                    }
                }
            }
            // 如果是图片聊天室，移除图片session
            else if (pictureId != null) {
                Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        pictureSessions.remove(pictureId);
                    } else {
                        broadcastOnlineUsers(pictureId, null, null);
                    }
                }
            }
            // 如果是空间聊天，移除空间session
            else if (spaceId != null) {
                Set<WebSocketSession> sessions = spaceSessions.get(spaceId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        spaceSessions.remove(spaceId);
                    } else {
                        broadcastOnlineUsers(null, spaceId, null);
                    }
                }
            }
            lastActivityTime.remove(session);
            log.debug("已移除会话 {} 的活动时间跟踪", session.getId());
        } catch (Exception e) {
            log.error("关闭连接时发生错误", e);
        }
    }

    /**
     * 发送私聊消息
     */
    private void sendToPrivateChat(ChatMessage message) throws IOException {
        if (message.getPrivateChatId() == null) {
            log.error("privateChatId为空，无法发送消息");
            return;
        }

        Set<WebSocketSession> sessions = privateChatSessions.get(message.getPrivateChatId());
        if (sessions != null) {
            String messageStr = webSocketObjectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送图片聊天室消息
     */
    private void sendToPictureRoom(ChatMessage chatMessage) throws IOException {
        if (chatMessage.getPictureId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }

        Set<WebSocketSession> sessions = pictureSessions.get(chatMessage.getPictureId());
        if (sessions != null && !sessions.isEmpty()) {
            String messageStr = webSocketObjectMapper.writeValueAsString(chatMessage);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送空间聊天消息
     */
    private void sendToSpaceRoom(ChatMessage chatMessage) throws IOException {
        Set<WebSocketSession> sessions = spaceSessions.get(chatMessage.getSpaceId());
        if (sessions != null) {
            String messageStr = webSocketObjectMapper.writeValueAsString(chatMessage);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送图片聊天室的历史消息
     */
    private void sendPictureChatHistory(WebSocketSession session, Long pictureId) {
        try {
            // 获取最近的20条消息，消息中会包含发送者信息
            Page<ChatMessage> history = chatMessageService.getPictureChatHistory(pictureId, 1, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     * 发送空间聊天历史记录
     */
    private void sendSpaceChatHistory(WebSocketSession session, Long spaceId) {
        try {
            Page<ChatMessage> history = chatMessageService.getSpaceChatHistory(spaceId, 1, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送空间历史消息失败", e);
        }
    }

    /**
     * 发送私聊历史消息
     */
    private void sendUserChatHistory(WebSocketSession session, Long privateChatId) {
        try {
            // 获取最近的20条消息
            Page<ChatMessage> history = chatMessageService.getPrivateChatHistory(privateChatId, 1L, 20L);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > 1);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送私聊历史消息失败", e);
        }
    }

    private void sendMorePictureHistory(WebSocketSession session, Long pictureId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getPictureChatHistory(pictureId, page, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多历史消息失败", e);
        }
    }

    private void sendMoreUserHistory(WebSocketSession session, Long privateChatId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getPrivateChatHistory(privateChatId, page, 20L);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多私聊历史消息失败", e);
        }
    }

    private void handleLoadMoreMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        // 安全地处理数值类型转换
        Object pictureIdObj = messageMap.get("pictureId");
        Object spaceIdObj = messageMap.get("spaceId");
        Object pageObj = messageMap.get("page");

        Long pictureId = null;
        Long spaceId = null;
        Long page = null;

        // 处理页码
        if (pageObj instanceof Integer) {
            page = Long.valueOf((Integer) pageObj);
        } else if (pageObj instanceof Long) {
            page = (Long) pageObj;
        } else if (pageObj instanceof String) {
            page = Long.parseLong((String) pageObj);
        }
        page = page != null ? page : 1L;

        // 处理图片ID
        if (pictureIdObj instanceof Integer) {
            pictureId = Long.valueOf((Integer) pictureIdObj);
        } else if (pictureIdObj instanceof Long) {
            pictureId = (Long) pictureIdObj;
        } else if (pictureIdObj instanceof String) {
            pictureId = Long.parseLong((String) pictureIdObj);
        }

        // 处理空间ID
        if (spaceIdObj instanceof Integer) {
            spaceId = Long.valueOf((Integer) spaceIdObj);
        } else if (spaceIdObj instanceof Long) {
            spaceId = (Long) spaceIdObj;
        } else if (spaceIdObj instanceof String) {
            spaceId = Long.parseLong((String) spaceIdObj);
        }

        if (pictureId != null) {
            sendMorePictureHistory(session, pictureId, page);
        } else if (spaceId != null) {
            sendMoreSpaceHistory(session, spaceId, page);
        } else {
            Object privateChatIdObj = messageMap.get("privateChatId");
            Long privateChatId = null;

            if (privateChatIdObj instanceof Integer) {
                privateChatId = Long.valueOf((Integer) privateChatIdObj);
            } else if (privateChatIdObj instanceof Long) {
                privateChatId = (Long) privateChatIdObj;
            } else if (privateChatIdObj instanceof String) {
                privateChatId = Long.parseLong((String) privateChatIdObj);
            }

            if (privateChatId != null) {
                sendMoreUserHistory(session, privateChatId, page);
            }
        }
    }

    /**
     * 发送更多空间聊天历史记录
     */
    private void sendMoreSpaceHistory(WebSocketSession session, Long spaceId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getSpaceChatHistory(spaceId, page, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多空间历史消息失败", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("message", message);
        session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(errorResponse)));
    }

    /**
     * 广播在线用户信息
     */
    private void broadcastOnlineUsers(Long pictureId, Long spaceId, Long privateChatId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "onlineUsers");

            Set<WebSocketSession> targetSessions;
            if (pictureId != null) {
                targetSessions = pictureSessions.get(pictureId);
                // 获取该图片聊天室的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl(user.getUserAvatar()));
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }
                response.put("onlineCount", onlineUsers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("pictureId", pictureId);
            } else if (spaceId != null) {
                targetSessions = spaceSessions.get(spaceId);
                // 获取该空间的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl(user.getUserAvatar()));
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }

                // 获取空间所有成员
                List<User> allMembers = chatMessageService.getSpaceMembers(spaceId);

                // 计算离线用户
                Set<User> offlineUsers = new HashSet<>();
                for (User member : allMembers) {
                    boolean isOnline = onlineUsers.stream()
                            .anyMatch(onlineUser -> onlineUser.getId().equals(member.getId()));
                    if (!isOnline) {
                        offlineUsers.add(member);
                    }
                }

                response.put("onlineCount", onlineUsers.size());
                response.put("offlineCount", offlineUsers.size());
                response.put("totalCount", allMembers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("offlineUsers", offlineUsers);
                response.put("spaceId", spaceId);
            } else if (privateChatId != null) {
                targetSessions = privateChatSessions.get(privateChatId);
                // 获取该私聊的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl(user.getUserAvatar()));
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }
                response.put("onlineCount", onlineUsers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("privateChatId", privateChatId);
            } else {
                return;
            }

            if (targetSessions != null) {
                String messageStr = webSocketObjectMapper.writeValueAsString(response);
                for (WebSocketSession session : targetSessions) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(messageStr));
                    }
                }
            }
        } catch (IOException e) {
            log.error("广播在线用户信息失败", e);
        }
    }

    /**
     * 获取私聊聊天室的会话集合
     */
    public static Set<WebSocketSession> getPrivateChatSessions(Long privateChatId) {
        if (privateChatId == null) {
            return null;
        }
        return privateChatSessions.get(privateChatId);
    }

    /**
     * 发送聊天历史记录
     */
    private void sendChatHistory(WebSocketSession session, List<ChatMessage> history, boolean hasMore) {
        try {
            // 填充所有消息的信息（包括回复消息）
            history.forEach(msg -> chatMessageService.fillMessageInfo(msg));

            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history);
            response.put("hasMore", hasMore);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     * 处理图片聊天室消息
     */
    public void handlePictureChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            if (chatMessage.getPictureId() == null) {
                log.error("图片ID为空，无法处理消息");
                sendErrorMessage(session, "消息处理失败");
                return;
            }

            // 保存消息
            chatMessage.setContent(SensitiveUtil.filter(chatMessage.getContent()));
            chatMessageService.save(chatMessage);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 填充发送者信息
            User sender = userService.getById(chatMessage.getSenderId());
            chatMessage.setSender(sender);

            // 发送消息
            sendToPictureRoom(chatMessage);
        } catch (Exception e) {
            log.error("处理图片聊天室消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 处理空间聊天消息
     */
    public void handleSpaceChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            // 如果是公共空间且内容中包含@悦木小助手
            // 过滤消息内容中的敏感词
            String filteredContent = SensitiveUtil.filter(chatMessage.getContent());
            chatMessage.setContent(filteredContent);
            if (chatMessage.getSpaceId() == -2 && chatMessage.getContent().contains("@悦木小助手")) {
                // 提取问题内容（去掉@悦木小助手）
                String question = chatMessage.getContent().replace("@悦木小助手", "").trim();

                // 构建上下文信息
                if (chatMessage.getReplyId() != null) {
                    // 如果是回复消息，添加回复的上下文
                    ChatMessage replyMessage = chatMessageService.getById(chatMessage.getReplyId());
                    if (replyMessage != null) {
                        question = String.format("用户在回复这条消息：\"%s\"\n\n用户的问题是：%s",
                                replyMessage.getContent(), question);
                    }
                }

                // 保存用户的问题消息
                chatMessage.setCreateTime(new Date());
                chatMessageService.save(chatMessage);
                chatMessageService.fillMessageInfo(chatMessage);
                User sender = userService.getById(chatMessage.getSenderId());
                chatMessage.setSender(sender);
                sendToSpaceRoom(chatMessage);

                // 异步处理AI助手的回复
                handleAIAssistantMessageAsync(chatMessage, question, session);
            } else {
                // 处理普通消息
                chatMessage.setCreateTime(new Date());
                chatMessageService.save(chatMessage);
                chatMessageService.fillMessageInfo(chatMessage);
                User sender = userService.getById(chatMessage.getSenderId());
                chatMessage.setSender(sender);
                sendToSpaceRoom(chatMessage);
            }
        } catch (Exception e) {
            log.error("处理空间聊天消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 处理私聊消息
     */
    public void handlePrivateChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            User user = (User) session.getAttributes().get("user");

            // 先保存消息
            chatMessageService.save(chatMessage);

            // 使用原有的私聊处理逻辑（更新私聊记录）
            privateChatService.handlePrivateChatMessage(chatMessage, chatMessage.getPrivateChatId(), user);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 填充发送者信息
            chatMessage.setSender(user);

            // 发送消息
            sendToPrivateChat(chatMessage);
        } catch (BusinessException e) {
            log.error("处理私聊消息失败: {}", e.getMessage());
            sendErrorMessage(session, e.getMessage());
        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 异步处理AI助手消息
     */
    private void handleAIAssistantMessageAsync(ChatMessage userMessage, String question, WebSocketSession session) {
        try {
            // 将消息任务添加到队列
            AIMessageTask task = new AIMessageTask(userMessage, question, session);
            boolean added = aiMessageQueue.offer(task);
            if (!added) {
                // 如果队列已满，发送提示消息
                ChatMessage busyMessage = new ChatMessage();
                busyMessage.setSenderId(0L);
                busyMessage.setSpaceId(-2L);
                busyMessage.setType(3);
                busyMessage.setCreateTime(new Date());
                busyMessage.setReplyId(userMessage.getId());
                busyMessage.setRootId(userMessage.getRootId() != null ?
                        userMessage.getRootId() : userMessage.getId());
                busyMessage.setContent("抱歉，我现在有点忙，请稍后再问我问题 😊");

                User aiUser = new User();
                aiUser.setId(0L);
                aiUser.setUserName("悦木小助手");
                aiUser.setUserAccount("ai_assistant");
                aiUser.setUserAvatar(VoUrlReplaceUtil.replaceUrl("https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com//public/1866450683272450049/2025-05-26_AOTA5tKU2w6ci7hH.png"));
                busyMessage.setSender(aiUser);

                chatMessageService.save(busyMessage);
                chatMessageService.fillMessageInfo(busyMessage);
                sendToSpaceRoom(busyMessage);
            }
        } catch (Exception e) {
            log.error("添加AI消息任务到队列失败", e);
            try {
                sendErrorMessage(session, "AI助手消息处理失败");
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }
}
