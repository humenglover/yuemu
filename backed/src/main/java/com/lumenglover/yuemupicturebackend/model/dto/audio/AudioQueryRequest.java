package com.lumenglover.yuemupicturebackend.model.dto.audio;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 音频查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AudioQueryRequest extends PageRequest implements Serializable {

    /**
     * 音频标题
     */
    private String title;

    /**
     * 艺术家/作者
     */
    private String artist;

    /**
     * 专辑名称
     */
    private String album;

    /**
     * 音乐类型/风格
     */
    private String genre;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（ascend 升序 / descend 降序）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}
