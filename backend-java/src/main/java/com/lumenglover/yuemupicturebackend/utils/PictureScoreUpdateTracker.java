package com.lumenglover.yuemupicturebackend.utils;

import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 图片分数更新追踪器
 * 用于维护需要重新计算各种分数（热榜分数、推荐分数）的图片ID列表
 */
@Slf4j
@Component
public class PictureScoreUpdateTracker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加需要重新计算热榜分数的图片ID
     * @param pictureId 图片ID
     */
    public void addPictureToHotScoreUpdateQueue(Long pictureId) {
        try {
            String key = RedisConstant.HOT_SCORE_UPDATE_QUEUE_KEY;
            stringRedisTemplate.opsForSet().add(key, pictureId.toString());
            // 设置过期时间为24小时，避免队列无限增长
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
            log.debug("Added pictureId {} to hot score update queue", pictureId);
        } catch (Exception e) {
            log.error("Failed to add pictureId {} to hot score update queue", pictureId, e);
        }
    }

    /**
     * 添加需要重新计算推荐分数的图片ID
     * @param pictureId 图片ID
     */
    public void addPictureToRecommendScoreUpdateQueue(Long pictureId) {
        try {
            String key = RedisConstant.RECOMMEND_SCORE_UPDATE_QUEUE_KEY;
            stringRedisTemplate.opsForSet().add(key, pictureId.toString());
            // 设置过期时间为24小时，避免队列无限增长
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
            log.debug("Added pictureId {} to recommend score update queue", pictureId);
        } catch (Exception e) {
            log.error("Failed to add pictureId {} to recommend score update queue", pictureId, e);
        }
    }

    /**
     * 获取需要重新计算热榜分数的图片ID集合
     * @return 图片ID集合
     */
    public Set<String> getPictureIdsForHotScoreUpdate() {
        try {
            String key = RedisConstant.HOT_SCORE_UPDATE_QUEUE_KEY;
            Set<String> result = stringRedisTemplate.opsForSet().members(key);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to get picture IDs from hot score update queue", e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取需要重新计算推荐分数的图片ID集合
     * @return 图片ID集合
     */
    public Set<String> getPictureIdsForRecommendScoreUpdate() {
        try {
            String key = RedisConstant.RECOMMEND_SCORE_UPDATE_QUEUE_KEY;
            Set<String> result = stringRedisTemplate.opsForSet().members(key);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to get picture IDs from recommend score update queue", e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取需要重新计算推荐分数的图片ID集合（限制数量）
     * @param limit 限制数量
     * @return 图片ID集合
     */
    public Set<String> getPictureIdsForRecommendScoreUpdateLimit(int limit) {
        try {
            String key = RedisConstant.RECOMMEND_SCORE_UPDATE_QUEUE_KEY;
            // 使用 SRANDMEMBER 命令随机获取指定数量的成员
            Set<String> allMembers = stringRedisTemplate.opsForSet().members(key);
            if (allMembers == null || allMembers.isEmpty()) {
                return Collections.emptySet();
            }

            // 如果总数少于限制，则返回全部
            if (allMembers.size() <= limit) {
                return allMembers;
            }

            // 否则随机选取指定数量的元素
            List<String> allMembersList = new ArrayList<>(allMembers);
            Collections.shuffle(allMembersList);
            return new HashSet<>(allMembersList.subList(0, limit));
        } catch (Exception e) {
            log.error("Failed to get limited picture IDs from recommend score update queue", e);
            return Collections.emptySet();
        }
    }

    /**
     * 从热榜分数更新队列中移除指定的图片ID
     * @param pictureIds 图片ID集合
     */
    public void removePicturesFromHotScoreUpdateQueue(Set<String> pictureIds) {
        try {
            String key = RedisConstant.HOT_SCORE_UPDATE_QUEUE_KEY;
            if (pictureIds != null && !pictureIds.isEmpty()) {
                stringRedisTemplate.opsForSet().remove(key, pictureIds.toArray());
                log.debug("Removed {} picture IDs from hot score update queue", pictureIds.size());
            }
        } catch (Exception e) {
            log.error("Failed to remove picture IDs from hot score update queue", e);
        }
    }

    /**
     * 从推荐分数更新队列中移除指定的图片ID
     * @param pictureIds 图片ID集合
     */
    public void removePicturesFromRecommendScoreUpdateQueue(Set<String> pictureIds) {
        try {
            String key = RedisConstant.RECOMMEND_SCORE_UPDATE_QUEUE_KEY;
            if (pictureIds != null && !pictureIds.isEmpty()) {
                stringRedisTemplate.opsForSet().remove(key, pictureIds.toArray());
                log.debug("Removed {} picture IDs from recommend score update queue", pictureIds.size());
            }
        } catch (Exception e) {
            log.error("Failed to remove picture IDs from recommend score update queue", e);
        }
    }

    /**
     * 获取热榜分数更新队列中图片的数量
     * @return 队列中的图片数量
     */
    public Long getHotScoreQueueSize() {
        try {
            String key = RedisConstant.HOT_SCORE_UPDATE_QUEUE_KEY;
            return stringRedisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("Failed to get hot score update queue size", e);
            return 0L;
        }
    }

    /**
     * 获取推荐分数更新队列中图片的数量
     * @return 队列中的图片数量
     */
    public Long getRecommendScoreQueueSize() {
        try {
            String key = RedisConstant.RECOMMEND_SCORE_UPDATE_QUEUE_KEY;
            return stringRedisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("Failed to get recommend score update queue size", e);
            return 0L;
        }
    }

    /**
     * 获取所有需要更新分数的图片总数
     * @return 需要更新的图片总数
     */
    public Long getTotalQueueSize() {
        return getHotScoreQueueSize() + getRecommendScoreQueueSize();
    }
}
