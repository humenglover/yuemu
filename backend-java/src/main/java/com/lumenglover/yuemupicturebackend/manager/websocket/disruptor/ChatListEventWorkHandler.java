package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.lumenglover.yuemupicturebackend.manager.websocket.ChatListWebSocketServer;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.service.PrivateChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天列表事件处理器
 */
@Component
@Slf4j
public class ChatListEventWorkHandler implements WorkHandler<ChatListEvent> {

    @Resource
    @Lazy
    private ChatListWebSocketServer chatListWebSocketServer;

    @Resource
    private PrivateChatService privateChatService;

    @Override
    public void onEvent(ChatListEvent event) {
        try {
            switch (event.getType()) {
                case "UPDATE":
                    handleUpdateEvent(event);
                    break;
                case "DELETE":
                    handleDeleteEvent(event);
                    break;
                case "NEW":
                    handleNewEvent(event);
                    break;
                default:
                    log.error("Unknown event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("处理聊天列表事件失败", e);
        } finally {
            event.clear();
        }
    }

    /**
     * 处理更新事件
     */
    private void handleUpdateEvent(ChatListEvent event) throws Exception {
        PrivateChat privateChat = event.getPrivateChat();

        // 构建响应消息
        Map<String, Object> response = new HashMap<>();
        response.put("type", "UPDATE");
        response.put("data", privateChat);

        // 发送给相关用户
        String message = JSONUtil.toJsonStr(response);
        chatListWebSocketServer.sendToUser(privateChat.getUserId(), message);
        if (privateChat.getTargetUserId() != null) {
            chatListWebSocketServer.sendToUser(privateChat.getTargetUserId(), message);
        }
    }

    /**
     * 处理删除事件
     */
    private void handleDeleteEvent(ChatListEvent event) throws Exception {
        PrivateChat privateChat = event.getPrivateChat();

        // 构建响应消息
        Map<String, Object> response = new HashMap<>();
        response.put("type", "DELETE");
        response.put("privateChatId", privateChat.getId());

        // 发送给相关用户
        String message = JSONUtil.toJsonStr(response);
        chatListWebSocketServer.sendToUser(privateChat.getUserId(), message);
        if (privateChat.getTargetUserId() != null) {
            chatListWebSocketServer.sendToUser(privateChat.getTargetUserId(), message);
        }
    }

    /**
     * 处理新建事件
     */
    private void handleNewEvent(ChatListEvent event) throws Exception {
        PrivateChat privateChat = event.getPrivateChat();
        Long targetUserId = event.getTargetUserId();

        // 构建响应消息
        Map<String, Object> response = new HashMap<>();
        response.put("type", "NEW");
        response.put("data", privateChat);

        // 发送给相关用户
        String message = JSONUtil.toJsonStr(response);
        chatListWebSocketServer.sendToUser(privateChat.getUserId(), message);
        if (targetUserId != null) {
            chatListWebSocketServer.sendToUser(targetUserId, message);
        }
    }
}
