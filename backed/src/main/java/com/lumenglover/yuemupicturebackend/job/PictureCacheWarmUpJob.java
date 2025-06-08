package com.lumenglover.yuemupicturebackend.job;

import cn.hutool.core.util.RandomUtil;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cn.hutool.json.JSONUtil;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PictureCacheWarmUpJob {

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 定时更新图片榜单数据
     * 每半小时执行一次 (0分和30分)
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void warmUpTop100Cache() {
        log.info("开始更新图片榜单数据");
        try {
            // 预热四种榜单：日榜、周榜、月榜、总榜
            for (long i = 1; i <= 4; i++) {
                String cacheKey = RedisConstant.TOP_100_PIC_REDIS_KEY_PREFIX + i;

                List<PictureVO> pictureVOList = pictureService.getTop100Picture(i);

                // 更新缓存，设置过期时间为35分钟（比更新间隔多5分钟）
                int cacheExpireTime = 35 * 60 + RandomUtil.randomInt(0, 300);
                stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    JSONUtil.toJsonStr(pictureVOList),
                    cacheExpireTime,
                    TimeUnit.SECONDS
                );

                log.info("图片{}榜单更新成功，数据量: {}", getRankName(i), pictureVOList.size());
            }
            log.info("图片榜单数据更新完成");
        } catch (Exception e) {
            log.error("图片榜单数据更新失败", e);
        }
    }

    /**
     * 获取榜单名称
     */
    private String getRankName(long rankId) {
        switch ((int) rankId) {
            case 1:
                return "日";
            case 2:
                return "周";
            case 3:
                return "月";
            case 4:
                return "总";
            default:
                return "未知";
        }
    }
}
