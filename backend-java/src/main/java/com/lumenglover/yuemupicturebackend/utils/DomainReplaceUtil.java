package com.lumenglover.yuemupicturebackend.utils;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 域名替换工具类
 * 用于将数据库中存储的腾讯云COS域名替换为自定义域名
 */
@Component
public class DomainReplaceUtil {

    /**
     * -- GETTER --
     *  获取存储桶域名
     *
     * @return 存储桶域名
     */
    // 存储桶域名模式 - 从配置中获取
    @Getter
    private static String bucketDomain;
    /**
     * -- GETTER --
     *  获取自定义域名
     *
     * @return 自定义域名
     */
    // 自定义域名
    @Getter
    private static String customDomain;

    @Value("${cos.client.host:}")
    public void setBucketDomain(String host) {
        // 从host配置中提取域名部分，例如从 https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com 提取域名
        if (host.startsWith("http://") || host.startsWith("https://")) {
            int startIndex = host.indexOf("//") + 2;
            int endIndex = host.indexOf('/', startIndex);
            if (endIndex == -1) {
                endIndex = host.length();
            }
            DomainReplaceUtil.bucketDomain = host.substring(startIndex, endIndex);
        } else {
            DomainReplaceUtil.bucketDomain = host;
        }
    }

    @Value("${cos.client.custom-domain:}")
    public void setCustomDomain(String customDomain) {
        DomainReplaceUtil.customDomain = customDomain;
    }

    /**
     * 替换单个URL的域名
     *
     * @param url 原始URL
     * @return 替换后的URL
     */
    public static String replaceDomain(String url) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(customDomain) || StrUtil.isBlank(bucketDomain)) {
            // 如果没有自定义域名或存储桶域名，则直接返回原URL
            return url;
        }

        // 检查URL是否包含存储桶域名
        if (url.contains(bucketDomain)) {
            // 提取路径部分（域名之后的内容）
            int bucketDomainIndex = url.indexOf(bucketDomain);
            String pathAndQuery = url.substring(bucketDomainIndex + bucketDomain.length());

            // 确保路径部分以/开头
            if (!pathAndQuery.startsWith("/")) {
                pathAndQuery = "/" + pathAndQuery;
            }

            // 构建新的URL，使用自定义域名
            return customDomain + pathAndQuery;
        }

        return url;
    }

    /**
     * 批量替换URL数组中的域名
     *
     * @param urls URL数组
     * @return 替换后的URL数组
     */
    public static String[] replaceDomains(String[] urls) {
        if (urls == null) {
            return null;
        }

        String[] result = new String[urls.length];
        for (int i = 0; i < urls.length; i++) {
            result[i] = replaceDomain(urls[i]);
        }

        return result;
    }

    /**
     * 批量替换URL列表中的域名
     *
     * @param urls URL列表
     * @return 替换后的URL列表
     */
    public static java.util.List<String> replaceDomains(java.util.List<String> urls) {
        if (urls == null) {
            return null;
        }

        java.util.List<String> result = new java.util.ArrayList<>();
        for (String url : urls) {
            result.add(replaceDomain(url));
        }

        return result;
    }

}
