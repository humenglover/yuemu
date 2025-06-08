package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

@Data
public class UserHighestScoreVO {
    /**
     * 经典模式最高分
     */
    private Integer classicModeScore;

    /**
     * 无墙模式最高分
     */
    private Integer noWallModeScore;

    /**
     * 竞速模式最高分
     */
    private Integer speedModeScore;
}
