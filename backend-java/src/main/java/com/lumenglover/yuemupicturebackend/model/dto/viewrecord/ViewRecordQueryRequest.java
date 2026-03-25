package com.lumenglover.yuemupicturebackend.model.dto.viewrecord;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 浏览记录查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ViewRecordQueryRequest extends PageRequest {

    /**
     * 目标类型（1-图片，2-帖子等）
     */
    private Integer targetType;
}
