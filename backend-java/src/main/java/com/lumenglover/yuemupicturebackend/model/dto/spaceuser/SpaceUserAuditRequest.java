package com.lumenglover.yuemupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核空间成员请求
 */
@Data
public class SpaceUserAuditRequest implements Serializable {

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 被审核用户ID
     */
    private Long userId;

    /**
     * 审核结果：1-通过 2-拒绝
     */
    private Integer status;


    private static final long serialVersionUID = 1L;
}
