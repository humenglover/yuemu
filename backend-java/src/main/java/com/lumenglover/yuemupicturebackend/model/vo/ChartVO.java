package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 图表数据封装类
 */
@Data
public class ChartVO implements Serializable {

    /**
     * 雷达图数据 - 用户信息
     */
    private RadarChartData radarChartData;

    /**
     * 饼图数据 - 图片信息
     */
    private PieChartData pieChartData;

    /**
     * 堆叠柱状图数据 - 帖子信息
     */
    private StackedBarChartData stackedBarChartData;

    /**
     * 面积图数据 - 空间信息
     */
    private AreaChartData areaChartData;

    private static final long serialVersionUID = 1L;

    /**
     * 雷达图数据
     */
    @Data
    public static class RadarChartData implements Serializable {
        private List<String> indicator; // 雷达图指标
        private List<Map<String, Object>> data; // 雷达图数据

        private static final long serialVersionUID = 1L;
    }

    /**
     * 饼图数据
     */
    @Data
    public static class PieChartData implements Serializable {
        private List<String> labels; // 饼图标签
        private List<Integer> values; // 饼图值

        private static final long serialVersionUID = 1L;
    }

    /**
     * 堆叠柱状图数据
     */
    @Data
    public static class StackedBarChartData implements Serializable {
        private List<String> xAxisData; // x轴数据
        private List<Map<String, Object>> series; // 系列数据

        private static final long serialVersionUID = 1L;
    }

    /**
     * 面积图数据
     */
    @Data
    public static class AreaChartData implements Serializable {
        private List<String> xAxisData; // x轴数据
        private List<Map<String, Object>> series; // 系列数据

        private static final long serialVersionUID = 1L;
    }
}
