package com.lumenglover.yuemupicturebackend.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import com.lumenglover.yuemupicturebackend.config.CosClientConfig;

/**
 * 腾讯云图片审核工具类
 */
@Slf4j
@Component
public class TencentCloudImageAuditUtil {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 同步审核图片
     * @param imageUrl 图片URL
     * @param bizType 审核策略类型
     * @return 审核结果
     */
    public ImageAuditingResponse auditImageByUrl(String imageUrl, String bizType) {
        try {
            // 创建图片审核请求
            ImageAuditingRequest request = new ImageAuditingRequest();
            // 设置bucketName
            request.setBucketName(cosClientConfig.getBucket());
            // 使用detect-url参数审核任意公网可访问的图片链接
            request.setDetectUrl(imageUrl); // 设置图片URL
            if (bizType != null && !bizType.isEmpty()) {
                request.setBizType(bizType); // 设置审核策略类型
            }

            // 执行图片审核
            ImageAuditingResponse response = cosClient.imageAuditing(request);
            log.info("图片审核完成，URL: {}, 审核结果: {}", imageUrl, response);
            return response;
        } catch (Exception e) {
            log.error("图片审核失败，URL: {}, 错误: {}", imageUrl, e.getMessage());
            throw e;
        }
    }

    /**
     * 根据审核结果判断图片是否合规
     * @param response 审核响应
     * @return true表示合规，false表示不合规
     */
    public boolean isImageCompliant(ImageAuditingResponse response) {
        if (response == null) {
            log.warn("审核结果为空，视为不合规");
            return false;
        }

        // Result: 0-审核正常，1-判定为违规敏感文件，2-疑似敏感，建议人工复核
        String result = response.getResult();
        if (result == null) {
            log.warn("审核结果为空，视为不合规");
            return false;
        }

        // 0表示正常，1表示违规，2表示疑似
        return "0".equals(result);
    }

    /**
     * 获取审核结果的标签
     * @param response 审核响应
     * @return 审核标签
     */
    public String getAuditLabel(ImageAuditingResponse response) {
        if (response != null && response.getLabel() != null) {
            return response.getLabel();
        }
        return "Normal";
    }

    /**
     * 获取审核结果的分数
     * @param response 审核响应
     * @return 审核分数
     */
    public Integer getAuditScore(ImageAuditingResponse response) {
        if (response != null && response.getScore() != null) {
            try {
                return Integer.valueOf(response.getScore());
            } catch (NumberFormatException e) {
                log.warn("审核分数格式错误: {}", response.getScore());
                return 0;
            }
        }
        return 0;
    }
}
