package com.lumenglover.yuemupicturebackend.model.dto.search;

import lombok.Data;

@Data
public class SearchRequest {
    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 搜索类型
     * picture - 图片搜索
     * user - 用户搜索
     */
    private String type;

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 页面大小
     */
    private Integer pageSize = 10;

    /**
     * 用户ID
     */
    private Long userId;
}
