package com.lumenglover.yuemupicturebackend.model.dto.loveboard;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 恋爱画板管理查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoveBoardAdminRequest extends PageRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 男生昵称
     */
    private String manName;

    /**
     * 女生昵称
     */
    private String womanName;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
} 