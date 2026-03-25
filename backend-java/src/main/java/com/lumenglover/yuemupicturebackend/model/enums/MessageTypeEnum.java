package com.lumenglover.yuemupicturebackend.model.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum MessageTypeEnum {

    TEXT("文本消息", "text"),
    IMAGE("图片消息", "image"),
    AUDIO("音频消息", "audio"),
    VIDEO("视频消息", "video");

    private final String text;
    private final String value;

    MessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return TEXT;
        }
        for (MessageTypeEnum typeEnum : MessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return TEXT;
    }
}
