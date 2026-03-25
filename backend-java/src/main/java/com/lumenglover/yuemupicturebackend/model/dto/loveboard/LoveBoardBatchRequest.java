package com.lumenglover.yuemupicturebackend.model.dto.loveboard;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 恋爱画板批量操作请求
 */
@Data
public class LoveBoardBatchRequest implements Serializable {

    /**
     * 操作类型（"delete" - 删除, "recover" - 恢复, "physical" - 物理删除）
     */
    private String operation;

    /**
     * 要操作的恋爱画板ID列表
     */
    private List<Long> ids;

    private static final long serialVersionUID = 1L;
} 