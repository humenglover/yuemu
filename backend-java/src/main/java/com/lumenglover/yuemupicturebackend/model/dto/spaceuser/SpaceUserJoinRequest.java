package com.lumenglover.yuemupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请加入空间请求
 */
@Data
public class SpaceUserJoinRequest implements Serializable {

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
