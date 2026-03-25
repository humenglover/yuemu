package com.lumenglover.yuemupicturebackend.job;

import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.model.dto.picture.PictureHotScoreDto;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.utils.PictureScoreUpdateTracker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片热榜分数计算任务
 * 该任务负责计算图片的热榜分数，不偏向新内容
 */
@Slf4j
@Component
public class PictureHotScoreJob implements CommandLineRunner {

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

    private static final int BATCH_SIZE = 50; // 每个小批次的大小
    private static final int PAGE_SIZE = 1000; // 分页查询大小

    /**
     * 程序启动时异步执行一次热榜分数计算
     */
    @Override
    public void run(String... args) {
        log.info("程序启动，开始异步执行首次热榜分数计算...");
        CompletableFuture.runAsync(this::calculateHotScores, taskExecutor);
    }

    /**
     * 每日凌晨2点执行一次热榜分数计算
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void calculateHotScoresScheduled() {
        log.info("定时任务：开始计算图片热榜分数...");
        calculateHotScores();
    }

    /**
     * 计算图片的热榜分数
     * 优先处理增量更新队列，若队列为空则处理全量数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculateHotScores() {
        RLock lock = redissonClient.getLock("picture:hot_score:calculate_lock");
        try {
            // 尝试获取分布式锁，等待10秒，持有300秒
            if (lock.tryLock(10, 300, TimeUnit.SECONDS)) {
                log.info("获取到分布式锁，开始计算图片热榜分数...");

                // 先尝试处理增量更新队列
                long queueSize = pictureScoreUpdateTracker.getHotScoreQueueSize();
                if (queueSize > 0) {
                    log.info("发现 {} 个待更新的图片，开始增量计算...", queueSize);
                    calculateHotScoresIncrementally();
                } else {
                    log.info("增量更新队列为空，开始全量计算...");
                    calculateHotScoresFull();
                }
            } else {
                log.warn("未能获取到分布式锁，跳过本次热榜分数计算");
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("计算热榜分数时发生异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.info("已释放分布式锁");
                } catch (Exception e) {
                    log.error("释放分布式锁失败", e);
                }
            }
        }
    }

    /**
     * 增量计算热榜分数
     */
    private void calculateHotScoresIncrementally() {
        // 获取待更新的图片ID
        Set<String> pictureIds = pictureScoreUpdateTracker.getPictureIdsForHotScoreUpdate();
        if (pictureIds == null || pictureIds.isEmpty()) {
            log.info("增量更新队列为空，无需计算");
            return;
        }

        log.info("开始处理 {} 个待更新的图片ID", pictureIds.size());

        // 将图片ID转换为Long类型列表
        List<Long> ids = pictureIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 分批处理
        for (int i = 0; i < ids.size(); i += PAGE_SIZE) {
            int endIndex = Math.min(i + PAGE_SIZE, ids.size());
            List<Long> batchIds = ids.subList(i, endIndex);

            // 查询需要更新的图片数据
            List<PictureHotScoreDto> pictureDtos = pictureService.listByIds(batchIds).stream()
                    .filter(p -> p.getSpaceId() == null && p.getReviewStatus() == 1 && p.getIsDraft() == 0) // 只处理公共空间、审核通过且非草稿状态的图片
                    .map(p -> {
                        PictureHotScoreDto dto = new PictureHotScoreDto();
                        dto.setId(p.getId());
                        dto.setViewCount(p.getViewCount());
                        dto.setLikeCount(p.getLikeCount());
                        dto.setCommentCount(p.getCommentCount());
                        dto.setShareCount(p.getShareCount());
                        return dto;
                    })
                    .collect(Collectors.toList());

            if (!pictureDtos.isEmpty()) {
                // 计算热榜分数
                List<Picture> picturesToUpdate = new ArrayList<>();
                for (PictureHotScoreDto dto : pictureDtos) {
                    double hotScore = calculateHotScore(dto);
                    Picture picture = new Picture();
                    picture.setId(dto.getId());
                    picture.setHotScore(hotScore);
                    picturesToUpdate.add(picture);
                }

                // 批量更新热榜分数
                boolean updateResult = pictureService.updateBatchHotScore(picturesToUpdate);
                if (!updateResult) {
                    log.error("批量更新热榜分数失败，批次大小: {}", picturesToUpdate.size());
                } else {
                    log.info("成功更新 {} 个图片的热榜分数", picturesToUpdate.size());
                }
            }
        }

        // 从更新队列中移除已处理的图片ID
        pictureScoreUpdateTracker.removePicturesFromHotScoreUpdateQueue(pictureIds);
        log.info("增量计算完成，已从更新队列中移除 {} 个图片ID", pictureIds.size());
    }

    /**
     * 全量计算热榜分数
     */
    private void calculateHotScoresFull() {
        log.info("开始全量计算图片热榜分数...");

        // 获取总图片数量
        long totalPictures = pictureService.countPictureScoreData();
        log.info("总共需要处理 {} 张图片", totalPictures);

        // 如果图片数量较多，启用分片处理
        if (totalPictures > 10000) {  // 如果超过1万张图片，使用分片处理
            calculateHotScoresWithSharding(totalPictures);
        } else {
            calculateHotScoresWithoutSharding();
        }
    }

    /**
     * 使用分片的方式计算热榜分数
     * 按图片ID范围分片，可以并行处理
     */
    private void calculateHotScoresWithSharding(long totalPictures) {
        log.info("图片数量较多({})，启用分片处理模式", totalPictures);

        // 计算分片数量，每个分片处理约5000张图片
        int shardSize = 5000;
        long minId = 0;
        long maxId = getMaxPictureId();

        log.info("开始按ID范围分片处理，最大ID: {}", maxId);

        long totalProcessed = 0;

        while (minId <= maxId) {
            // 计算当前分片的最大ID
            long currentMaxId = Math.min(minId + shardSize, maxId);

            log.info("处理分片: ID范围 [{} - {}]", minId, currentMaxId);

            // 处理当前分片
            long processedInShard = processShard(minId, currentMaxId);
            totalProcessed += processedInShard;

            log.info("分片 [{} - {}] 处理完成，本分片处理 {} 张，累计处理 {} 张",
                    minId, currentMaxId, processedInShard, totalProcessed);

            minId = currentMaxId + 1;  // 移动到下一个分片

            // 为了避免影响数据库性能，在分片之间短暂休眠
            try {
                Thread.sleep(100);  // 休眠100毫秒
            } catch (InterruptedException e) {
                log.warn("分片处理被中断");
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("分片计算完成，共处理 {} 张图片", totalProcessed);
    }

    /**
     * 顺序处理不分片的计算
     */
    private void calculateHotScoresWithoutSharding() {
        log.info("图片数量较少，使用顺序处理模式");
        long totalProcessed = 0;
        long offset = 0;

        while (true) {
            // 分页查询用于计算热榜分数的图片数据
            List<PictureHotScoreDto> pictureDtos = pictureService.selectPictureScoreData(offset, PAGE_SIZE).stream()
                    .filter(dto -> {
                        // 获取完整的图片实体以检查空间、审核状态和草稿状态
                        Picture picture = pictureService.getById(dto.getId());
                        return picture != null && picture.getSpaceId() == null &&
                                picture.getReviewStatus() == 1 && picture.getIsDraft() == 0;
                    })
                    .collect(Collectors.toList());

            if (pictureDtos.isEmpty()) {
                break;
            }

            // 计算每张图片的热榜分数
            List<Picture> picturesToUpdate = new ArrayList<>();
            for (PictureHotScoreDto dto : pictureDtos) {
                double hotScore = calculateHotScore(dto);
                Picture picture = new Picture();
                picture.setId(dto.getId());
                picture.setHotScore(hotScore);
                picturesToUpdate.add(picture);
            }

            // 分批更新热榜分数
            boolean updateResult = pictureService.updateBatchHotScore(picturesToUpdate);
            if (!updateResult) {
                log.error("批量更新热榜分数失败，批次大小: {}", picturesToUpdate.size());
            }

            totalProcessed += pictureDtos.size();
            log.info("已处理 {} 张图片的热榜分数", totalProcessed);

            if (pictureDtos.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }

        log.info("顺序计算完成，共处理 {} 张图片", totalProcessed);
    }

    /**
     * 获取最大的图片ID
     */
    private long getMaxPictureId() {
        try {
            Long maxId = pictureService.selectMaxPictureId();
            return maxId != null ? maxId : 0L;
        } catch (Exception e) {
            log.warn("获取最大图片ID失败，使用默认值", e);
            return 0L;
        }
    }

    /**
     * 处理一个分片（ID范围内的图片）
     */
    private long processShard(long minId, long maxId) {
        long totalProcessed = 0;
        long currentOffset = 0;

        while (true) {
            // 查询指定ID范围内的图片数据
            List<PictureHotScoreDto> pictureDtos = pictureService.selectPictureScoreDataInRange(minId, maxId, currentOffset, PAGE_SIZE).stream()
                    .filter(dto -> {
                        // 获取完整的图片实体以检查空间、审核状态和草稿状态
                        Picture picture = pictureService.getById(dto.getId());
                        return picture != null && picture.getSpaceId() == null &&
                                picture.getReviewStatus() == 1 && picture.getIsDraft() == 0;
                    })
                    .collect(Collectors.toList());

            if (pictureDtos.isEmpty()) {
                break;
            }

            // 计算每张图片的热榜分数
            List<Picture> picturesToUpdate = new ArrayList<>();
            for (PictureHotScoreDto dto : pictureDtos) {
                double hotScore = calculateHotScore(dto);
                Picture picture = new Picture();
                picture.setId(dto.getId());
                picture.setHotScore(hotScore);
                picturesToUpdate.add(picture);
            }

            // 分批更新热榜分数
            boolean updateResult = pictureService.updateBatchHotScore(picturesToUpdate);
            if (!updateResult) {
                log.error("批量更新热榜分数失败，批次大小: {}", picturesToUpdate.size());
            }

            totalProcessed += pictureDtos.size();

            if (pictureDtos.size() < PAGE_SIZE) {
                break;
            }

            currentOffset += PAGE_SIZE;
        }

        return totalProcessed;
    }

    /**
     * 计算单张图片的热榜分数
     * 使用对数函数减少新老内容之间的差距，使排名更公平
     */
    private double calculateHotScore(PictureHotScoreDto dto) {
        // 使用对数函数来平衡各个指标的影响，避免新内容因为时间短而分数低
        // 对数函数可以减少绝对数值差异，让排名更依赖于相对比例
        double viewScore = Math.log1p((dto.getViewCount() != null ? dto.getViewCount() : 0) * 0.3);
        double likeScore = Math.log1p((dto.getLikeCount() != null ? dto.getLikeCount() : 0) * 0.4);
        double commentScore = Math.log1p((dto.getCommentCount() != null ? dto.getCommentCount() : 0) * 0.5);
        double shareScore = Math.log1p((dto.getShareCount() != null ? dto.getShareCount() : 0) * 0.6);

        return viewScore + likeScore + commentScore + shareScore;
    }
}
