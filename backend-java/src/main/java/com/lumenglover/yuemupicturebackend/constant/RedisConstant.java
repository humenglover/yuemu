package com.lumenglover.yuemupicturebackend.constant;

/**
 * Redis 常量
 */
public interface RedisConstant {

    /**
     * 用户签到记录的 Redis key 前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * top100
     */
    String TOP_100_PIC_REDIS_KEY_PREFIX = "top100Picture:";

    /**
     * top100过期时间为1天
     */
    long TOP_100_PIC_REDIS_KEY_EXPIRE_TIME =  24 * 60 * 60;

    /**
     * 公共图库前置
     */
    String PUBLIC_PIC_REDIS_KEY_PREFIX = "yuemupicture:listPictureVOByPage:";

    /**
     * 帖子top100
     */
    String TOP_100_POST_REDIS_KEY_PREFIX = "top100Post:";

    /**
     * 帖子top100过期时间为1天
     */
    long TOP_100_POST_REDIS_KEY_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * 空间聊天记录缓存前缀
     */
    String SPACE_CHAT_HISTORY_PREFIX = "chat:space:";

    /**
     * 图片聊天记录缓存前缀
     */
    String PICTURE_CHAT_HISTORY_PREFIX = "chat:picture:";

    /**
     * 私聊记录缓存前缀
     */
    String PRIVATE_CHAT_HISTORY_PREFIX = "chat:private:";

    /**
     * 聊天记录缓存过期时间（30分钟）
     */
    long CHAT_HISTORY_EXPIRE_TIME = 30 * 60;

    /**
     * 帖子分页数据缓存前缀
     */
    String POST_PAGE_CACHE_PREFIX = "post:page:";


    /**
     * 帖子缓存过期时间（1小时）
     */
    long POST_CACHE_EXPIRE_TIME = 60 * 60;

    /**
     * 友链缓存前缀
     */
    String FRIEND_LINK_REDIS_KEY_PREFIX = "friend_link:list:";

    /**
     * 友链缓存过期时间（秒）
     */
    long FRIEND_LINK_REDIS_KEY_EXPIRE_TIME = 300;

    /**
     * 获取用户签到记录的 Redis Key
     * @param year 年份
     * @param userId 用户 id
     * @return 拼接好的 Redis Key
     */
    static String getUserSignInRedisKey(int year, long userId) {
        return String.format("%s:%s:%S", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }

    String WEBSITE_STATISTICS_REDIS_KEY_PREFIX = "website_stats:";

    /**
     * 网站总访问量 Redis key
     */
    String TOTAL_VIEW_COUNT_KEY = "website:total_views";

    /**
     * 热榜分数更新队列 Redis key
     */
    String HOT_SCORE_UPDATE_QUEUE_KEY = "hot_score:update_queue";

    /**
     * 推荐分数更新队列 Redis key
     */
    String RECOMMEND_SCORE_UPDATE_QUEUE_KEY = "recommend_score:update_queue";

    /**
     * 公共空间图片推荐排序 ZSet key
     */
    String PUBLIC_PICTURE_RECOMMEND_SORT_KEY = "public:picture:recommend";

    /**
     * 公共空间图片热度排序 ZSet key
     */
    String PUBLIC_PICTURE_HOT_SORT_KEY = "public:picture:hot";

    /**
     * 公共空间图片时间排序 ZSet key
     */
    String PUBLIC_PICTURE_TIME_SORT_KEY = "public:picture:time";

    /**
     * 公共空间图片总数量 key
     */
    String PUBLIC_PICTURE_TOTAL_COUNT_KEY = "public:picture:%s:total";

    /**
     * 公共空间图片版本号 key
     */
    String PUBLIC_PICTURE_VERSION_KEY = "public:picture:%s:version";

    /**
     * 公共空间图片基础信息 Hash key
     */
    String PUBLIC_PICTURE_BASE_INFO_KEY = "picture:base:%d";

    /**
     * 公共空间图片同步锁 key
     */
    String PUBLIC_PICTURE_SYNC_LOCK_KEY = "public:picture:sync:lock";
}
