package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 友情链接
 */
@TableName(value = "friend_link")
@Data
public class FriendLink implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 网站名称
     */
    private String siteName;

    /**
     * 网站链接
     */
    private String siteUrl;

    /**
     * 网站logo
     */
    private String siteLogo;

    /**
     * 网站描述
     */
    private String siteDesc;

    /**
     * 站长名称
     */
    private String ownerName;

    /**
     * 站长联系方式
     */
    private String ownerContact;

    /**
     * 申请用户id
     */
    private Long userId;

    /**
     * 网站类型 (例如: 个人博客、技术社区、资源网站等)
     */
    private String siteType;

    /**
     * 审核状态 (0-待审核 1-通过 2-拒绝)
     */
    private Integer status;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点击量
     */
    private Long clickCount;

    /**
     * 排序权重
     */
    private Integer weight;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 替换URL为自定义域名
     */
    public void replaceUrlWithCustomDomain() {
        this.siteUrl = VoUrlReplaceUtil.replaceUrl(this.siteUrl);
        this.siteLogo = VoUrlReplaceUtil.replaceUrl(this.siteLogo);
    }
}
