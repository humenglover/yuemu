package com.lumenglover.yuemupicturebackend.model.dto.message;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量操作留言请求
 */
@Data
public class MessageBatchRequest implements Serializable {

    /**
     * 留言ID列表
     */
    private List<Long> ids;

    /**
     * 操作类型（delete-删除, recover-恢复, physical-物理删除）
     */
    private String operation;

    private static final long serialVersionUID = 1L;
}
