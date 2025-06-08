package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;

/**
 * 通用文件上传服务
 */
public interface FileUploadService {

    /**
     * 上传图片（通用接口）
     *
     * @param inputSource 输入源（可以是MultipartFile或URL字符串）
     * @param userId 用户ID
     * @param name 图片名称（可选）
     * @param description 图片描述（可选）
     * @param tags 标签（可选）
     * @return 图片视图对象
     */
    PictureVO uploadPicture(Object inputSource, Long userId, String name, String description, String tags);

    /**
     * 上传音频（通用接口）
     *
     * @param inputSource 输入源（可以是MultipartFile或URL字符串）
     * @param userId 用户ID
     * @param title 音频标题（可选）
     * @param description 音频描述（可选）
     * @param artist 艺术家（可选）
     * @param tags 标签（可选）
     * @return 音频视图对象
     */
    AudioFileVO uploadAudio(Object inputSource, Long userId, String title, String description, String artist, String tags);
}
