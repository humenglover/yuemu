package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

@Data
public class HotSearchVO {
    private String keyword;
    private Long count;
    private Long realTimeCount;
    private Double trend;
    private Double score;
    private String reason;
}
