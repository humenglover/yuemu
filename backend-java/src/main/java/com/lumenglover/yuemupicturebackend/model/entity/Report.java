package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 举报表
 * @TableName report
 */
@TableName(value = "report")
@Data
public class Report implements Serializable {
    /**
     * 举报ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 举报人ID
     */
    private Long userId;

    /**
     * 被举报内容ID
     */
    private Long targetId;

    /**
     * 举报内容类型：1-图片 2-帖子 3-评论 4-用户 5-其他
     */
    private Integer targetType;

    /**
     * 举报类型：1-垃圾广告 2-违规内容 3-有害信息 4-人身攻击 5-侵犯隐私 6-版权问题 7-其他
     */
    private Integer reportType;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报截图URL（JSON数组）
     */
    private String screenshotUrls;

    /**
     * 处理状态：0-待处理 1-已处理 2-驳回
     */
    private Integer status;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 处理时间
     */
    private Date handleTime;

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
    private static final long serialVersionUID = 1L;
}
