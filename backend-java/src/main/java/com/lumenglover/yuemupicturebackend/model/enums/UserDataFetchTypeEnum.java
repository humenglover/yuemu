package com.lumenglover.yuemupicturebackend.model.enums;

import lombok.Getter;


/**
 * 用户数据获取类型枚举类
 */
@Getter
public enum UserDataFetchTypeEnum {
    RECOMMEND("推荐", 0),
    FOLLOW("关注", 1),
    RANKING("榜单", 2);


    private final String text;
    private final int value;


    UserDataFetchTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }


    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 要查找的枚举值
     * @return 对应的枚举实例，如果没找到则返回 null
     */
    public static UserDataFetchTypeEnum getEnumByValue(int value) {
        for (UserDataFetchTypeEnum userDataFetchTypeEnum : UserDataFetchTypeEnum.values()) {
            if (userDataFetchTypeEnum.value == value) {
                return userDataFetchTypeEnum;
            }
        }
        return null;
    }
}
