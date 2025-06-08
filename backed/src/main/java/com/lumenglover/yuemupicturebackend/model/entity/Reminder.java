package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Date;

@TableName(value = "reminder")
@Data
public class Reminder implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String content;

    private LocalTime remindTime;

    private Integer completed;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    /**
     * 是否收藏 0-未收藏 1-已收藏
     */
    private Integer isStarred;

    /**
     * 是否重要 0-普通 1-重要
     */
    private Integer isImportant;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
