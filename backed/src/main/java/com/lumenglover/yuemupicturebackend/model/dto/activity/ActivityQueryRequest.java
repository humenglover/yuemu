package com.lumenglover.yuemupicturebackend.model.dto.activity;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 活动查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ActivityQueryRequest extends PageRequest {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否只看未过期
     */
    private Boolean notExpired;

    /**
     * 是否公开查询（不需要管理员权限）
     */
    private Boolean isPublic;
}
