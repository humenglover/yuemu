package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 活动表
 */
@TableName(value = "activity")
@Data
public class Activity implements Serializable {
    /**
     * 活动ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发布用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 状态 0-待审核 1-已发布 2-已拒绝
     */
    private Integer status;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 活动过期时间
     */
    private Date expireTime;

    /**
     * 是否过期 0-未过期 1-已过期
     */
    private Integer isExpired;

    /**
     * 创建用户信息
     */
    @TableField(exist = false)
    private UserVO user;

    /**
     * 图片附件列表
     */
    @TableField(exist = false)
    private List<PostAttachment> attachments;

    /**
     * 是否已点赞 0-未点赞 1-已点赞
     */
    @TableField(exist = false)
    private Integer isLiked;

    /**
     * 是否已分享 0-未分享 1-已分享
     */
    @TableField(exist = false)
    private Integer isShared;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
