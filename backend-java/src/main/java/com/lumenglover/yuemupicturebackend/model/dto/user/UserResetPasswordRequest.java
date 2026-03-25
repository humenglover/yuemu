package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 验证码
     */
    private String code;
}
