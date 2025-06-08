package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 祝福板实体
 */
@Data
@TableName("message_board")
public class MessageBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 主人id
     */
    private Long ownerId;

    /**
     * 留言用户ID
     */
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 留言内容
     */
    private String content;

    /**
     * QQ号
     */
    private String qq;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 浏览器信息
     */
    private String browser;

    /**
     * 操作系统信息
     */
    private String os;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 状态 0-隐藏 1-显示
     */
    private Integer status;

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
}
