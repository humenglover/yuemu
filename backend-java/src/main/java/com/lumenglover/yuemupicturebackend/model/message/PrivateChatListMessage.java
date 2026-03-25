package com.lumenglover.yuemupicturebackend.model.message;

import lombok.Data;

@Data
public class PrivateChatListMessage {
    /**
     * 消息类型
     */
    private String type;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息数据
     */
    private Object data;
}
