package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.esdao.EsSearchKeywordDao;
import com.lumenglover.yuemupicturebackend.mapper.HotSearchMapper;
import com.lumenglover.yuemupicturebackend.mapper.UserSearchRecordMapper;
import com.lumenglover.yuemupicturebackend.model.entity.HotSearch;
import com.lumenglover.yuemupicturebackend.model.entity.UserSearchRecord;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsSearchKeyword;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Collection;
import cn.hutool.core.util.RandomUtil;

@Component
@Slf4j
public class HotSearchSyncJob implements HotSearchSync, CommandLineRunner {

    private static final String HOT_SEARCH_CACHE_KEY = "hot_search:%s";
    private static final String HOT_SEARCH_REALTIME_KEY = "hot:search:realTime:%s";
    private static final String HOT_SEARCH_REALTIME_OLD_KEY = "hot:search:realTime:%s:old";
    private static final String HOT_SEARCH_MYSQL_TOP_KEY = "hot_search:mysql_top:%s"; // MySQL热门词缓存
    private static final String USER_SEARCH_RECORD_KEY = "user:search:record"; // 用户搜索记录缓存键
    private static final String INVALID_SEARCH_RECORD_KEY = "user:search:record:invalid"; // 无效搜索记录缓存键
    private static final String HOT_SEARCH_SYNC_LOCK_KEY = "hot:search:sync:lock:%s"; // 同步任务分布式锁
    private static final int DEFAULT_SIZE = 50;  // 缓存前50个热门搜索
    private static final int MYSQL_TOP_SIZE = 1000; // MySQL热门词缓存数量
    private static final int TREND_CALCULATION_LIMIT = 200; // 趋势计算限制数量
    private static final int RETRY_TIMES = 3; // 重试次数
    private static final long RETRY_INTERVAL = 100L; // 重试间隔(毫秒)
    private static final String[] SEARCH_TYPES = {"picture", "user", "post", "space"};

    // 任务执行时间统计
    private final Map<String, Long> taskExecutionTimes = new HashMap<>();
    private static final int MAX_TASK_EXECUTION_HISTORY = 10; // 保留最近10次执行时间用于动态计算锁超时时间

    // 任务失败计数
    private final Map<String, Integer> taskFailureCounts = new HashMap<>();
    private static final int FAILURE_THRESHOLD = 3; // 连续失败阈值

    // 用户搜索记录处理限制
    private static final int MAX_USER_SEARCH_RECORDS_PER_BATCH = 5000; // 单次处理最大记录数

    @Resource
    private EsSearchKeywordDao esSearchKeywordDao;

    @Resource
    private HotSearchMapper hotSearchMapper;

    @Resource
    private UserSearchRecordMapper userSearchRecordMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 应用启动时进行缓存预热
     */
    @Override
    @Async
    public void run(String... args) {
        try {
            log.info("开始热门搜索缓存预热");
            // 检查Redis/MySQL/ES是否就绪，而非固定休眠
            if (checkDependenciesReady()) {
                warmUpCache();
                log.info("热门搜索缓存预热完成");
            } else {
                log.warn("依赖服务未就绪，跳过缓存预热");
                // 缓存预热失败兜底：延迟1分钟后重试
                scheduleWarmupRetry();
            }
        } catch (Exception e) {
            log.error("热门搜索缓存预热失败", e);
            // 缓存预热失败兜底：延迟1分钟后重试
            scheduleWarmupRetry();
        }
    }

    /**
     * 检查依赖服务是否就绪
     */
    private boolean checkDependenciesReady() {
        try {
            // 简单检查Redis连接
            stringRedisTemplate.opsForValue().set("health_check", "test", 1, TimeUnit.SECONDS);
            // 简单检查数据库连接
            hotSearchMapper.selectCount(new QueryWrapper<HotSearch>().last("LIMIT 1"));
            return true;
        } catch (Exception e) {
            log.warn("依赖服务检查失败", e);
            return false;
        }
    }

    /**
     * 缓存预热失败后的重试调度
     */
    private void scheduleWarmupRetry() {
        // 在新线程中延迟1分钟后重试
        new Thread(() -> {
            try {
                Thread.sleep(60 * 1000); // 延迟1分钟
                log.info("开始重试热门搜索缓存预热");
                if (checkDependenciesReady()) {
                    warmUpCache();
                    log.info("热门搜索缓存预热重试完成");
                } else {
                    log.warn("重试时依赖服务仍不可用，稍后再次重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("缓存预热重试被中断", e);
            } catch (Exception e) {
                log.error("缓存预热重试失败", e);
            }
        }).start();
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
                // 1. 预加载MySQL中排名前1000的搜索词到Redis（仅当缓存不存在时）
                preloadMysqlTopKeywordsIfNeeded(type);

                // 2. 先尝试从MySQL获取数据
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
     * 预加载MySQL中排名前1000的搜索词到Redis（仅当缓存不存在时）
     */
    private void preloadMysqlTopKeywordsIfNeeded(String type) {
        try {
            String mysqlTopKey = String.format(HOT_SEARCH_MYSQL_TOP_KEY, type);
            // 检查缓存是否存在
            Boolean hasKey = stringRedisTemplate.hasKey(mysqlTopKey);
            if (hasKey != null && hasKey) {
                log.debug("类型{}的MySQL热门词缓存已存在，跳过预加载", type);
                return;
            }

            // 从MySQL获取排名前1000的搜索词
            List<HotSearch> topKeywords = hotSearchMapper.selectList(
                    new QueryWrapper<HotSearch>()
                            .eq("type", type)
                            .eq("isDelete", 0)
                            .orderByDesc("count")
                            .last("LIMIT " + MYSQL_TOP_SIZE)
            );

            // 存储到Redis中，使用Hash结构存储关键词和搜索次数
            Map<String, String> keywordCountMap = new HashMap<>();

            for (HotSearch hotSearch : topKeywords) {
                keywordCountMap.put(hotSearch.getKeyword(), String.valueOf(hotSearch.getCount()));
            }

            if (!keywordCountMap.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(mysqlTopKey, keywordCountMap);
                // 设置过期时间24小时
                stringRedisTemplate.expire(mysqlTopKey, 24, TimeUnit.HOURS);
                log.info("预加载{}类型MySQL热门词到Redis成功，数量: {}", type, keywordCountMap.size());
            }
        } catch (Exception e) {
            log.error("预加载{}类型MySQL热门词到Redis失败", type, e);
        }
    }

    /**
     * 每分钟同步一次热门搜索数据到MySQL和Redis
     */
    @Override
    @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟执行一次（因为趋势计算依赖1小时前的数据）
    public void syncHotSearch() {
        for (String type : SEARCH_TYPES) {
            // 为每个类型添加分布式锁
            String lockKey = String.format(HOT_SEARCH_SYNC_LOCK_KEY, type);
            RLock lock = redissonClient.getFairLock(lockKey); // 使用公平锁避免任务饥饿

            try {
                // 使用动态锁超时时间
                long lockTimeout = calculateDynamicLockTimeout("hot_search_sync_" + type);
                if (lock.tryLock(5, lockTimeout, TimeUnit.SECONDS)) {
                    log.debug("获取到{}类型同步锁，开始同步热门搜索数据", type);
                    syncHotSearchForType(type);
                } else {
                    log.warn("未能获取到{}类型的同步锁，跳过本次同步", type);
                }
            } catch (InterruptedException e) {
                log.error("获取{}类型同步锁被中断", type, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("同步{}类型热门搜索数据失败", type, e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        log.error("释放{}类型同步锁失败", type, e);
                    }
                }
            }
        }
    }

    /**
     * 同步指定类型的热门搜索数据
     */
    private void syncHotSearchForType(String type) {
        try {
            log.debug("开始同步{}类型的热门搜索数据", type);

            // 获取最近24小时的数据
            Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

            // 1. 读取实时ZSet数据（限制范围以减少内存占用）
            String zsetKey = String.format(HOT_SEARCH_REALTIME_KEY, type);
            var tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, TREND_CALCULATION_LIMIT);

            List<HotSearch> hotSearchList = null;
            if (tuples != null && !tuples.isEmpty()) {
                hotSearchList = tuples.stream().map(t -> {
                    HotSearch hs = new HotSearch();
                    // 对关键词进行长度校验和截断，避免数据库错误
                    String keyword = t.getValue();
                    if (keyword != null && keyword.length() > 128) {  // 数据库keyword字段最大长度为128
                        keyword = keyword.substring(0, 128);
                    }
                    hs.setKeyword(keyword);
                    hs.setType(type);
                    hs.setRealTimeCount(t.getScore() == null ? 0L : t.getScore().longValue());
                    hs.setCount(hs.getRealTimeCount());
                    hs.setTrend(0D);
                    hs.setLastUpdateTime(new Date());
                    return hs;
                }).collect(Collectors.toList());

                // 仅对前N条计算趋势（优化性能）
                calculateTrendsForTopItems(hotSearchList, type);

                // 批量插入或更新到MySQL
                hotSearchMapper.batchInsertOrUpdate(hotSearchList);

                // 更新Redis缓存
                updateCache(type, hotSearchList);

                log.debug("同步{}类型的实时热门搜索数据成功，数量: {}", type, hotSearchList.size());
            } else {
                // 回退到ES数据
                List<EsSearchKeyword> keywords = esSearchKeywordDao
                        .findByTypeAndUpdateTimeAfterOrderByCountDesc(type, startTime);
                if (!keywords.isEmpty()) {
                    hotSearchList = keywords.stream()
                            .map(keyword -> {
                                HotSearch hotSearch = new HotSearch();
                                // 对关键词进行长度校验和截断，避免数据库错误
                                String kw = keyword.getKeyword();
                                if (kw != null && kw.length() > 128) {  // 数据库keyword字段最大长度为128
                                    kw = kw.substring(0, 128);
                                }
                                hotSearch.setKeyword(kw);
                                hotSearch.setType(keyword.getType());
                                hotSearch.setCount(keyword.getCount());
                                hotSearch.setRealTimeCount(0L);
                                hotSearch.setTrend(0D);
                                hotSearch.setLastUpdateTime(keyword.getUpdateTime());
                                return hotSearch;
                            })
                            .collect(Collectors.toList());
                    hotSearchMapper.batchInsertOrUpdate(hotSearchList);
                    updateCache(type, hotSearchList);
                    log.debug("同步{}类型的ES热门搜索数据成功，数量: {}", type, hotSearchList.size());
                } else {
                    // 如果ES也没有数据，使用默认数据
                    List<String> defaultKeywords = getDefaultHotSearchKeywords(type);
                    hotSearchList = defaultKeywords.stream()
                            .map(keyword -> {
                                HotSearch hotSearch = new HotSearch();
                                // 对关键词进行长度校验和截断，避免数据库错误
                                String kw = keyword;
                                if (kw != null && kw.length() > 512) {  // 数据库keyword字段最大长度为512
                                    kw = kw.substring(0, 512);
                                }
                                hotSearch.setKeyword(kw);
                                hotSearch.setType(type);
                                hotSearch.setCount(1L);
                                hotSearch.setRealTimeCount(1L);
                                hotSearch.setTrend(0D);
                                hotSearch.setLastUpdateTime(new Date());
                                return hotSearch;
                            })
                            .collect(Collectors.toList());
                    hotSearchMapper.batchInsertOrUpdate(hotSearchList);
                    updateCache(type, hotSearchList);
                    log.debug("同步{}类型的默认热门搜索数据成功，数量: {}", type, hotSearchList.size());
                }
            }

            // 保存当前数据作为下一小时的基准数据（使用原子操作）
            saveCurrentAsOld(zsetKey, type);

            // 更新MySQL热门词缓存
            preloadMysqlTopKeywordsIfNeeded(type);

            log.debug("{}类型的热门搜索数据同步完成", type);
        } catch (Exception e) {
            log.error("同步{}类型热门搜索数据失败", type, e);
            recordTaskFailure("sync_hot_search_" + type);
            // 异常隔离，不影响其他类型处理
        }
    }

    /**
     * 仅对前N条计算趋势（性能优化）
     */
    private void calculateTrendsForTopItems(List<HotSearch> hotSearchList, String type) {
        // 获取1小时前的搜索次数
        String oldZSetKey = String.format(HOT_SEARCH_REALTIME_OLD_KEY, type);

        for (int i = 0; i < Math.min(hotSearchList.size(), TREND_CALCULATION_LIMIT); i++) {
            HotSearch hotSearch = hotSearchList.get(i);
            Double oldScore = stringRedisTemplate.opsForZSet().score(oldZSetKey, hotSearch.getKeyword());

            if (oldScore != null && oldScore > 0) {
                // 计算趋势 (当前次数 - 1小时前次数) / 1小时前次数
                double trend = (hotSearch.getRealTimeCount() - oldScore) / oldScore;
                // 限制趋势最大值为2.0 (即最多+200%)
                hotSearch.setTrend(Math.min(trend, 2.0));
            } else {
                hotSearch.setTrend(0.0);
            }
        }
    }

    /**
     * 每5分钟批量处理Redis中的搜索词数据并保存到ES和MySQL
     */
    @Override
    @Scheduled(fixedRate = 15 * 60 * 1000) // 每15分钟执行一次（业务搜索量小时减少执行频率）
    public void batchProcessSearchKeywords() {
        for (String type : SEARCH_TYPES) {
            // 为每个类型添加分布式锁
            String lockKey = String.format("es_keyword_process_lock:%s", type);
            RLock lock = redissonClient.getFairLock(lockKey); // 使用公平锁避免任务饥饿

            try {
                // 使用动态锁超时时间
                long lockTimeout = calculateDynamicLockTimeout("es_keyword_process_" + type);
                if (lock.tryLock(5, lockTimeout, TimeUnit.SECONDS)) {
                    log.debug("获取到{}类型ES关键词处理锁，开始批量处理", type);
                    batchProcessSearchKeywordsForType(type);
                } else {
                    log.warn("未能获取到{}类型的ES关键词处理锁，跳过本次处理", type);
                }
            } catch (InterruptedException e) {
                log.error("获取{}类型ES关键词处理锁被中断", type, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("批量处理{}类型搜索词数据失败", type, e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        log.error("释放{}类型ES关键词处理锁失败", type, e);
                    }
                }
            }
        }
    }

    /**
     * 批量处理指定类型的搜索词数据
     */
    private void batchProcessSearchKeywordsForType(String type) {
        try {
            log.debug("开始批量处理{}类型搜索词数据", type);

            // 处理搜索次数增量
            String incrementKey = "es_keyword_increment:" + type;
            Map<Object, Object> incrementMap = stringRedisTemplate.boundHashOps(incrementKey).entries();

            // 处理新增关键词
            String newKey = "es_keyword_new:" + type;
            Set<String> newKeywords = stringRedisTemplate.boundSetOps(newKey).members();

            List<EsSearchKeyword> keywordsToUpdate = new ArrayList<>();
            List<EsSearchKeyword> keywordsToCreate = new ArrayList<>();

            // 批量查询ES关键词（性能优化）
            if (!incrementMap.isEmpty()) {
                Set<String> keywordsToQuery = incrementMap.keySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());

                List<EsSearchKeyword> existingKeywords = new ArrayList<>();
                // 分批查询，避免一次性查询太多
                for (int i = 0; i < keywordsToQuery.size(); i += 100) {
                    int endIndex = Math.min(i + 100, keywordsToQuery.size());
                    List<String> batchKeywords = new ArrayList<>(keywordsToQuery).subList(i, endIndex);

                    // 这里假设ES DAO有批量查询方法，如果没有则逐个查询并重试
                    for (String keyword : batchKeywords) {
                        EsSearchKeyword esKeyword = retryOperation(() ->
                                esSearchKeywordDao.findByTypeAndKeyword(type, keyword), "ES查询关键词");
                        if (esKeyword != null) {
                            existingKeywords.add(esKeyword);
                        }
                    }
                }

                // 构建关键词到ES实体的映射，处理重复键的情况
                Map<String, EsSearchKeyword> keywordToEsMap = new HashMap<>();
                for (EsSearchKeyword keyword : existingKeywords) {
                    keywordToEsMap.put(keyword.getKeyword(), keyword);
                }

                // 处理增量更新
                for (Map.Entry<Object, Object> entry : incrementMap.entrySet()) {
                    String keyword = (String) entry.getKey();
                    long increment = Long.parseLong((String) entry.getValue());

                    EsSearchKeyword esKeyword = keywordToEsMap.get(keyword);
                    if (esKeyword != null) {
                        esKeyword.setCount(esKeyword.getCount() + increment);
                        esKeyword.setUpdateTime(new Date());
                        keywordsToUpdate.add(esKeyword);
                    }
                }
            }

            // 处理新增关键词
            Date now = new Date();
            if (newKeywords != null) {
                for (String keyword : newKeywords) {
                    EsSearchKeyword esKeyword = new EsSearchKeyword();
                    // 对关键词进行长度校验和截断，避免数据库错误
                    String kw = keyword;
                    if (kw != null && kw.length() > 512) {  // 数据库keyword字段最大长度为512
                        kw = kw.substring(0, 512);
                    }
                    esKeyword.setKeyword(kw);
                    esKeyword.setType(type);
                    esKeyword.setCount(1L);
                    esKeyword.setCreateTime(now);
                    esKeyword.setUpdateTime(now);
                    keywordsToCreate.add(esKeyword);
                }
            }

            // 批量保存到ES（分步执行，每步异常单独捕获）
            boolean updateSuccess = false;
            boolean createSuccess = false;

            if (!keywordsToUpdate.isEmpty()) {
                try {
                    retryOperation(() -> {
                        esSearchKeywordDao.saveAll(keywordsToUpdate);
                        return null;
                    }, "ES批量更新关键词", "es_keyword_update_" + type);
                    updateSuccess = true;
                } catch (Exception e) {
                    log.error("批量更新ES关键词失败", e);
                }
            }

            if (!keywordsToCreate.isEmpty()) {
                try {
                    retryOperation(() -> {
                        esSearchKeywordDao.saveAll(keywordsToCreate);
                        return null;
                    }, "ES批量创建关键词", "es_keyword_create_" + type);
                    createSuccess = true;
                } catch (Exception e) {
                    log.error("批量创建ES关键词失败", e);
                }
            }

            // 清空Redis中的临时数据（批量删除，减少Redis交互次数）
            List<String> keysToDelete = new ArrayList<>();
            if (updateSuccess && !incrementMap.isEmpty()) {
                keysToDelete.add(incrementKey);
            }
            if (createSuccess && newKeywords != null && !newKeywords.isEmpty()) {
                keysToDelete.add(newKey);
            }

            if (!keysToDelete.isEmpty()) {
                stringRedisTemplate.delete(keysToDelete);
            }

            log.debug("批量处理{}类型搜索词数据完成，更新:{}条，新增:{}条",
                    type, keywordsToUpdate.size(), keywordsToCreate.size());
        } catch (Exception e) {
            log.error("批量处理{}类型搜索词数据失败", type, e);
            recordTaskFailure("batch_process_keywords_" + type);
        }
    }

    /**
     * 重试操作
     */
    private <T> T retryOperation(java.util.function.Supplier<T> operation, String operationName) {
        return retryOperation(operation, operationName, null);
    }

    /**
     * 重试操作，区分可重试异常和不可重试异常
     */
    private <T> T retryOperation(java.util.function.Supplier<T> operation, String operationName, String taskType) {
        Exception lastException = null;
        for (int i = 0; i < RETRY_TIMES; i++) {
            try {
                T result = operation.get();
                // 任务成功，记录成功状态
                if (taskType != null) {
                    recordTaskSuccess(taskType);
                }
                return result;
            } catch (Exception e) {
                lastException = e;

                // 区分可重试异常和不可重试异常
                if (isRetryableException(e)) {
                    log.warn("{}失败，第{}次重试，原因: {}", operationName, i + 1, e.getMessage());
                    if (i < RETRY_TIMES - 1) {
                        try {
                            Thread.sleep(RETRY_INTERVAL);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试被中断", ie);
                        }
                    }
                } else {
                    log.error("{}遇到不可重试异常，停止重试，原因: {}", operationName, e.getMessage(), e);
                    // 记录任务失败
                    if (taskType != null) {
                        recordTaskFailure(taskType);
                    }
                    throw new RuntimeException("遇到不可重试异常: " + operationName, e);
                }
            }
        }

        // 记录任务失败
        if (taskType != null) {
            recordTaskFailure(taskType);
        }
        throw new RuntimeException("重试" + RETRY_TIMES + "次后仍然失败: " + operationName, lastException);
    }

    /**
     * 判断是否为可重试异常
     */
    private boolean isRetryableException(Exception e) {
        // 网络相关异常通常是可重试的
        if (e instanceof org.springframework.dao.RecoverableDataAccessException) {
            return true;
        }
        if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection") ||
                        e.getMessage().toLowerCase().contains("timeout") ||
                        e.getMessage().toLowerCase().contains("retry")
        )) {
            return true;
        }

        // 数据格式错误等通常是不可重试的
        if (e instanceof NumberFormatException || e instanceof IllegalArgumentException) {
            return false;
        }

        // 默认认为是可重试的
        return true;
    }

    /**
     * 每30分钟批量处理用户搜索记录
     */
    @Override
    @Scheduled(fixedRate = 60 * 60 * 1000) // 每60分钟执行一次
    public void batchProcessUserSearchRecords() {
        // 添加分布式锁
        String lockKey = "user_search_record_process_lock";
        RLock lock = redissonClient.getFairLock(lockKey); // 使用公平锁避免任务饥饿

        try {
            // 使用动态锁超时时间
            long lockTimeout = calculateDynamicLockTimeout("user_search_record_process");
            if (lock.tryLock(5, lockTimeout, TimeUnit.SECONDS)) {
                log.debug("获取到用户搜索记录处理锁，开始批量处理");
                batchProcessUserSearchRecordsInternal();
            } else {
                log.warn("未能获取到用户搜索记录处理锁，跳过本次处理");
            }
        } catch (InterruptedException e) {
            log.error("获取用户搜索记录处理锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("批量处理用户搜索记录失败", e);
            recordTaskFailure("batch_process_user_records");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("释放用户搜索记录处理锁失败", e);
                }
            }
        }
    }

    /**
     * 内部批量处理用户搜索记录方法
     */
    private void batchProcessUserSearchRecordsInternal() {
        try {
            log.debug("开始批量处理用户搜索记录");

            // 从Redis中获取暂存的用户搜索记录
            String key = USER_SEARCH_RECORD_KEY;
            List<String> records = stringRedisTemplate.boundListOps(key).range(0, -1);

            if (records != null && !records.isEmpty()) {
                List<UserSearchRecord> validRecords = new ArrayList<>();
                List<String> invalidRecords = new ArrayList<>();

                // 解析Redis中的记录，先过滤无效记录，并进行去重
                HashSet<String> deduplicatedKeys = new HashSet<>(); // 用于去重：userId + keyword + searchTime
                for (String recordStr : records) {
                    try {
                        UserSearchRecord record = parseUserSearchRecord(recordStr);
                        if (record != null) {
                            // 生成去重键：userId + keyword + searchTime
                            String dedupKey = record.getUserId() + "|" + record.getKeyword() + "|" + record.getSearchTime().getTime();
                            if (!deduplicatedKeys.contains(dedupKey)) {
                                deduplicatedKeys.add(dedupKey);
                                validRecords.add(record);
                            } else {
                                log.debug("发现重复用户搜索记录，已跳过: {}", recordStr);
                                invalidRecords.add(recordStr); // 重复记录也视为无效
                            }
                        } else {
                            invalidRecords.add(recordStr);
                        }
                    } catch (Exception e) {
                        log.warn("解析用户搜索记录失败: {}", recordStr, e);
                        invalidRecords.add(recordStr);
                    }
                }

                // 保存无效记录到单独的Redis键，便于后续排查
                if (!invalidRecords.isEmpty()) {
                    stringRedisTemplate.boundListOps(INVALID_SEARCH_RECORD_KEY).rightPushAll(
                            invalidRecords.toArray(new String[0]));
                    log.warn("发现{}条无效用户搜索记录，已保存到{}中", invalidRecords.size(), INVALID_SEARCH_RECORD_KEY);
                }

                // 限制单次处理记录数，避免OOM
                if (!validRecords.isEmpty()) {
                    List<UserSearchRecord> recordsToProcess = validRecords.size() > MAX_USER_SEARCH_RECORDS_PER_BATCH ?
                            validRecords.subList(0, MAX_USER_SEARCH_RECORDS_PER_BATCH) : validRecords;

                    userSearchRecordMapper.batchInsertRecords(recordsToProcess);
                    log.debug("批量处理用户搜索记录完成，处理{}条有效记录（最大限制{}条）",
                            recordsToProcess.size(), MAX_USER_SEARCH_RECORDS_PER_BATCH);

                    // 清空Redis中的记录
                    stringRedisTemplate.delete(key);
                } else {
                    log.debug("没有有效用户搜索记录需要处理");
                }
            } else {
                log.debug("没有用户搜索记录需要处理");
            }
        } catch (Exception e) {
            log.error("批量处理用户搜索记录失败", e);
        }
    }

    /**
     * 解析用户搜索记录
     */
    private UserSearchRecord parseUserSearchRecord(String recordStr) {
        if (recordStr == null || recordStr.trim().isEmpty()) {
            return null;
        }

        String[] parts = recordStr.split("\\|");
        if (parts.length != 5) {
            return null;
        }

        try {
            Long userId = Long.parseLong(parts[0]);
            String keyword = parts[1];
            String type = parts[2];
            Date searchTime = new Date(Long.parseLong(parts[3]));
            Date createTime = new Date(Long.parseLong(parts[4]));

            // 验证数据有效性
            if (userId <= 0 || keyword == null || keyword.trim().isEmpty() ||
                    type == null || type.trim().isEmpty()) {
                return null;
            }

            // 对关键词进行长度校验和截断，避免数据库错误
            if (keyword.length() > 512) {
                keyword = keyword.substring(0, 512);
            }

            UserSearchRecord record = new UserSearchRecord();
            record.setUserId(userId);
            record.setKeyword(keyword);
            record.setType(type);
            record.setSearchTime(searchTime);
            record.setCreateTime(createTime);
            record.setUpdateTime(createTime);
            record.setIsDelete(0);
            return record;
        } catch (NumberFormatException e) {
            log.warn("解析用户搜索记录数值失败: {}", recordStr, e);
            return null;
        }
    }

    /**
     * 计算动态锁超时时间
     */
    private long calculateDynamicLockTimeout(String taskType) {
        // 获取历史执行时间，如果没有则使用默认值
        Long avgExecutionTime = taskExecutionTimes.get(taskType);
        if (avgExecutionTime == null) {
            // 根据任务类型返回不同的默认超时时间
            switch (taskType) {
                case "user_search_record_process":
                    return 120; // 用户搜索记录处理可能较慢
                default:
                    return 60; // 默认60秒
            }
        }

        // 使用平均执行时间的3倍作为锁超时时间，但至少30秒，最多300秒
        long calculatedTimeout = Math.max(30, Math.min(300, avgExecutionTime * 3 / 1000));
        return calculatedTimeout;
    }

    /**
     * 记录任务执行时间
     */
    private void recordTaskExecutionTime(String taskType, long executionTimeMs) {
        taskExecutionTimes.merge(taskType, executionTimeMs, (oldVal, newVal) -> {
            // 简单移动平均，保留最近几次执行时间
            long currentAvg = oldVal != null ? (oldVal + newVal) / 2 : newVal;
            return currentAvg;
        });

        // 限制历史记录数量
        if (taskExecutionTimes.size() > MAX_TASK_EXECUTION_HISTORY) {
            // 简单清空旧记录
            Map<String, Long> newMap = new HashMap<>();
            taskExecutionTimes.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(MAX_TASK_EXECUTION_HISTORY)
                    .forEach(entry -> newMap.put(entry.getKey(), entry.getValue()));
            taskExecutionTimes.clear();
            taskExecutionTimes.putAll(newMap);
        }
    }

    /**
     * 检查任务是否连续失败
     */
    private boolean isTaskContinuouslyFailing(String taskType) {
        Integer failureCount = taskFailureCounts.getOrDefault(taskType, 0);
        return failureCount >= FAILURE_THRESHOLD;
    }

    /**
     * 记录任务失败
     */
    private void recordTaskFailure(String taskType) {
        taskFailureCounts.merge(taskType, 1, Integer::sum);

        // 如果连续失败达到阈值，触发告警
        if (isTaskContinuouslyFailing(taskType)) {
            log.error("任务 {} 连续失败 {} 次，请检查!", taskType, FAILURE_THRESHOLD);
            // 这里可以集成告警系统
        }
    }

    /**
     * 记录任务成功
     */
    private void recordTaskSuccess(String taskType) {
        taskFailureCounts.put(taskType, 0);
    }

    /**
     * 获取默认热门搜索关键词
     */
    private List<String> getDefaultHotSearchKeywords(String type) {
        if ("space".equals(type) || "user".equals(type)) {
            // 对于空间和用户，返回默认关键词 "鹿梦"
            return Arrays.asList("鹿梦");
        } else {
            // 对于帖子和图片，返回与图片相关的默认关键词
            return Arrays.asList(
                    "风景", "动物", "城市", "自然", "艺术",
                    "黑白", "抽象", "肖像", "摄影", "插画",
                    "时尚", "美食", "旅行", "运动", "科技"
            );
        }
    }

    /**
     * 保存当前数据作为下一小时的基准数据（使用原子操作优化）
     */
    private void saveCurrentAsOld(String currentZSetKey, String type) {
        try {
            String oldZSetKey = String.format(HOT_SEARCH_REALTIME_OLD_KEY, type);

            // 使用Redis的RENAME命令原子性地替换旧数据
            Boolean keyExists = stringRedisTemplate.hasKey(currentZSetKey);
            if (keyExists != null && keyExists) {
                // 如果新数据存在，先删除旧数据，然后重命名新数据
                stringRedisTemplate.delete(oldZSetKey);

                // 尝试重命名当前ZSet为旧ZSet
                try {
                    stringRedisTemplate.rename(currentZSetKey, oldZSetKey);
                } catch (Exception e) {
                    log.warn("重命名ZSet失败，使用遍历复制方式，type: {}", type, e);
                    // 重命名失败时，使用原来的复制方式作为备选
                    var tuples = stringRedisTemplate.opsForZSet().rangeWithScores(currentZSetKey, 0, -1);
                    if (tuples != null && !tuples.isEmpty()) {
                        for (var tuple : tuples) {
                            stringRedisTemplate.opsForZSet().add(oldZSetKey, tuple.getValue(), tuple.getScore());
                        }
                        // 删除原ZSet
                        stringRedisTemplate.delete(currentZSetKey);
                    }
                }

                // 为旧ZSet设置过期时间
                stringRedisTemplate.expire(oldZSetKey, 60 * 60, TimeUnit.SECONDS);
            } else {
                log.debug("当前ZSet {} 不存在，跳过保存基准数据", currentZSetKey);
            }
        } catch (Exception e) {
            log.warn("保存基准数据失败 type={}", type, e);
        }
    }

    /**
     * 更新Redis缓存（优化过期时间策略）
     */
    private void updateCache(String type, List<HotSearch> hotSearchList) {
        String cacheKey = String.format(HOT_SEARCH_CACHE_KEY, type);
        try {
            // 获取前50个热门搜索词
            List<String> keywords = hotSearchList.stream()
                    .limit(DEFAULT_SIZE)
                    .map(HotSearch::getKeyword)
                    .collect(Collectors.toList());

            // 删除旧的缓存并添加新的缓存
            stringRedisTemplate.delete(cacheKey);
            if (!keywords.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(cacheKey, keywords.toArray(new String[0]));
                // 设置固定15分钟过期，同步任务执行时主动更新（避免缓存穿透）
                stringRedisTemplate.expire(cacheKey, 15 * 60, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("更新热门搜索缓存失败, type={}", type, e);
        }
    }
}
