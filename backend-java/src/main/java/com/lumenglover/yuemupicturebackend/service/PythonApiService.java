package com.lumenglover.yuemupicturebackend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 专门用于与后端 Python 服务交互扩展功能的接口 (如智能抠图等)
 */
public interface PythonApiService {

    /**
     * 调用 Python 服务智能去除图片背景
     * 直接返回带透明背景的 PNG 图片字节流 (不落库/不存云)
     *
     * @param file 原图片文件
     * @return 抠图后 PNG 格式的图片字节数组
     */
    byte[] removeBackground(MultipartFile file);

    /**
     * 调用 Python 服务进行人脸打马赛克
     *
     * @param file 原图片文件
     * @return 打码后 PNG 格式的图片字节数组
     */
    byte[] faceBlur(MultipartFile file);

    /**
     * 调用 Python 服务进行智能换背景 (MODNet)
     *
     * @param file 原图文件
     * @param color 背景颜色
     * @param backgroundImage 背景图片文件 (可选)
     * @return 换背景后 PNG 格式的图片字节数组
     */
    byte[] changeBackground(MultipartFile file, String color, MultipartFile backgroundImage);
}
