package com.lumenglover.yuemupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddRequest implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;
    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间简介
     */
    private String spaceDesc;

    /**
     * 空间封面图
     */
    private String spaceCover;

    private static final long serialVersionUID = 1L;
}
