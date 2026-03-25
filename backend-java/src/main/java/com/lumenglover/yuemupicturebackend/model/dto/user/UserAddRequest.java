package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户创建请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    /**
     * 用户性别
     */
    private String gender;

    /**
     * 所在地区
     */
    private String region;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 个人标签
     */
    private String userTags;

    /**
     * 个性签名
     */
    private String personalSign;

    /**
     * 兴趣领域
     */
    private String interestField;

    /**
     * 主页背景图URL
     */
    private String homepageBg;

    /**
     * 主题偏好
     */
    private String themePreference;

    /**
     * 内容可见性设置
     */
    private String visibilitySetting;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    private static final long serialVersionUID = 12L;
}
