package com.lumenglover.yuemupicturebackend.model.dto.user;


import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
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

    private static final long serialVersionUID = 1L;
}
