package com.lumenglover.yuemupicturebackend.model.dto.like;

import lombok.Data;

import java.util.List;

@Data
public class LikeBatchRequest {
    /**
     * 点赞记录ID列表
     */
    private List<Long> ids;

    /**
     * 操作类型：delete-删除 recover-恢复 physical-物理删除
     */
    private String operation;
}
