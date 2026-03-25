package com.lumenglover.yuemupicturebackend.manager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * WebSocket 配置（定义连接）
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    @Resource
    private ChatWebSocketServer chatWebSocketServer;

    @Resource
    private ChatListWebSocketServer chatListWebSocketServer;

    @Resource
    private MessageWebSocketHandler messageWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 添加聊天WebSocket配置
        registry.addHandler(chatWebSocketServer, "/ws/chat")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 添加聊天列表WebSocket配置
        registry.addHandler(chatListWebSocketServer, "/ws/chat-list")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 添加消息WebSocket配置
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
