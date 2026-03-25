package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 帖子标签分类列表视图
 */
@Data
public class PostTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
