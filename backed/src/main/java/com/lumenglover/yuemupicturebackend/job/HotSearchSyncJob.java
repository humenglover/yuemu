package com.lumenglover.yuemupicturebackend.job;

import com.lumenglover.yuemupicturebackend.esdao.EsSearchKeywordDao;
import com.lumenglover.yuemupicturebackend.mapper.HotSearchMapper;
import com.lumenglover.yuemupicturebackend.model.entity.HotSearch;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsSearchKeyword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import cn.hutool.core.util.RandomUtil;

@Component
@Slf4j
public class HotSearchSyncJob implements HotSearchSync, CommandLineRunner {

    private static final String HOT_SEARCH_CACHE_KEY = "hot_search:%s";
    private static final int DEFAULT_SIZE = 50;  // 缓存前50个热门搜索
    private static final String[] SEARCH_TYPES = {"picture", "user", "post", "space"};

    @Resource
    private EsSearchKeywordDao esSearchKeywordDao;

    @Resource
    private HotSearchMapper hotSearchMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 应用启动时进行缓存预热
     */
    @Override
    @Async
    public void run(String... args) {
        try {
            log.info("开始热门搜索缓存预热");
            // 延迟5秒，等待其他组件初始化完成
            TimeUnit.SECONDS.sleep(5);
            warmUpCache();
            log.info("热门搜索缓存预热完成");
        } catch (Exception e) {
            log.error("热门搜索缓存预热失败", e);
        }
    }

    /**
     * 缓存预热
     */
    @Override
    public void warmUpCache() {
        // 获取最近24小时的数据
        Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

        for (String type : SEARCH_TYPES) {
            try {
                // 先尝试从MySQL获取数据
                List<HotSearch> hotSearchList = hotSearchMapper.getHotSearchAfter(type, startTime, DEFAULT_SIZE);

                if (hotSearchList.isEmpty()) {
                    // MySQL没有数据，从ES获取
                    List<EsSearchKeyword> keywords = esSearchKeywordDao
                            .findByTypeAndUpdateTimeAfterOrderByCountDesc(type, startTime);

                    if (!keywords.isEmpty()) {
                        // 转换为MySQL实体并保存
                        hotSearchList = keywords.stream()
                                .map(keyword -> {
                                    HotSearch hotSearch = new HotSearch();
                                    hotSearch.setKeyword(keyword.getKeyword());
                                    hotSearch.setType(keyword.getType());
                                    hotSearch.setCount(keyword.getCount());
                                    hotSearch.setLastUpdateTime(keyword.getUpdateTime());
                                    return hotSearch;
                                })
                                .collect(Collectors.toList());

                        // 批量插入或更新到MySQL
                        hotSearchMapper.batchInsertOrUpdate(hotSearchList);
                    }
                }

                // 更新Redis缓存
                if (!hotSearchList.isEmpty()) {
                    updateCache(type, hotSearchList);
                    log.info("类型{}的热门搜索缓存预热成功，数量: {}", type, hotSearchList.size());
                }
            } catch (Exception e) {
                log.error("类型{}的热门搜索缓存预热失败", type, e);
            }
        }
    }

    /**
     * 每17分钟同步一次热门搜索数据到MySQL和Redis
     */
    @Override
    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void syncHotSearch() {
        try {
            log.info("开始同步热门搜索数据");

            // 获取最近24小时的数据
            Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

            // 获取所有类型的热门搜索
            for (String type : SEARCH_TYPES) {
                // 从ES获取热门搜索数据
                List<EsSearchKeyword> keywords = esSearchKeywordDao
                        .findByTypeAndUpdateTimeAfterOrderByCountDesc(type, startTime);

                if (!keywords.isEmpty()) {
                    // 转换为MySQL实体
                    List<HotSearch> hotSearchList = keywords.stream()
                            .map(keyword -> {
                                HotSearch hotSearch = new HotSearch();
                                hotSearch.setKeyword(keyword.getKeyword());
                                hotSearch.setType(keyword.getType());
                                hotSearch.setCount(keyword.getCount());
                                hotSearch.setLastUpdateTime(keyword.getUpdateTime());
                                return hotSearch;
                            })
                            .collect(Collectors.toList());

                    // 批量插入或更新到MySQL
                    hotSearchMapper.batchInsertOrUpdate(hotSearchList);

                    // 更新Redis缓存
                    updateCache(type, hotSearchList);

                    log.info("同步{}类型的热门搜索数据成功，数量: {}", type, hotSearchList.size());
                }
            }

            log.info("同步热门搜索数据完成");
        } catch (Exception e) {
            log.error("同步热门搜索数据失败", e);
        }
    }

    /**
     * 更新Redis缓存
     */
    private void updateCache(String type, List<HotSearch> hotSearchList) {
        String cacheKey = String.format(HOT_SEARCH_CACHE_KEY, type);
        try {
            // 获取前50个热门搜索词
            List<String> keywords = hotSearchList.stream()
                    .limit(DEFAULT_SIZE)
                    .map(HotSearch::getKeyword)
                    .collect(Collectors.toList());

            // 使用管道批量操作
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.multi();
                // 删除旧的缓存
                connection.del(cacheKey.getBytes());
                // 添加新的缓存
                for (String keyword : keywords) {
                    connection.rPush(cacheKey.getBytes(), keyword.getBytes());
                }
                // 设置过期时间（13-15分钟随机）
                long expireSeconds = 13 * 60 + RandomUtil.randomInt(0, 120);
                connection.expire(cacheKey.getBytes(), expireSeconds);
                connection.exec();
                return null;
            });
        } catch (Exception e) {
            log.error("更新热门搜索缓存失败, type={}", type, e);
        }
    }
}
