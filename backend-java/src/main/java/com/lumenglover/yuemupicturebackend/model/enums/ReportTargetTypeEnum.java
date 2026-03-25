package com.lumenglover.yuemupicturebackend.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报目标类型枚举
 */
public enum ReportTargetTypeEnum {

    PICTURE(1, "图片"),
    POST(2, "帖子"),
    COMMENT(3, "评论"),
    USER(4, "用户"),
    OTHER(5, "其他");

    private final int value;
    private final String text;

    ReportTargetTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(ReportTargetTypeEnum::getValue).collect(Collectors.toList());
    }
}
