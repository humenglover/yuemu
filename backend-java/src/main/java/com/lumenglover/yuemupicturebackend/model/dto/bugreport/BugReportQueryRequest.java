package com.lumenglover.yuemupicturebackend.model.dto.bugreport;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author 鹿梦
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugReportQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * bug标题
     */
    private String title;

    /**
     * bug类型
     */
    private Integer bugType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 出现问题的网站URL
     */
    private String websiteUrl;

    private static final long serialVersionUID = 1L;
}