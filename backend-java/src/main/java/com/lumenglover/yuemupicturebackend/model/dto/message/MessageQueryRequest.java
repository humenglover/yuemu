package com.lumenglover.yuemupicturebackend.model.dto.message;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 留言查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageQueryRequest extends PageRequest {
    /**
     * 留言内容
     */
    private String content;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder;
}
