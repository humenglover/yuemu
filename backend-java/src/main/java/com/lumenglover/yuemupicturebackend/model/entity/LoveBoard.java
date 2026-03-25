package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 恋爱画板实体类
 */
@TableName(value = "love_board")
@Data
public class LoveBoard implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 背景封面
     */
    private String bgCover;

    /**
     * 男生头像
     */
    private String manCover;

    /**
     * 女生头像
     */
    private String womanCover;

    /**
     * 男生昵称
     */
    private String manName;

    /**
     * 女生昵称
     */
    private String womanName;

    /**
     * 计时
     */
    private String timing;

    /**
     * 倒计时标题
     */
    private String countdownTitle;

    /**
     * 倒计时时间
     */
    private String countdownTime;

    /**
     * 是否启用[0:否，1:是]
     */
    private Integer status;

    /**
     * 额外信息
     */
    private String familyInfo;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 浏览数
     */
    private Long viewCount;

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
        this.bgCover = VoUrlReplaceUtil.replaceUrl(this.bgCover);
        this.manCover = VoUrlReplaceUtil.replaceUrl(this.manCover);
        this.womanCover = VoUrlReplaceUtil.replaceUrl(this.womanCover);
    }
}
