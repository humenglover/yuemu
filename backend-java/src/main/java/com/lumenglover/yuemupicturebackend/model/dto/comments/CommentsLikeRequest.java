package com.lumenglover.yuemupicturebackend.model.dto.comments;


import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论查询请求
 */
@Data
public class CommentsLikeRequest  implements Serializable {

    /**
     *  评论id
     */
    private Long commentId;

    /**
     *  用户id
     */
    private Long userId;


    /**
     *   点赞评论内容
     */
    private Long likeCount;

    /**
     *  踩评论
     */
    private Long dislikeCount;

    private static final long serialVersionUID = 1L;
}
