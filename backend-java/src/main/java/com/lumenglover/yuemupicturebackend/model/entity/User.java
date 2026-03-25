package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
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
     * 主页背景图URL
     */
    private String homepageBg;

    /**
     * 个性签名
     */
    private String personalSign;

    /**
     * 主题偏好
     */
    private String themePreference;

    /**
     * 内容可见性设置
     */
    private String visibilitySetting;

    /**
     * 兴趣领域
     */
    private String interestField;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    /**
     * 是否允许私聊：1-允许、0-禁止
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注：1-允许、0-禁止
     */
    private Integer allowFollow;

    /**
     * 是否展示关注列表：1-展示、0-隐藏
     */
    private Integer showFollowList;

    /**
     * 是否展示粉丝列表：1-展示、0-隐藏
     */
    private Integer showFansList;

    /**
     * 是否允许多设备登录：1-允许、0-禁止
     */
    private Integer allowMultiDeviceLogin;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 公众号 OpenId
     */
    private String mpOpenId;

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
        this.userAvatar = VoUrlReplaceUtil.replaceUrl(this.userAvatar);
        this.homepageBg = VoUrlReplaceUtil.replaceUrl(this.homepageBg);
    }
}
