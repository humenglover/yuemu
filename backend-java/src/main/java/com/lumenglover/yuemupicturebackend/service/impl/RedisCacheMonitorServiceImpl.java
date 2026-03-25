package com.lumenglover.yuemupicturebackend.service.impl;

import com.lumenglover.yuemupicturebackend.service.RedisCacheMonitorService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Redis缓存监控Service实现类
 */
@Service
public class RedisCacheMonitorServiceImpl implements RedisCacheMonitorService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Map<String, Object> getRedisMonitorInfo() {
        Map<String, Object> result = new HashMap<>(3);

        // 获取Redis信息
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisConnection::info);

        // 获取命令统计信息
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));

        // 获取数据库大小
        Long dbSize = redisTemplate.execute(RedisConnection::dbSize);

        // 处理命令统计信息
        List<Map<String, String>> commandStatsList = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, String> data = new HashMap<>(2);
                String property = commandStats.getProperty(key);
                data.put("name", key.substring(key.startsWith("cmdstat_") ? 8 : 0));
                data.put("value", property.split(",")[0].substring(6));
                commandStatsList.add(data);
            });
        }

        result.put("info", info);
        result.put("dbSize", dbSize);
        result.put("commandStats", commandStatsList);

        return result;
    }

    @Override
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> result = new HashMap<>(4);

        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisConnection::info);

        if (info != null) {
            // 已用内存
            result.put("usedMemory", info.getProperty("used_memory"));
            // 内存峰值
            result.put("usedMemoryPeak", info.getProperty("used_memory_peak"));
            // 总内存
            result.put("totalSystemMemory", info.getProperty("total_system_memory"));
            // 内存碎片率
            result.put("memFragmentationRatio", info.getProperty("mem_fragmentation_ratio"));
            // Lua内存
            result.put("usedMemoryLua", info.getProperty("used_memory_lua"));
            // 内存分配器
            result.put("memAllocator", info.getProperty("mem_allocator"));
        }

        return result;
    }

    @Override
    public Map<String, Object> getKeysStatistics() {
        Map<String, Object> result = new HashMap<>(3);

        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisConnection::info);

        if (info != null) {
            // 获取总键数
            Long totalKeys = redisTemplate.execute(RedisConnection::dbSize);
            result.put("totalKeys", totalKeys != null ? totalKeys.toString() : "0");

            // 获取过期键数
            String expiredKeys = info.getProperty("expired_keys", "0");
            result.put("expiredKeys", expiredKeys);

            // 计算每秒过期键数
            String uptime = info.getProperty("uptime_in_seconds", "1");
            long uptimeSeconds = Long.parseLong(uptime);
            long expiredCount = Long.parseLong(expiredKeys);
            double expiredPerSec = uptimeSeconds > 0 ? (double) expiredCount / uptimeSeconds : 0;
            result.put("expiredKeysPerSec", String.format("%.2f", expiredPerSec));
        }

        return result;
    }
}
