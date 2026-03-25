package com.lumenglover.yuemupicturebackend.model.dto.report;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新举报请求（主要用于管理员处理举报）
 */
@Data
public class ReportUpdateRequest implements Serializable {

    /**
     * 举报ID
     */
    private Long id;

    /**
     * 处理状态：0-待处理 1-已处理 2-驳回
     */
    private Integer status;

    /**
     * 处理结果
     */
    private String handleResult;

    private static final long serialVersionUID = 1L;
}