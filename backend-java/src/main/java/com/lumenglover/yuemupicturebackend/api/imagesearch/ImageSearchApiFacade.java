package com.lumenglover.yuemupicturebackend.api.imagesearch;

import com.lumenglover.yuemupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.lumenglover.yuemupicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.lumenglover.yuemupicturebackend.api.imagesearch.sub.GetImageListApi;
import com.lumenglover.yuemupicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import com.lumenglover.yuemupicturebackend.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     * @param imageUrl 原始图片URL
     * @return 搜索结果列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        try {
            // 先将图片转换为PNG格式
            byte[] pngImageData = ImageUtils.convertToPng(imageUrl);
            log.info("图片已转换为PNG格式，大小: {} bytes", pngImageData.length);

            // 使用原始图片URL进行搜索（因为GetImagePageUrlApi内部会处理图片下载）
            String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
            log.info("获取到搜索页面URL: {}", imagePageUrl);

            String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
            log.info("获取到第一个图片URL: {}", imageFirstUrl);

            List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
            log.info("获取到搜索结果列表，数量: {}", imageList.size());

            return imageList;
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        // 示例：使用一个通用的测试图片URL
        List<ImageSearchResult> imageList = searchImage("https://example.com/test-image.jpg");
        System.out.println("结果列表: " + imageList);
    }
}
