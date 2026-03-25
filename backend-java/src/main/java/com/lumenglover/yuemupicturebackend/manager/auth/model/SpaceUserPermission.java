package com.lumenglover.yuemupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员权限
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}
