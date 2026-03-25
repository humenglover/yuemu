package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * 账号或邮箱
     */
    private String accountOrEmail;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 验证码
     */
    private String verifyCode;

    /**
     * 验证码ID
     */
    private String serververifycode;
}
