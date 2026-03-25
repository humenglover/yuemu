package com.lumenglover.yuemupicturebackend.job;

/**
 * 热门搜索同步接口
 */
public interface HotSearchSync {

    /**
     * 同步热门搜索数据
     */
    void syncHotSearch();

    /**
     * 预热缓存
     */
    void warmUpCache();

    /**
     * 批量处理搜索关键词
     */
    void batchProcessSearchKeywords();

    /**
     * 批量处理用户搜索记录
     */
    void batchProcessUserSearchRecords();
}
