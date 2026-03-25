package com.lumenglover.yuemupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间请求
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

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
