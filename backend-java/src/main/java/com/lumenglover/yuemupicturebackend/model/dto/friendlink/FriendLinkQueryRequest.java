package com.lumenglover.yuemupicturebackend.model.dto.friendlink;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class FriendLinkQueryRequest extends PageRequest implements Serializable {
    /**
     * 网站名称（模糊搜索）
     */
    private String siteName;

    /**
     * 网站类型
     */
    private String siteType;

    /**
     * 审核状态（管理员可用）
     */
    private Integer status;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
} 