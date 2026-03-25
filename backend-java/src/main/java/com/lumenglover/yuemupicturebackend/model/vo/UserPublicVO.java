package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户公开信息视图（不包含邮箱、密码等敏感信息）
 */
@Data
public class UserPublicVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 性别
     */
    private String gender;

    /**
     * 地区
     */
    private String region;

    /**
     * 生日
     */
    private String birthday;

    /**
     * 用户标签
     */
    private String userTags;

    /**
     * 主页背景图
     */
    private String homepageBg;

    /**
     * 个性签名
     */
    private String personalSign;

    /**
     * 是否允许私聊
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注
     */
    private Integer allowFollow;

    /**
     * 是否展示关注列表
     */
    private Integer showFollowList;

    /**
     * 是否展示粉丝列表
     */
    private Integer showFansList;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     *
     * @param user
     * @return
     */
    public static UserPublicVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserPublicVO userPublicVO = new UserPublicVO();
        BeanUtils.copyProperties(user, userPublicVO);

        // 替换URL为自定义域名
        userPublicVO.setUserAvatar(VoUrlReplaceUtil.replaceUrl(userPublicVO.getUserAvatar()));
        userPublicVO.setHomepageBg(VoUrlReplaceUtil.replaceUrl(userPublicVO.getHomepageBg()));

        // 复制权限字段
        userPublicVO.setAllowPrivateChat(user.getAllowPrivateChat());
        userPublicVO.setAllowFollow(user.getAllowFollow());
        userPublicVO.setShowFollowList(user.getShowFollowList());
        userPublicVO.setShowFansList(user.getShowFansList());

        return userPublicVO;
    }
}
