package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 恋爱板音乐专栏
 */
@TableName(value = "love_board_music_album")
@Data
public class LoveBoardMusicAlbum implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 恋爱板ID
     */
    private Long loveBoardId;

    /**
     * 专栏名称
     */
    private String albumName;

    /**
     * 专栏封面URL
     */
    private String coverUrl;

    /**
     * 专栏描述
     */
    private String description;

    /**
     * 是否公开[0-私密，1-公开]
     */
    private Integer isPublic;

    /**
     * 专栏访问密码
     */
    private String password;

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
        this.coverUrl = VoUrlReplaceUtil.replaceUrl(this.coverUrl);
    }
}
