package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 聊天事件生产者
 */
@Component
@Slf4j
public class ChatEventProducer {

    @Resource
    private Disruptor<ChatEvent> chatEventDisruptor;

    public void publishEvent(ChatMessage chatMessage, WebSocketSession session,
            User user, Long targetId, Integer targetType) {
        RingBuffer<ChatEvent> ringBuffer = chatEventDisruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            ChatEvent event = ringBuffer.get(sequence);
            event.setChatMessage(chatMessage);
            event.setSession(session);
            event.setUser(user);
            event.setTargetId(targetId);
            event.setTargetType(targetType);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    @PreDestroy
    public void destroy() {
        chatEventDisruptor.shutdown();
    }
}
