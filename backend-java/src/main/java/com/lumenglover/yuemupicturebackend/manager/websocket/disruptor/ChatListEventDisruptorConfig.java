package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 聊天列表事件 Disruptor 配置
 */
@Configuration
public class ChatListEventDisruptorConfig {

    @Resource
    private ChatListEventWorkHandler chatListEventWorkHandler;

    @Bean("chatListEventDisruptor")
    public Disruptor<ChatListEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<ChatListEvent> disruptor = new Disruptor<>(
                ChatListEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("chatListEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(chatListEventWorkHandler);
        // 启动 disruptor
        disruptor.start();
        return disruptor;
    }
}
