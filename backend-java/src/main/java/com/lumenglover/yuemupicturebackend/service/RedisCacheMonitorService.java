package com.lumenglover.yuemupicturebackend.service;

import java.util.Map;

/**
 * Redis缓存监控Service接口
 */
public interface RedisCacheMonitorService {

    /**
     * 获取Redis缓存监控信息
     *
     * @return Redis监控信息，包含info、dbSize和命令统计
     */
    Map<String, Object> getRedisMonitorInfo();

    /**
     * 获取Redis内存使用信息
     *
     * @return 内存使用信息
     */
    Map<String, Object> getMemoryInfo();

    /**
     * 获取Redis键值统计信息
     *
     * @return 键值统计信息
     */
    Map<String, Object> getKeysStatistics();
}
