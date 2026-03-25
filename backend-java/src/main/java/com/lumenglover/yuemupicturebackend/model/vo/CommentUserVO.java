package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommentUserVO implements Serializable {
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
     * 对象转封装类
     *
     * @param user
     * @return
     */
    public static CommentUserVO objToVo(com.lumenglover.yuemupicturebackend.model.entity.User user) {
        if (user == null) {
            return null;
        }
        CommentUserVO commentUserVO = new CommentUserVO();
        commentUserVO.setId(user.getId());
        commentUserVO.setUserAccount(user.getUserAccount());
        commentUserVO.setUserName(user.getUserName());

        // 替换URL为自定义域名
        commentUserVO.setUserAvatar(VoUrlReplaceUtil.replaceUrl(user.getUserAvatar()));

        return commentUserVO;
    }
}
