package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


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
     * 空间ID
     */
    private Long spaceId;

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
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
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

    /**
     * 替换URL为自定义域名
     */
    public void replaceUrlWithCustomDomain() {
        this.coverUrl = VoUrlReplaceUtil.replaceUrl(this.coverUrl);
    }

    /**
     * 检查活动是否过期
     *
     * @return true if expired, false otherwise
     */
    public boolean isActivityExpired() {
        if (this.expireTime == null) {
            return false; // 如果没有设置过期时间，则认为不过期
        }
        return this.expireTime.before(new Date());
    }
}
