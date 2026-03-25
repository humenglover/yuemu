package com.lumenglover.yuemupicturebackend.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 举报视图对象
 */
@Data
public class ReportVO implements Serializable {

    /**
     * 举报ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 举报人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 举报人昵称
     */
    private String userName;

    /**
     * 举报人头像
     */
    private String userAvatar;

    /**
     * 被举报内容ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /**
     * 举报内容类型：1-图片 2-帖子 3-评论 4-用户 5-其他
     */
    private Integer targetType;

    /**
     * 举报内容类型文本
     */
    private String targetTypeText;

    /**
     * 举报类型：1-垃圾广告 2-违规内容 3-有害信息 4-人身攻击 5-侵犯隐私 6-版权问题 7-其他
     */
    private Integer reportType;

    /**
     * 举报类型文本
     */
    private String reportTypeText;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报截图URL列表
     */
    private List<String> screenshotUrls;

    /**
     * 处理状态：0-待处理 1-已处理 2-驳回
     */
    private Integer status;

    /**
     * 处理状态文本
     */
    private String statusText;

    /**
     * 处理人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long handlerId;

    /**
     * 处理人昵称
     */
    private String handlerName;

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
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
