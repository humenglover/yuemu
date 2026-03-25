package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "hot_search")
@Data
public class HotSearch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索类型
     */
    private String type;

    /**
     * 搜索次数
     */
    private Long count;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

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
    @TableLogic
    private Integer isDelete;

    private Long realTimeCount;

    private Double trend;

    private static final long serialVersionUID = 1L;
}
