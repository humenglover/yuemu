package com.lumenglover.yuemupicturebackend.model.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建举报请求
 */
@Data
public class ReportAddRequest implements Serializable {

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
     * 举报截图URL列表
     */
    private List<String> screenshotUrls;

    private static final long serialVersionUID = 1L;
}