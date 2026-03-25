package com.lumenglover.yuemupicturebackend.manager.websocket.model;

import lombok.Getter;

/**
 * 聊天消息类型枚举
 */
@Getter
public enum ChatMessageTypeEnum {

    CHAT("发送聊天消息", "CHAT"),
    JOIN("加入聊天", "JOIN"),
    LEAVE("离开聊天", "LEAVE"),
    HISTORY("历史消息", "HISTORY"),
    ONLINE("在线用户", "ONLINE"),
    ERROR("错误消息", "ERROR"),
    RECALL("撤回消息", "RECALL"),
    HEARTBEAT("心跳消息", "HEARTBEAT");

    private final String text;
    private final String value;

    ChatMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChatMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (ChatMessageTypeEnum typeEnum : ChatMessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
