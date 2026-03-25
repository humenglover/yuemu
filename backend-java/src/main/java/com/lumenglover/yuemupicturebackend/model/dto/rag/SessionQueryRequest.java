package com.lumenglover.yuemupicturebackend.model.dto.rag;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 会话查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SessionQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会话名称
     */
    private String sessionName;
    
    /**
     * 是否活跃
     */
    private Integer isActive;
    
    /**
     * 是否删除
     */
    private Integer isDelete;
}