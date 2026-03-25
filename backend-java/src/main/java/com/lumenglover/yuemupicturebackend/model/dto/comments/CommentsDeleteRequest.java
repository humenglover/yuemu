package com.lumenglover.yuemupicturebackend.model.dto.comments;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * @TableName comments
 */
@Data
public class CommentsDeleteRequest implements Serializable {
    /**
     *  评论id
     */
    private Long commentId;

    private static final long serialVersionUID = 1L;
}
