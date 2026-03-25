package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户绑定微信请求
 */
@Data
public class UserWxBindRequest implements Serializable {

    /**
     * 微信验证码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
