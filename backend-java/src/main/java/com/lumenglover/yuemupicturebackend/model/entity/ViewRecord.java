package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 浏览记录表
 * @TableName view_record
 */
@TableName(value = "view_record")
@Data
public class ViewRecord implements Serializable {
    /**
     * 浏览记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被浏览内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间 4-用户
     */
    private Integer targetType;

    /**
     * 浏览时间
     */
    /**
     * 浏览时长(秒)
     */
    private Integer viewDuration;



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
}