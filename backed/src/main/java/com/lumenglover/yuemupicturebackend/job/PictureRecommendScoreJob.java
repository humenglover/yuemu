package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class PictureRecommendScoreJob implements CommandLineRunner {

    @Resource
    private PictureService pictureService;

    // 权重配置
    @Value("${recommend.score.view:0.15}")
    private double viewWeight;

    @Value("${recommend.score.like:0.15}")
    private double likeWeight;

    @Value("${recommend.score.comment:0.15}")
    private double commentWeight;

    @Value("${recommend.score.share:0.10}")
    private double shareWeight;

    @Value("${recommend.score.time-weight:0.30}")
    private double timeWeight;

    @Value("${recommend.score.time-decay:0.1}")
    private double timeDecay;

    /**
     * 程序启动时执行一次
     */
    @Override
    public void run(String... args) {
        log.info("程序启动，开始执行首次推荐分数计算...");
        calculateRecommendScores();
    }

    /**
     * 每5分钟执行一次推荐分数计算
     */
    @Scheduled(fixedRate = 300000)
    public void calculateRecommendScores() {
        log.info("开始计算图片推荐分数...");
        int pageSize = 100;
        long current = 1;
        long totalProcessed = 0;

        while (true) {
            // 分页查询所有已审核通过的图片
            Page<Picture> page = pictureService.page(
                    new Page<>(current, pageSize),
                    new LambdaQueryWrapper<Picture>()
                            .eq(Picture::getReviewStatus, 1)
                            .eq(Picture::getIsDelete, 0)
            );

            List<Picture> pictures = page.getRecords();
            if (pictures.isEmpty()) {
                break;
            }

            // 计算每张图片的推荐分数
            for (Picture picture : pictures) {
                double score = calculateScore(picture);
                picture.setRecommendScore(score);
            }

            // 批量更新推荐分数
            pictureService.updateBatchById(pictures);

            totalProcessed += pictures.size();
            log.info("已处理 {}/{} 张图片的推荐分数", totalProcessed, page.getTotal());

            if (pictures.size() < pageSize) {
                break;
            }
            current++;
        }

        log.info("图片推荐分数计算完成，共处理 {} 张图片", totalProcessed);
    }

    /**
     * 计算单张图片的推荐分数
     */
    private double calculateScore(Picture picture) {
        // 时间衰减分数
        double timeScore = calculateTimeScore(picture.getCreateTime());

        // 互动数据标准化，使用更温和的对数转换
        double viewScore = Math.log1p(picture.getViewCount() * 0.6);
        double likeScore = Math.log1p(picture.getLikeCount() * 0.6);
        double commentScore = Math.log1p(picture.getCommentCount() * 0.6);
        double shareScore = Math.log1p(picture.getShareCount() * 0.6);

        // 加权求和，新内容获得额外的时间加成
        double baseScore = timeScore * timeWeight +
                viewScore * viewWeight +
                likeScore * likeWeight +
                commentScore * commentWeight +
                shareScore * shareWeight;

        // 根据内容发布时间给予不同的加成
        long hours = ChronoUnit.HOURS.between(picture.getCreateTime().toInstant(), Instant.now());
        if (hours <= 6) {
            // 6小时内的内容获得3倍的分数加成
            baseScore *= 3.0;
        } else if (hours <= 12) {
            // 12小时内的内容获得2.5倍的分数加成
            baseScore *= 2.5;
        } else if (hours <= 24) {
            // 24小时内的内容获得2倍的分数加成
            baseScore *= 2.0;
        } else if (hours <= 48) {
            // 48小时内的内容获得1.5倍的分数加成
            baseScore *= 1.5;
        }

        return baseScore;
    }

    /**
     * 计算时间衰减分数
     */
    private double calculateTimeScore(java.util.Date createTime) {
        long hours = ChronoUnit.HOURS.between(createTime.toInstant(), Instant.now());
        // 使用更平缓的衰减曲线，使用立方根而不是平方根
        return Math.exp(-timeDecay * Math.cbrt(hours));
    }
}
