package com.lumenglover.yuemupicturebackend.model.dto.audio;

import lombok.Data;

import java.io.Serializable;

/**
 * 音频上传请求
 */
@Data
public class AudioUploadRequest implements Serializable {

    /**
     * 音频标题
     */
    private String title;

    /**
     * 音频描述
     */
    private String description;

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
     * 所属空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
