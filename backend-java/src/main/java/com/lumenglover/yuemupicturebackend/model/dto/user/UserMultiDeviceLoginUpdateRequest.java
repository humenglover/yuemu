package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户多设备登录设置更新请求
 *
 * @author 鹿梦
 * @date 2024/12/21
 */
@Data
public class UserMultiDeviceLoginUpdateRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否允许多设备登录：1-允许、0-禁止
     */
    private Integer allowMultiDeviceLogin;

    private static final long serialVersionUID = 1L;
}
