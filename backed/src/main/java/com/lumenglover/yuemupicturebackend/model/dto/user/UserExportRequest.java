package com.lumenglover.yuemupicturebackend.model.dto.user;

import lombok.Data;

import java.util.Date;

@Data
public class UserExportRequest {
    /**
     * 导出类型：1-天 2-周 3-月 4-年 5-自定义
     */
    private Integer type;

    /**
     * 自定义开始时间
     */
    private Date startTime;

    /**
     * 自定义结束时间
     */
    private Date endTime;
}
