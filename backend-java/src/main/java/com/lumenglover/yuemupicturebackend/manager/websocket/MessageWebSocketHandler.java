package com.lumenglover.yuemupicturebackend.manager.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.MessageCenterVO;
import com.lumenglover.yuemupicturebackend.service.CommentsService;
import com.lumenglover.yuemupicturebackend.service.LikeRecordService;
import com.lumenglover.yuemupicturebackend.service.ShareRecordService;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
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

/**
 * 消息WebSocket处理器
 */
@Component
@Slf4j
public class MessageWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private CommentsService commentsService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private ShareRecordService shareRecordService;

    @Resource
    private SystemNotifyService systemNotifyService;

    // 存储所有活跃的WebSocket连接，key为用户ID
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 连接建立成功
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user != null) {
            sessions.put(user.getId().toString(), session);
            log.info("用户 {} 的消息WebSocket连接已建立", user.getUserName());

            // 连接建立后立即发送未读消息统计
            sendUnreadCountToUser(user.getId().toString());
        } else {
            log.error("无法获取用户信息，关闭WebSocket连接");
            session.close();
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user != null) {
            sessions.remove(user.getId().toString());
            log.info("用户 {} 的消息WebSocket连接已关闭", user.getUserName());
        }
    }

    /**
     * 处理文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
                case "REQUEST_UNREAD_COUNTS":
                    handleRequestUnreadCounts(session, user);
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
     * 处理获取未读消息总数请求
     */
    private void handleRequestUnreadCounts(WebSocketSession session, User user) {
        sendUnreadCountToUser(user.getId().toString());
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
     * 发送消息给指定用户
     */
    public void sendMessageToUser(String userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String messageStr = webSocketObjectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageStr));
            } catch (IOException e) {
                log.error("发送消息给用户 {} 失败", userId, e);
            }
        }
    }

    /**
     * 发送消息给所有用户
     */
    public void sendMessageToAllUsers(Object message) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    String messageStr = webSocketObjectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(messageStr));
                } catch (IOException e) {
                    log.error("发送消息给所有用户失败", e);
                }
            }
        }
    }

    /**
     * 发送未读消息统计给用户
     */
    public void sendUnreadCountToUser(String userId) {
        try {
            // 获取各类型未读数
            long unreadComments = commentsService.getUnreadCommentsCount(Long.valueOf(userId));
            long unreadLikes = likeRecordService.getUnreadLikesCount(Long.valueOf(userId));
            long unreadShares = shareRecordService.getUnreadSharesCount(Long.valueOf(userId));
            long unreadSystemNotifies = systemNotifyService.getUserUnreadCount(userId);

            // 构造消息中心VO
            MessageCenterVO messageCenterVO = new MessageCenterVO();
            messageCenterVO.setUnreadComments(unreadComments);
            messageCenterVO.setUnreadLikes(unreadLikes);
            messageCenterVO.setUnreadShares(unreadShares);
            messageCenterVO.setUnreadSystemNotifies(unreadSystemNotifies);
            messageCenterVO.setTotalUnread(unreadComments + unreadLikes + unreadShares + unreadSystemNotifies);

            // 构造响应消息（使用Java 8兼容的方式创建Map）
            Map<String, Object> response = new HashMap<>();
            response.put("type", "unread_counts_response");
            response.put("data", messageCenterVO);

            // 发送消息
            sendMessageToUser(userId, response);
        } catch (Exception e) {
            log.error("发送未读消息统计给用户 {} 失败", userId, e);
        }
    }
}
