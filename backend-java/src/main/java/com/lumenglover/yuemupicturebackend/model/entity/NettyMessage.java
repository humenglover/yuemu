package com.lumenglover.yuemupicturebackend.model.entity;

import lombok.Data;

/**
 * Netty聊天测试消息类
 */
@Data
public class NettyMessage {

    /**
     * 发送者用户ID
     */
    private Long userId;

    /**
     * 接收者用户ID（私聊时使用）
     */
    private Long toUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（PRIVATE-私聊消息，BROADCAST-广播消息）
     */
    private String type;

    /**
     * 消息发送时间戳
     */
    private Long timestamp;
}
