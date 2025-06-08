package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "app_version")
@Data
public class AppVersion implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版本号
     */
    private String version;

    /**
     * 版本码
     */
    private Integer versionCode;

    /**
     * APK文件路径
     */
    private String apkPath;

    /**
     * APK文件大小
     */
    private Long apkSize;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 是否强制更新
     */
    private Integer isForce;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

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
