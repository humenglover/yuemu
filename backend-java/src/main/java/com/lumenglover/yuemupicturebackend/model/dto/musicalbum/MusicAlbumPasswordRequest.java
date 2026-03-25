package com.lumenglover.yuemupicturebackend.model.dto.musicalbum;

import lombok.Data;

import java.io.Serializable;

/**
 * 音乐专栏密码请求
 */
@Data
public class MusicAlbumPasswordRequest implements Serializable {

    /**
     * 专栏ID
     */
    private Long albumId;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
} 