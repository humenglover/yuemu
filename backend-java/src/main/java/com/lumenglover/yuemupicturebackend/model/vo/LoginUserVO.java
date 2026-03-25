package com.lumenglover.yuemupicturebackend.model.vo;

import cn.hutool.core.util.StrUtil;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 已登录用户视图（脱敏）
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * id
     */
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
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

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
    private Date birthday;

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
     * 是否绑定微信
     */
    private Boolean hasBindWx;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     *
     * @param user
     * @return
     */
    public static LoginUserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);

        // 设置微信绑定状态
        loginUserVO.setHasBindWx(StrUtil.isNotBlank(user.getMpOpenId()));

        // 替换URL为自定义域名
        loginUserVO.setUserAvatar(VoUrlReplaceUtil.replaceUrl(loginUserVO.getUserAvatar()));
        loginUserVO.setHomepageBg(VoUrlReplaceUtil.replaceUrl(loginUserVO.getHomepageBg()));

        return loginUserVO;
    }
}
