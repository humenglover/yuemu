package com.lumenglover.yuemupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间成员请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
