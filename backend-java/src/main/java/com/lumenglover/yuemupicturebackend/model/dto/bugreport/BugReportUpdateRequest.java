package com.lumenglover.yuemupicturebackend.model.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
 * @author 鹿梦
 */
@Data
public class BugReportUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

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
     * 状态
     */
    private Integer status;

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