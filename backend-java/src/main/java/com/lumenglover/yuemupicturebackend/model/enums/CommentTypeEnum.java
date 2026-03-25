package com.lumenglover.yuemupicturebackend.model.enums;

import lombok.Getter;

/**
 * 评论类型枚举类
 */
@Getter
public enum CommentTypeEnum {
    TOP_LEVEL_COMMENT("顶级评论", 0),
    SUB_LEVEL_COMMENT("子级评论", 1);

    private final String text;
    private final int value;

    CommentTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 要查找的枚举值
     * @return 对应的枚举实例，如果没找到则返回 null
     */
    public static CommentTypeEnum getEnumByValue(int value) {
        for (CommentTypeEnum commentTypeEnum : CommentTypeEnum.values()) {
            if (commentTypeEnum.value == value) {
                return commentTypeEnum;
            }
        }
        return null;
    }
}
