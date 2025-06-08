package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 时光相册实体
 */
@TableName(value = "time_album")
@Data
public class TimeAlbum implements Serializable {

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
     * 相册名称
     */
    private String albumName;

    /**
     * 相册封面URL
     */
    private String coverUrl;

    /**
     * 相册描述
     */
    private String description;

    /**
     * 是否公开[0-私密，1-公开]
     */
    private Integer isPublic;

    /**
     * 相册访问密码
     */
    private String password;

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
