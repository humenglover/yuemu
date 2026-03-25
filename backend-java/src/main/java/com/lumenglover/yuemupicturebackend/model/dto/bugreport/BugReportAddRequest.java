package com.lumenglover.yuemupicturebackend.model.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 * @author 鹿梦
 */
@Data
public class BugReportAddRequest implements Serializable {

    /**
     * bug标题
     */
    private String title;

    /**
     * 详细
     */
    private String description;

    /**
     * bug类型
     */
    private Integer bugType;

    /**
     * 截图URL数组(JSON格式)
     */
    private String screenshotUrls;

    /**
     * 出现问题的网站URL
     */
    private String websiteUrl;

    private static final long serialVersionUID = 1L;
}