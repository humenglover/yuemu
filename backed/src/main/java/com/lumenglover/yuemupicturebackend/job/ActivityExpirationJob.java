package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 活动过期检查定时任务
 */
@Component
@Slf4j
public class ActivityExpirationJob {

    @Resource
    private ActivityService activityService;

    /**
     * 每天凌晨1点执行过期检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkExpiredActivities() {
        try {
            log.info("开始检查过期活动");

            // 更新所有已过期但未标记的活动
            UpdateWrapper<Activity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("isExpired", 0)  // 未标记过期的
                    .lt("expireTime", new Date())  // 过期时间早于当前时间
                    .set("isExpired", 1);  // 标记为已过期

            boolean success = activityService.update(updateWrapper);

            log.info("过期活动检查完成，更新状态：{}", success);
        } catch (Exception e) {
            log.error("过期活动检查任务失败", e);
        }
    }
}
