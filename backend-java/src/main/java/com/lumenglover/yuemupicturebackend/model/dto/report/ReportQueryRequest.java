package com.lumenglover.yuemupicturebackend.model.dto.report;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 举报查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportQueryRequest extends PageRequest implements Serializable {

    /**
     * 举报ID
     */
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
     * 处理状态：0-待处理 1-已处理 2-驳回
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}