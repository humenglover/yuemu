package com.lumenglover.yuemupicturebackend.constant;

/**
 * 推荐系统相关常量
 */
public interface RecommendConstant {

    /**
     * 用户帖子行为权重缓存键
     */
    String USER_POST_BEHAVIOR_KEY = "user:post:behavior:%d";

    /**
     * 帖子相似度缓存键
     */
    String POST_SIMILARITY_KEY = "post:similarity:%d";

    /**
     * 用户个性化推荐帖子缓存键
     */
    String USER_POST_RECOMMEND_KEY = "recommend:post:user:%d";

    /**
     * 热门帖子推荐缓存键
     */
    String HOT_POST_RECOMMEND_KEY = "recommend:post:hot";

    /**
     * 用户已互动帖子集合缓存键
     */
    String USER_POST_INTERACTED_KEY = "user:post:interacted:%d";

    /**
     * 推荐分数更新队列 Redis key
     */
    String RECOMMEND_SCORE_UPDATE_QUEUE_KEY = "recommend_score:update_queue";

    /**
     * 获取用户帖子行为权重缓存键
     * @param userId 用户ID
     * @return Redis键
     */
    static String getUserPostBehaviorKey(long userId) {
        return String.format(USER_POST_BEHAVIOR_KEY, userId);
    }

    /**
     * 获取帖子相似度缓存键
     * @param postId 帖子ID
     * @return Redis键
     */
    static String getPostSimilarityKey(long postId) {
        return String.format(POST_SIMILARITY_KEY, postId);
    }

    /**
     * 获取用户个性化推荐帖子缓存键
     * @param userId 用户ID
     * @return Redis键
     */
    static String getUserPostRecommendKey(long userId) {
        return String.format(USER_POST_RECOMMEND_KEY, userId);
    }

    /**
     * 获取用户已互动帖子集合缓存键
     * @param userId 用户ID
     * @return Redis键
     */
    static String getUserPostInteractedKey(long userId) {
        return String.format(USER_POST_INTERACTED_KEY, userId);
    }
}
