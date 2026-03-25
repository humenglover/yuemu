package com.lumenglover.yuemupicturebackend.model.dto.comments;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理员评论查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentsAdminRequest extends PageRequest implements Serializable {
    /**
     * 评论id
     */
    private Long commentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 是否删除
     */
    private Integer isDelete;

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
