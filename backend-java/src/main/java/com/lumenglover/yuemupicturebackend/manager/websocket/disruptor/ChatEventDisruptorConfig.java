package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 聊天事件 Disruptor 配置
 */
@Configuration
public class ChatEventDisruptorConfig {

    @Resource
    private ChatEventWorkHandler chatEventWorkHandler;

    @Bean("chatEventDisruptor")
    public Disruptor<ChatEvent> messageModelRingBuffer() {
        int bufferSize = 1024 * 256;
        Disruptor<ChatEvent> disruptor = new Disruptor<>(
                ChatEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("chatEventDisruptor").build()
        );
        disruptor.handleEventsWithWorkerPool(chatEventWorkHandler);
        disruptor.start();
        return disruptor;
    }
}
