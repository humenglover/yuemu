package com.lumenglover.yuemupicturebackend.model.dto.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员查询留言请求
 */
@Data
public class MessageAdminRequest implements Serializable {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 8;

    /**
     * 留言ID
     */
    private Long id;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 留言内容
     */
    private String content;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";

    private static final long serialVersionUID = 1L;
}
