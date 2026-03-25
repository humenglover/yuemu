package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
@Data
public class FollowersAndFansVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 粉丝数量
     */
    private Long fansCount;
    /**
     * 关注数量
     */
    private Long followCount;
}
