package com.lumenglover.yuemupicturebackend.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumenglover.yuemupicturebackend.common.PageRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @TableName comments
 */
@Data
public class CommentsVO extends PageRequest implements Serializable {
    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型（1-图片 2-帖子）
     */
    private Integer targetType;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 点踩数
     */
    private Long dislikeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 评论用户信息
     */
    private CommentUserVO commentUser;

    /**
     * 图片信息（当 targetType = 1 时）
     */
    private PictureVO picture;

    /**
     * 帖子信息（当 targetType = 2 时）
     */
    private Post post;

    /**
     * 子评论列表
     */
    private List<CommentsVO> children;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 评论位置（省份）
     */
    private String location;
}
