package com.lumenglover.yuemupicturebackend.model.dto.share;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ShareBatchRequest implements Serializable {
    /**
     * 分享记录ID列表
     */
    private List<Long> ids;

    /**
     * 操作类型（"delete" - 删除, "restore" - 恢复）
     */
    private String operation;

    private static final long serialVersionUID = 1L;
}
