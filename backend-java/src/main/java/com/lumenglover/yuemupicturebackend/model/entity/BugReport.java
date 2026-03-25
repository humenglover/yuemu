package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * bug报告表
 * @TableName bug_report
 */
@TableName(value = "bug_report")
@Data
public class BugReport implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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

    /**
     * 解决时间
     */
    private Date resolvedTime;

    /**
     * 解决方案或说明
     */
    private String resolution;

     /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final transient long serialVersionUID = 1L;
}
