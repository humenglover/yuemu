package com.lumenglover.yuemupicturebackend.model.dto.systemnotify;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新系统通知请求
 */
@Data
public class SystemNotifyUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 通知标题（如：系统公告、您的帖子已精选）
     */
    private String title;

    /**
     * 通知详情（支持富文本）
     */
    private String content;

    /**
     * 是否有效[0:无效（如误发通知）, 1:有效]
     */
    private Integer isEnabled;

    private static final long serialVersionUID = 1L;
}