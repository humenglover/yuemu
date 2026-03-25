package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 聊天列表事件生产者
 */
@Component
@Slf4j
public class ChatListEventProducer {

    @Resource
    private Disruptor<ChatListEvent> chatListEventDisruptor;

    /**
     * 发布更新事件
     */
    public void publishUpdateEvent(PrivateChat privateChat, WebSocketSession session, User user) {
        RingBuffer<ChatListEvent> ringBuffer = chatListEventDisruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            ChatListEvent event = ringBuffer.get(sequence);
            event.setType("UPDATE");
            event.setPrivateChat(privateChat);
            event.setSession(session);
            event.setUser(user);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * 发布删除事件
     */
    public void publishDeleteEvent(Long privateChatId, WebSocketSession session, User user) {
        RingBuffer<ChatListEvent> ringBuffer = chatListEventDisruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            ChatListEvent event = ringBuffer.get(sequence);
            event.setType("DELETE");
            PrivateChat privateChat = new PrivateChat();
            privateChat.setId(privateChatId);
            event.setPrivateChat(privateChat);
            event.setSession(session);
            event.setUser(user);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * 发布新建事件
     */
    public void publishNewEvent(PrivateChat privateChat, WebSocketSession session, User user, Long targetUserId) {
        RingBuffer<ChatListEvent> ringBuffer = chatListEventDisruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            ChatListEvent event = ringBuffer.get(sequence);
            event.setType("NEW");
            event.setPrivateChat(privateChat);
            event.setSession(session);
            event.setUser(user);
            event.setTargetUserId(targetUserId);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    @PreDestroy
    public void destroy() {
        chatListEventDisruptor.shutdown();
    }
}
