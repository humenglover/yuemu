package com.lumenglover.yuemupicturebackend.model.dto.comments;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 评论批量操作请求
 */
@Data
public class CommentsBatchRequest implements Serializable {

    /**
     * 评论 id 列表
     */
    private List<Long> ids;

    /**
     * 操作类型（"delete" - 删除, "restore" - 恢复）
     */
    private String operation;

    private static final long serialVersionUID = 1L;
}
