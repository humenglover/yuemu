package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户微信验证码登录请求
 */
@Data
public class UserWxLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 6 位验证码
     */
    private String code;
}
