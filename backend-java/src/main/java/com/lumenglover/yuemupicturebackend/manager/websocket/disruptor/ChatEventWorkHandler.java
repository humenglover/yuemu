package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.lumenglover.yuemupicturebackend.manager.websocket.ChatWebSocketServer;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

/**
 * 聊天事件处理器
 */
@Component
@Slf4j
public class ChatEventWorkHandler implements WorkHandler<ChatEvent> {

    @Resource
    @Lazy
    private ChatWebSocketServer chatWebSocketServer;

    @Override
    public void onEvent(ChatEvent event) {
        try {
            ChatMessage chatMessage = event.getChatMessage();
            // 确保消息的目标ID已设置
            switch (event.getTargetType()) {
                case 1: // 私聊
                    chatMessage.setPrivateChatId(event.getTargetId());
                    chatWebSocketServer.handlePrivateChatMessage(chatMessage, event.getSession());
                    break;
                case 2: // 图片聊天室
                    chatMessage.setPictureId(event.getTargetId());
                    chatWebSocketServer.handlePictureChatMessage(chatMessage, event.getSession());
                    break;
                case 3: // 空间聊天
                    chatMessage.setSpaceId(event.getTargetId());
                    chatWebSocketServer.handleSpaceChatMessage(chatMessage, event.getSession());
                    break;
                default:
                    log.error("Unknown target type: {}", event.getTargetType());
            }
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
        } finally {
            event.clear(); // 清空事件数据
        }
    }
}
