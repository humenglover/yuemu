package com.lumenglover.yuemupicturebackend.model.dto.comments;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName comments
 */
@Data
public class CommentsAddRequest implements Serializable {

    /**
     *  用户id
     */
    private Long userId;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子，默认为1(图片)
     */
    private Integer targetType = 1;

    /**
     *内容
     */
    private String content;

    /**
     *父类
     */
    private Long parentCommentId;
}
