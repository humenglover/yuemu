package com.lumenglover.yuemupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户创建请求
 */
@Data
public class PictureOperation implements Serializable {

    /**
     * id
     */
    private List<Long> ids;

    /**
     * 操作类型
     */
    private long operationType;
}
