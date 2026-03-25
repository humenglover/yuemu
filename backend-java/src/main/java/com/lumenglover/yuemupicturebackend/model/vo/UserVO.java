package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {

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
     * 邮箱
     */
    private String email;

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
     * 粉丝数量
     */
    private Long fansCount;

    /**
     * 关注数量
     */
    private Long followCount;

    /**
     * 主页背景图
     */
    private String homepageBg;

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

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     *
     * @param user
     * @return
     */
    public static UserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 替换URL为自定义域名
        userVO.setUserAvatar(VoUrlReplaceUtil.replaceUrl(userVO.getUserAvatar()));
        userVO.setHomepageBg(VoUrlReplaceUtil.replaceUrl(userVO.getHomepageBg()));

        return userVO;
    }
}
