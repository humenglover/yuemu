package com.lumenglover.yuemupicturebackend.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报状态枚举
 */
public enum ReportStatusEnum {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理"),
    REJECTED(2, "驳回");

    private final int value;
    private final String text;

    ReportStatusEnum(int value, String text) {
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
        return Arrays.stream(values()).map(ReportStatusEnum::getValue).collect(Collectors.toList());
    }
}
