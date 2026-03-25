package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户安全注销请求 DTO
 *
 * @author 鹿梦
 * @date 2026/1/17
 */
@Data
public class UserDestroySecureRequest implements Serializable {

    /**
     * 用户当前密码
     */
    private String userPassword;

    /**
     * 邮箱验证码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
