package com.lumenglover.yuemupicturebackend.model.dto.timealbum;

import lombok.Data;

@Data
public class TimeAlbumPasswordRequest {
    /**
     * 相册ID
     */
    private Long albumId;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 原密码（修改密码时需要）
     */
    private String oldPassword;
}
