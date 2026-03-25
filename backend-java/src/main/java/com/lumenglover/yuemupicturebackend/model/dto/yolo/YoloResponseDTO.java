package com.lumenglover.yuemupicturebackend.model.dto.yolo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * YOLO 检测结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoloResponseDTO {

    /**
     * 标注后的图片（Base64 编码）
     */
    private String annotatedImageBase64;

    /**
     * 检测到的目标列表
     */
    private List<YoloDetection> detections;

    /**
     * 单个检测目标信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YoloDetection {
        private String label;
        private double confidence;
        private int x;
        private int y;
        private int width;
        private int height;
    }
}
