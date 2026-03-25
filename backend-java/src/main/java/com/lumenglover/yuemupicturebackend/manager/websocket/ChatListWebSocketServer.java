package com.lumenglover.yuemupicturebackend.manager.websocket;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenglover.yuemupicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.PrivateChatService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatListWebSocketServer extends TextWebSocketHandler {

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private UserService userService;

    @Resource
    private PrivateChatService privateChatService;

    /**
     * 用户ID -> WebSocket session
     */
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            // 从session中获取用户信息
            User user = (User) session.getAttributes().get("user");
            if (user != null) {
                userSessions.put(user.getId(), session);
                log.info("用户 {} 的聊天列表WebSocket连接已建立, 会话ID: {}", user.getUserName(), session.getId());
            } else {
                log.error("无法获取用户信息，关闭WebSocket连接");
                session.close();
            }
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            User user = (User) session.getAttributes().get("user");
            if (user != null) {
                userSessions.remove(user.getId());
                log.info("用户 {} 的聊天列表WebSocket连接已关闭, 会话ID: {}", user.getUserName(), session.getId());
            }
        } catch (Exception e) {
            log.error("WebSocket连接关闭处理失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            // 解析消息
            Map<String, Object> messageMap = webSocketObjectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) messageMap.get("type");

            // 获取用户信息
            User user = (User) session.getAttributes().get("user");
            if (user == null) {
                sendErrorMessage(session, "用户未登录");
                return;
            }

            // 处理不同类型的消息
            switch (type) {
                case "REQUEST_LIST":
                    handleRequestList(session, messageMap, user);
                    break;
                case "REQUEST_UNREAD_COUNTS":
                    handleRequestUnreadCounts(session, user);
                    break;
                case "CLEAR_ALL_UNREAD":
                    handleClearAllUnread(session, user);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(session, user);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendErrorMessage(session, "处理消息失败");
        }
    }

    /**
     * 处理获取聊天列表请求
     */
    private void handleRequestList(WebSocketSession session, Map<String, Object> messageMap, User user) {
        try {
            // 从消息中获取查询参数
            Map<String, Object> data = (Map<String, Object>) messageMap.get("data");
            if (data == null) {
                sendErrorMessage(session, "请求参数错误");
                return;
            }

            // 构建查询请求对象
            PrivateChatQueryRequest queryRequest = new PrivateChatQueryRequest();

            // 设置分页参数
            Number current = (Number) data.get("current");
            Number pageSize = (Number) data.get("pageSize");
            queryRequest.setCurrent(current != null ? (int) current.longValue() : 1);
            queryRequest.setPageSize(pageSize != null ? (int) pageSize.longValue() : 20);

            // 设置其他查询参数
            if (data.containsKey("searchText")) {
                queryRequest.setSearchText((String) data.get("searchText"));
            }
            if (data.containsKey("chatType")) {
                Object chatType = data.get("chatType");
                if (chatType != null) {
                    queryRequest.setChatType(Integer.valueOf(chatType.toString()));
                }
            }
            if (data.containsKey("targetUserId")) {
                Object targetUserId = data.get("targetUserId");
                if (targetUserId != null) {
                    queryRequest.setTargetUserId(Long.valueOf(targetUserId.toString()));
                }
            }

            // 限制每页大小
            if (queryRequest.getPageSize() > 20) {
                queryRequest.setPageSize(20);
            }

            // 使用PrivateChatService执行查询
            Page<PrivateChat> privateChatPage = privateChatService.page(
                    new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                    privateChatService.getQueryWrapper(queryRequest, user),
                    user
            );

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_LIST");
            response.put("records", privateChatPage.getRecords());
            response.put("total", privateChatPage.getTotal());
            response.put("current", privateChatPage.getCurrent());
            response.put("size", privateChatPage.getSize());

            // 发送响应
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));

        } catch (Exception e) {
            log.error("获取聊天列表失败", e);
            sendErrorMessage(session, "获取聊天列表失败");
        }
    }

    /**
     * 处理获取未读消息总数请求
     */
    private void handleRequestUnreadCounts(WebSocketSession session, User user) {
        try {
            // 获取私聊未读消息总数
            int totalPrivateUnread = privateChatService.getTotalUnreadCount(user.getId());
            int friendUnreadCount = privateChatService.getFriendUnreadCount(user.getId());
            int normalUnreadCount = totalPrivateUnread - friendUnreadCount;

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "UNREAD_COUNTS");
            response.put("totalUnread", totalPrivateUnread);
            response.put("privateUnread", normalUnreadCount);
            response.put("friendUnread", friendUnreadCount);

            // 发送响应
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.error("获取未读消息总数失败", e);
            sendErrorMessage(session, "获取未读消息总数失败");
        }
    }

    /**
     * 处理清除所有未读消息请求
     */
    private void handleClearAllUnread(WebSocketSession session, User user) {
        try {
            // 清除该用户的所有未读消息
            privateChatService.clearAllUnreadCount(user.getId());

            // 构建清除未读消息的响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CLEAR_ALL_UNREAD_RESPONSE");
            response.put("success", true);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));

            // 广播更新聊天列表
            Set<WebSocketSession> sessions = userSessions.values().stream()
                    .filter(s -> s.isOpen())
                    .collect(Collectors.toSet());

            for (WebSocketSession s : sessions) {
                User sessionUser = (User) s.getAttributes().get("user");
                if (sessionUser != null && sessionUser.getId().equals(user.getId())) {
                    // 通知用户更新聊天列表
                    notifyUpdateChatList(sessionUser.getId());
                    // 通知用户更新未读消息总数
                    notifyUpdateUnreadCounts(sessionUser.getId());
                }
            }
        } catch (Exception e) {
            log.error("清除所有未读消息失败", e);
            sendErrorMessage(session, "清除所有未读消息失败");
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(WebSocketSession session, User user) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "HEARTBEAT");
        session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        log.debug("用户 {} 心跳响应已发送", user.getUserName());
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "ERROR");
            response.put("message", errorMessage);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息到用户 {} 失败", userId, e);
            }
        }
    }

    /**
     * 通知用户更新聊天列表
     */
    public void notifyUpdateChatList(Long userId) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "UPDATE_LIST");
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
            }
        } catch (IOException e) {
            log.error("通知用户 {} 更新聊天列表失败", userId, e);
        }
    }

    /**
     * 通知用户更新特定聊天的未读消息数
     */
    public void notifyUpdateUnreadCount(Long userId, Long privateChatId, Integer unreadCount) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "UPDATE_UNREAD");
                response.put("privateChatId", privateChatId);
                response.put("unreadCount", unreadCount);
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
            }
        } catch (IOException e) {
            log.error("通知用户 {} 更新未读消息数失败", userId, e);
        }
    }

    /**
     * 通知用户更新特定聊天的名称
     */
    public void notifyUpdateChatName(Long userId, Long privateChatId, String chatName) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "UPDATE_CHAT_NAME");
                response.put("privateChatId", privateChatId);
                response.put("chatName", chatName);
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
            }
        } catch (IOException e) {
            log.error("通知用户 {} 更新聊天名称失败", userId, e);
        }
    }

    /**
     * 通知用户删除特定聊天
     */
    public void notifyDeleteChat(Long userId, Long privateChatId) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "DELETE_CHAT");
                response.put("privateChatId", privateChatId);
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
            }
        } catch (IOException e) {
            log.error("通知用户 {} 删除聊天失败", userId, e);
        }
    }

    /**
     * 通知用户更新未读消息总数
     */
    public void notifyUpdateUnreadCounts(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            User user = (User) session.getAttributes().get("user");
            if (user != null) {
                handleRequestUnreadCounts(session, user);
            }
        }
    }
}
