package com.lumenglover.yuemupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 操作枚举类
 */
@Getter
public enum OperationEnum {

    DELETE("删除", 0),
    APPROVE("通过", 1),
    REJECT("拒绝", 2);

    private final String text;
    private final int value;

    OperationEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 要查找的枚举值
     * @return 对应的枚举实例，如果没找到则返回 null
     */
    public static OperationEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OperationEnum operationEnum : OperationEnum.values()) {
            if (operationEnum.value == value) {
                return operationEnum;
            }
        }
        return null;
    }
}
