package com.lumenglover.yuemupicturebackend.job;

import com.lumenglover.yuemupicturebackend.model.dto.picture.PictureHotScoreDto;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.utils.PictureScoreUpdateTracker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片推荐分数计算任务（完全无随机+分数平滑+新内容优先+低重复设计）
 * 增量更新（每30分钟） + 定时全量校准，平衡时效性与稳定性
 */
@Slf4j
@Component
public class PictureRecommendScoreJob implements CommandLineRunner {

    @Resource
    private PictureService pictureService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PictureScoreUpdateTracker pictureScoreUpdateTracker;

    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    // 核心权重配置（从配置文件读取）
    @Value("${recommend.score.view:0.12}")
    private double viewWeight;

    @Value("${recommend.score.like:0.15}")
    private double likeWeight;

    @Value("${recommend.score.comment:0.13}")
    private double commentWeight;

    @Value("${recommend.score.share:0.10}")
    private double shareWeight;

    @Value("${recommend.score.time-weight:0.50}")
    private double timeWeight;

    @Value("${recommend.score.time-decay:0.015}")
    private double timeDecay;

    // 新内容保护配置（从配置文件读取）
    @Value("${recommend.new.picture.hours:24}")
    private long newPictureHours;

    @Value("${recommend.score.new-bonus:1.8}")
    private double newPictureBonus;

    @Value("${recommend.score.smooth.alpha:0.7}")
    private double smoothAlpha;

    // 增量更新配置
    @Value("${recommend.increment.max-size:500}")
    private int recommendIncrementMaxSize;

    // 批量配置
    private static final int BATCH_UPDATE_SIZE = 100;
    private static final int PAGE_SIZE = 500;
    private static final int SHARD_SIZE = 5000;
    private static final long CACHE_EXPIRE_HOURS = 12;
    private static final long CACHE_THRESHOLD_HOURS = 720;

    // 分数计算常量（无随机相关常量）
    private static final double DECAY_RATE = 0.003;
    private static final double TIME_BONUS_BASE = 2.5;
    private static final double LOG_MULTIPLIER = 0.7;
    private static final double MIN_SCORE = 0.1;

    // 锁常量
    private static final String INCREMENT_LOCK_KEY = "picture:recommend_score:increment_lock";
    private static final String FULL_LOCK_KEY = "picture:recommend_score:full_lock";

    /**
     * 程序启动时执行一次全量计算
     */
    @Override
    public void run(String... args) {
        log.info("程序启动，开始初始化推荐分数（完全无随机+分数平滑）...");
        CompletableFuture.runAsync(this::calculateRecommendScoresFull, taskExecutor);
    }

    /**
     * 增量更新（每30分钟）：降低频率减少分数波动
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void calculateRecommendScoresIncrementallySchedule() {
        RLock lock = redissonClient.getLock(INCREMENT_LOCK_KEY);
        try {
            if (lock.tryLock(5, 20, TimeUnit.SECONDS)) {
                long queueSize = pictureScoreUpdateTracker.getRecommendScoreQueueSize();
                if (queueSize > 0) {
                    log.info("增量定时任务：发现{}个待更新图片，开始处理（单次最大{}条）",
                            queueSize, recommendIncrementMaxSize);
                    calculateRecommendScoresIncrementally(Instant.now(), true);
                }
            }
        } catch (InterruptedException e) {
            log.error("增量任务获取锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("增量任务执行异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 全量校准（每日凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void calculateRecommendScoresFullSchedule() {
        log.info("全量校准任务：开始执行（完全无随机）");
        calculateRecommendScoresFull();
    }

    /**
     * 增量计算核心方法
     */
    private void calculateRecommendScoresIncrementally(Instant now, boolean isLimit) {
        Set<String> pictureIds = isLimit
                ? pictureScoreUpdateTracker.getPictureIdsForRecommendScoreUpdateLimit(recommendIncrementMaxSize)
                : pictureScoreUpdateTracker.getPictureIdsForRecommendScoreUpdate();

        if (pictureIds == null || pictureIds.isEmpty()) {
            return;
        }

        List<Long> ids = pictureIds.stream().map(Long::valueOf).collect(Collectors.toList());
        Map<Boolean, List<Long>> idGroup = pictureService.groupPictureByNew(ids, newPictureHours);

        // 优先处理新图片
        if (!idGroup.getOrDefault(true, Collections.emptyList()).isEmpty()) {
            processPictureBatch(pictureService.listByIds(idGroup.get(true)), now, true, true);
        }

        // 处理旧图片
        if (!idGroup.getOrDefault(false, Collections.emptyList()).isEmpty()) {
            processPictureBatch(pictureService.listByIds(idGroup.get(false)), now, true, false);
        }

        pictureScoreUpdateTracker.removePicturesFromRecommendScoreUpdateQueue(pictureIds);
        log.info("增量任务完成：处理{}个图片", pictureIds.size());
    }

    /**
     * 全量校准核心方法
     */
    private void calculateRecommendScoresFull() {
        RLock lock = redissonClient.getLock(FULL_LOCK_KEY);
        try {
            if (lock.tryLock(10, 300, TimeUnit.SECONDS)) {
                long totalPictures = pictureService.countPictureScoreData();
                log.info("全量校准任务：共需处理{}张图片", totalPictures);

                Instant now = Instant.now();
                if (totalPictures > 10000) {
                    calculateRecommendScoresWithParallelSharding(now, totalPictures);
                } else {
                    calculateRecommendScoresWithoutSharding(now);
                }
            } else {
                log.warn("全量任务未获取到锁，跳过本次校准");
            }
        } catch (InterruptedException e) {
            log.error("全量任务获取锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("全量任务执行异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 并行分片处理全量数据
     */
    private void calculateRecommendScoresWithParallelSharding(Instant now, long totalPictures) {
        long minId = 0;
        long maxId = Optional.ofNullable(pictureService.selectMaxPictureId()).orElse(0L);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        long currentMinId = minId;
        while (currentMinId <= maxId) {
            long batchMinId = currentMinId;
            long batchMaxId = Math.min(currentMinId + SHARD_SIZE - 1, maxId);
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(
                    () -> processRecommendShard(batchMinId, batchMaxId, now), taskExecutor);
            futures.add(future);
            currentMinId = batchMaxId + 1;
        }

        long totalProcessed = 0;
        for (CompletableFuture<Long> future : futures) {
            try {
                totalProcessed += future.get();
            } catch (Exception e) {
                log.error("分片处理失败", e);
            }
        }
        log.info("全量并行处理完成：共处理{}张图片", totalProcessed);
    }

    /**
     * 顺序处理不分片数据
     */
    private void calculateRecommendScoresWithoutSharding(Instant now) {
        long totalProcessed = 0;
        long offset = 0;
        while (true) {
            List<PictureHotScoreDto> pictureDtos = pictureService.selectPictureScoreData(offset, PAGE_SIZE);
            if (pictureDtos.isEmpty()) break;

            List<Long> ids = pictureDtos.stream().map(PictureHotScoreDto::getId).collect(Collectors.toList());
            totalProcessed += processPictureDtoBatch(pictureDtos, pictureService.listByIds(ids), now);

            if (pictureDtos.size() < PAGE_SIZE) break;
            offset += PAGE_SIZE;
        }
        log.info("全量顺序处理完成：共处理{}张图片", totalProcessed);
    }

    /**
     * 处理单个分片
     */
    private long processRecommendShard(long minId, long maxId, Instant now) {
        long totalProcessed = 0;
        long currentOffset = 0;
        while (true) {
            List<PictureHotScoreDto> pictureDtos = pictureService.selectPictureScoreDataInRange(minId, maxId, currentOffset, PAGE_SIZE);
            if (pictureDtos.isEmpty()) break;

            List<Long> ids = pictureDtos.stream().map(PictureHotScoreDto::getId).collect(Collectors.toList());
            totalProcessed += processPictureDtoBatch(pictureDtos, pictureService.listByIds(ids), now);

            if (pictureDtos.size() < PAGE_SIZE) break;
            currentOffset += PAGE_SIZE;
        }
        return totalProcessed;
    }

    /**
     * 批量处理DTO数据
     */
    private long processPictureDtoBatch(List<PictureHotScoreDto> pictureDtos, List<Picture> pictures, Instant now) {
        List<Picture> picturesToUpdate = new ArrayList<>(BATCH_UPDATE_SIZE);
        for (int i = 0; i < pictureDtos.size(); i++) {
            Picture picture = pictures.get(i);
            Double cachedScore = getCachedScore(picture.getId());
            // 计算新分数（无随机）
            double newScore = cachedScore != null ? cachedScore : calculateScoreFromDto(picture, pictureDtos.get(i), now);
            // 分数平滑
            double smoothScore = smoothScore(picture.getRecommendScore(), newScore);

            picturesToUpdate.add(buildUpdatePicture(picture.getId(), smoothScore));
            if (picturesToUpdate.size() >= BATCH_UPDATE_SIZE) {
                batchUpdateScores(picturesToUpdate);
                picturesToUpdate.clear();
            }
        }
        if (!picturesToUpdate.isEmpty()) batchUpdateScores(picturesToUpdate);
        return pictureDtos.size();
    }

    /**
     * 批量处理Picture数据
     */
    private void processPictureBatch(List<Picture> pictures, Instant now, boolean isIncremental, boolean isNewPicture) {
        List<Picture> picturesToUpdate = new ArrayList<>(BATCH_UPDATE_SIZE);
        for (Picture picture : pictures) {
            Double cachedScore = isIncremental ? null : getCachedScore(picture.getId());
            // 计算新分数（无随机）
            double newScore = cachedScore != null ? cachedScore : calculateScore(picture, now, isNewPicture);
            // 分数平滑
            double smoothScore = smoothScore(picture.getRecommendScore(), newScore);

            picturesToUpdate.add(buildUpdatePicture(picture.getId(), smoothScore));
            if (picturesToUpdate.size() >= BATCH_UPDATE_SIZE) {
                batchUpdateScores(picturesToUpdate);
                picturesToUpdate.clear();
            }
        }
        if (!picturesToUpdate.isEmpty()) batchUpdateScores(picturesToUpdate);
    }

    /**
     * 分数平滑核心方法（关键：避免大幅波动）
     */
    private double smoothScore(Double oldScore, double newScore) {
        if (oldScore == null || oldScore <= 0) {
            return Math.max(newScore, MIN_SCORE);
        }
        // 平滑公式：旧分数占alpha权重，新分数占1-alpha权重
        double smoothScore = oldScore * smoothAlpha + newScore * (1 - smoothAlpha);
        // 限制单次分数变化幅度±10%
        double maxChange = oldScore * 0.1;
        if (smoothScore > oldScore + maxChange) {
            smoothScore = oldScore + maxChange;
        } else if (smoothScore < oldScore - maxChange) {
            smoothScore = oldScore - maxChange;
        }
        return Math.max(smoothScore, MIN_SCORE);
    }

    /**
     * 构建更新对象
     */
    private Picture buildUpdatePicture(Long id, double score) {
        Picture picture = new Picture();
        picture.setId(id);
        picture.setRecommendScore(score);
        return picture;
    }

    /**
     * 从DTO计算分数（完全无随机）
     */
    private double calculateScoreFromDto(Picture picture, PictureHotScoreDto dto, Instant now) {
        double timeScore = calculateTimeScore(picture.getCreateTime(), now);
        double baseScore = timeScore * timeWeight +
                Math.log1p(Optional.ofNullable(dto.getViewCount()).orElse(0L) * LOG_MULTIPLIER) * viewWeight +
                Math.log1p(Optional.ofNullable(dto.getLikeCount()).orElse(0L) * LOG_MULTIPLIER) * likeWeight +
                Math.log1p(Optional.ofNullable(dto.getCommentCount()).orElse(0L) * LOG_MULTIPLIER) * commentWeight +
                Math.log1p(Optional.ofNullable(dto.getShareCount()).orElse(0L) * LOG_MULTIPLIER) * shareWeight;

        long hours = ChronoUnit.HOURS.between(picture.getCreateTime().toInstant(), now);
        double timeBonus = hours <= newPictureHours
                ? newPictureBonus
                : Math.max(TIME_BONUS_BASE * Math.exp(-DECAY_RATE * hours), 1.0);

        double finalScore = baseScore * timeBonus;
        return Math.max(finalScore, MIN_SCORE);
    }

    /**
     * 计算单张图片分数（完全无随机）
     */
    private double calculateScore(Picture picture, Instant now, boolean isNewPicture) {
        double timeScore = calculateTimeScore(picture.getCreateTime(), now);
        long viewCount = Optional.ofNullable(picture.getViewCount()).orElse(0L);
        long likeCount = Optional.ofNullable(picture.getLikeCount()).orElse(0L);
        long commentCount = Optional.ofNullable(picture.getCommentCount()).orElse(0L);
        long shareCount = Optional.ofNullable(picture.getShareCount()).orElse(0L);

        double baseScore = timeScore * timeWeight +
                Math.log1p(viewCount * LOG_MULTIPLIER) * viewWeight +
                Math.log1p(likeCount * LOG_MULTIPLIER) * likeWeight +
                Math.log1p(commentCount * LOG_MULTIPLIER) * commentWeight +
                Math.log1p(shareCount * LOG_MULTIPLIER) * shareWeight;

        double timeBonus = isNewPicture
                ? newPictureBonus
                : Math.max(TIME_BONUS_BASE * Math.exp(-DECAY_RATE * ChronoUnit.HOURS.between(picture.getCreateTime().toInstant(), now)), 1.0);

        double finalScore = baseScore * timeBonus;
        return Math.max(finalScore, MIN_SCORE);
    }

    /**
     * 计算时间衰减分数
     */
    private double calculateTimeScore(java.util.Date createTime, Instant now) {
        long hours = ChronoUnit.HOURS.between(createTime.toInstant(), now);
        return Math.exp(-timeDecay * Math.cbrt(hours));
    }

    /**
     * 批量更新分数
     */
    private void batchUpdateScores(List<Picture> picturesToUpdate) {
        try {
            if (!pictureService.updateBatchRecommendScore(picturesToUpdate)) {
                log.error("批量更新失败，大小:{}", picturesToUpdate.size());
            }
        } catch (Exception e) {
            log.error("批量更新异常", e);
        }
    }

    /**
     * 获取缓存分数
     */
    private Double getCachedScore(Long pictureId) {
        String key = "picture:score:" + pictureId;
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Double.parseDouble(value) : null;
    }

    /**
     * 缓存分数（仅全量任务）
     */
    private void cacheScoreIfNeeded(Picture picture, double score, Instant now) {
        long hours = ChronoUnit.HOURS.between(picture.getCreateTime().toInstant(), now);
        if (hours > CACHE_THRESHOLD_HOURS) {
            stringRedisTemplate.opsForValue().set("picture:score:" + picture.getId(),
                    String.valueOf(score), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }
}
