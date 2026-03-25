package com.lumenglover.yuemupicturebackend.model.dto.post;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryRequest extends PageRequest implements Serializable {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 分类
     */
    private String category;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 审核状态（0-待审核, 1-已发布, 2-拒绝）
     */
    private Integer status;

    /**
     * 是否只查询公共数据（默认true）
     */
    private Boolean isPublic = true;

    private static final long serialVersionUID = 1L;
}
