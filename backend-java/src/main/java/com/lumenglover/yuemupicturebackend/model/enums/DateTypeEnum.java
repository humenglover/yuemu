package com.lumenglover.yuemupicturebackend.model.enums;

import lombok.Getter;


/**
 * 日期类型枚举类
 */
@Getter
public enum DateTypeEnum {
    YEAR("年", 0),
    MONTH("月", 1),
    DAY("日", 2);


    private final String text;
    private final int value;


    DateTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }


    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 要查找的枚举值
     * @return 对应的枚举实例，如果没找到则返回 null
     */
    public static DateTypeEnum getEnumByValue(int value) {
        for (DateTypeEnum dateTypeEnum : DateTypeEnum.values()) {
            if (dateTypeEnum.value == value) {
                return dateTypeEnum;
            }
        }
        return null;
    }
}
