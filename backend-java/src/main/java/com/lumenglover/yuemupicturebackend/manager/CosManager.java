package com.lumenglover.yuemupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();

        // 根据文件大小动态调整压缩质量
        int quality = getQualityForFileSize(file.length());

        // 1. 图片压缩（转成 webp 格式，根据文件大小动态调整质量）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule(String.format("imageMogr2/format/webp/quality/%d", quality));
        rules.add(compressRule);

        // 2. 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            // 同时设置缩略图质量与原图一致
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>/quality/%d", 1024, 1024, quality));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 根据文件大小动态确定压缩质量
     * @param fileSize 文件大小（字节）
     * @return 压缩质量（1-100）
     */
    private int getQualityForFileSize(long fileSize) {
        if (fileSize < 500 * 1024) { // 小于 500KB
            return 95; // 高质量
        } else if (fileSize < 1024 * 1024) { // 小于 1MB
            return 92; // 较高质量
        } else if (fileSize < 2 * 1024 * 1024) { // 小于 2MB
            return 90; // 高质量
        } else if (fileSize < 5 * 1024 * 1024) { // 小于 5MB
            return 85; // 中等质量
        } else { // 5MB及以上
            return 80; // 适中质量
        }
    }

    /**
     * 上传音频对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putAudioObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 设置存储类型为标准存储
        putObjectRequest.setStorageClass(StorageClass.Standard);
        // 设置文件的 Content-Type
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = "audio/" + FileUtil.getSuffix(key);
        metadata.setContentType(contentType);
        metadata.setContentLength(file.length());
        putObjectRequest.setMetadata(metadata);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
