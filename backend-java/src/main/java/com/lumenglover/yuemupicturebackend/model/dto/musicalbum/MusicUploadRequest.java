package com.lumenglover.yuemupicturebackend.model.dto.musicalbum;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 音乐上传请求
 */
@Data
public class MusicUploadRequest implements Serializable {

    /**
     * 音频文件
     */
    private MultipartFile file;

    /**
     * 专栏ID
     */
    private Long albumId;

    /**
     * 音频标题
     */
    private String title;

    /**
     * 音频描述
     */
    private String description;

    /**
     * 艺术家
     */
    private String artist;

    /**
     * 专辑名称
     */
    private String album;

    /**
     * 音乐类型
     */
    private String genre;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    private static final long serialVersionUID = 1L;
}
