package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 2048游戏记录VO
 */
@Data
public class Game2048RecordVO implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

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

    private static final long serialVersionUID = 1L;
}
