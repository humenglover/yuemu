package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 安全的用户视图（脱敏，不包含邮箱等敏感信息）
 */
@Data
public class SafeUserVO implements Serializable {

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

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     *
     * @param user
     * @return
     */
    public static SafeUserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        SafeUserVO safeUserVO = new SafeUserVO();
        BeanUtils.copyProperties(user, safeUserVO);

        // 替换URL为自定义域名
        safeUserVO.setUserAvatar(VoUrlReplaceUtil.replaceUrl(safeUserVO.getUserAvatar()));

        return safeUserVO;
    }
}
