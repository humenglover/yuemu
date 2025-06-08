package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 2048游戏记录
 */
@TableName(value = "game_2048_record")
@Data
public class Game2048Record implements Serializable {

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
     * 得分
     */
    private Integer score;

    /**
     * 最大数字
     */
    private Integer maxTile;

    /**
     * 游戏时长(秒)
     */
    private Integer gameTime;

    /**
     * 移动次数
     */
    private Integer moveCount;

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
