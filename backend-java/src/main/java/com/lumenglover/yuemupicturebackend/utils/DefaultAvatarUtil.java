package com.lumenglover.yuemupicturebackend.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 默认头像工具类
 * 用于管理用户注册时的默认头像分配
 */
public class DefaultAvatarUtil {

    /**
     * 前端定义的默认头像URL列表
     */
    private static final List<String> DEFAULT_AVATAR_URLS = Arrays.asList(
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-11-35.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-11-58.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-04.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-09.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-13.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-19.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-23.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-28.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-32.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-42.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-52.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-12-58.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-07.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-11.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-16.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-20.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-25.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-29.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-41.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-46.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-13-53.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-04.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-08.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-14.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-24.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-35.png",
        "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-14-44.png"
    );

    private static final Random RANDOM = new Random();

    /**
     * 随机获取一个默认头像URL
     *
     * @return 随机的头像URL
     */
    public static String getRandomAvatar() {
        if (DEFAULT_AVATAR_URLS.isEmpty()) {
            // 如果列表为空，返回一个默认的头像URL
            return "https://static.yuemutuku.com/avatar%2FSnipaste_2025-05-04_22-11-35.png";
        }

        int randomIndex = RANDOM.nextInt(DEFAULT_AVATAR_URLS.size());
        return DEFAULT_AVATAR_URLS.get(randomIndex);
    }

    /**
     * 获取默认头像列表的大小
     *
     * @return 头像列表大小
     */
    public static int getAvatarCount() {
        return DEFAULT_AVATAR_URLS.size();
    }

    /**
     * 获取指定索引的头像URL
     *
     * @param index 索引
     * @return 头像URL
     */
    public static String getAvatarByIndex(int index) {
        if (index < 0 || index >= DEFAULT_AVATAR_URLS.size()) {
            return getRandomAvatar(); // 如果索引超出范围，返回随机头像
        }
        return DEFAULT_AVATAR_URLS.get(index);
    }
}
