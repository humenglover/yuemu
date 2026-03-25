package com.lumenglover.yuemupicturebackend.model.dto.share;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShareAdminRequest extends PageRequest implements Serializable {
    /**
     * 分享ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被分享内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 被分享内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 是否分享
     */
    private Boolean isShared;

    /**
     * 分享时间
     */
    private Date shareTime;

    /**
     * 是否已读
     */
    private Integer isRead;

    private static final long serialVersionUID = 1L;
}
