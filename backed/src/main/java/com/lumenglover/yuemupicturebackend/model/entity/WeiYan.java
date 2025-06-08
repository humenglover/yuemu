package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 微言表
 */
@TableName(value = "wei_yan")
@Data
public class WeiYan implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 恋爱板ID
     */
    private Long loveBoardId;

    /**
     * 发布用户ID
     */
    private Long userId;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 内容
     */
    private String content;

    /**
     * 类型
     */
    private String type;

    /**
     * 来源标识
     */
    private Long source;

    /**
     * 是否公开[0:仅自己可见，1:所有人可见]
     */
    private Integer isPublic;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
