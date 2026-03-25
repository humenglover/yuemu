package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 *
 * @TableName userfollows
 */
@TableName(value ="userfollows")
@Data
public class Userfollows implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long followId;

    /**
     * 关注者的用户 ID
     */
    private Long followerId;

    /**
     * 被关注者的用户 ID
     */
    private Long followingId;

    /**
     * 关注状态，0 表示取消关注，1 表示关注
     */
    private Integer followStatus;

    /**
     * 是否为双向关注，0 表示单向，1 表示双向
     */
    private Integer isMutual;


    /**
     * 最后交互时间
     */
    private Date lastInteractionTime;

    /**
     * 关注关系创建时间，默认为当前时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 关注关系编辑时间，默认为当前时间
     */
    private Date editTime;

    /**
     * 关注关系更新时间，更新时自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除，0 表示未删除，1 表示已删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 3L;
}
