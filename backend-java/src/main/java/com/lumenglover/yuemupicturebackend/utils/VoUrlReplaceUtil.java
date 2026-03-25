package com.lumenglover.yuemupicturebackend.utils;

import cn.hutool.core.util.StrUtil;

/**
 * VO类URL替换工具类
 * 用于统一处理VO对象中的URL替换
 */
public class VoUrlReplaceUtil {

    /**
     * 替换URL字段
     * @param url 原始URL
     * @return 替换后的URL
     */
    public static String replaceUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        return DomainReplaceUtil.replaceDomain(url);
    }

    /**
     * 批量替换URL数组
     * @param urls URL数组
     * @return 替换后的URL数组
     */
    public static String[] replaceUrls(String[] urls) {
        if (urls == null) {
            return null;
        }
        String[] result = new String[urls.length];
        for (int i = 0; i < urls.length; i++) {
            result[i] = replaceUrl(urls[i]);
        }
        return result;
    }

    /**
     * 批量替换URL列表
     * @param urls URL列表
     * @return 替换后的URL列表
     */
    public static java.util.List<String> replaceUrls(java.util.List<String> urls) {
        if (urls == null) {
            return null;
        }
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String url : urls) {
            result.add(replaceUrl(url));
        }
        return result;
    }
}
