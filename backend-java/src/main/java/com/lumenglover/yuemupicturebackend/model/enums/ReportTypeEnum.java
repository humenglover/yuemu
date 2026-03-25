package com.lumenglover.yuemupicturebackend.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报类型枚举
 */
public enum ReportTypeEnum {

    SPAM_ADVERTISING(1, "垃圾广告"),
    VIOLATION_CONTENT(2, "违规内容"),
    HARMFUL_INFORMATION(3, "有害信息"),
    PERSONAL_ATTACK(4, "人身攻击"),
    PRIVACY_VIOLATION(5, "侵犯隐私"),
    COPYRIGHT_ISSUE(6, "版权问题"),
    OTHER(7, "其他");

    private final int value;
    private final String text;

    ReportTypeEnum(int value, String text) {
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
        return Arrays.stream(values()).map(ReportTypeEnum::getValue).collect(Collectors.toList());
    }
}
