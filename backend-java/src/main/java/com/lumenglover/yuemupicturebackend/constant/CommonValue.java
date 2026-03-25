package com.lumenglover.yuemupicturebackend.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 通用常量定义
 */
@Component
public class CommonValue {

    public static String DEFAULT_USER_NAME;

    public static String DEFAULT_PASSWORD;

    /**
     * 加密盐
     */
    public static String DEFAULT_SALT;

    @Value("${common.default-user-name:无名}")
    public void setDefaultUserName(String defaultUserName) {
        CommonValue.DEFAULT_USER_NAME = defaultUserName;
    }

    @Value("${common.default-password:12345678}")
    public void setDefaultPassword(String defaultPassword) {
        CommonValue.DEFAULT_PASSWORD = defaultPassword;
    }

    @Value("${common.default-salt:lumeng}")
    public void setDefaultSalt(String defaultSalt) {
        CommonValue.DEFAULT_SALT = defaultSalt;
    }
}
