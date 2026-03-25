package com.lumenglover.yuemupicturebackend.job;

import com.lumenglover.yuemupicturebackend.service.KnowledgeFileService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 知识库文件数据同步任务
 * 应用启动时自动执行一次数据一致性检查，定期维护两端数据同步
 */
@Slf4j
@Component
public class KnowledgeFileSyncJob implements CommandLineRunner {

    @Resource
    private KnowledgeFileService knowledgeFileService;

    @Resource
    private RedissonClient redissonClient;

    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    /**
     * 程序启动时异步执行一次知识库数据同步
     */
    @Override
    public void run(String... args) {
        log.info("程序启动，开始异步执行知识库数据一致性检查...");
        CompletableFuture.runAsync(this::syncKnowledgeFilesOnStartup, taskExecutor);
    }

    /**
     * 每日凌晨3点执行一次知识库数据同步（备用定时任务）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncKnowledgeFilesScheduled() {
        log.info("定时任务：开始执行知识库数据同步...");
        syncKnowledgeFiles();
    }

    /**
     * 应用启动时的数据同步
     * 带有重试机制和详细日志
     */
    @Async("taskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void syncKnowledgeFilesOnStartup() {
        log.info("=== 知识库数据同步任务启动 ===");
        log.info("开始执行Java和Python知识库数据一致性检查...");

        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;

        while (retryCount < maxRetries && !success) {
            try {
                retryCount++;
                log.info("第 {} 次尝试同步知识库数据...", retryCount);

                success = performSyncOperation();

                if (success) {
                    log.info("✅ 知识库数据同步成功完成！");
                } else {
                    log.warn("❌ 第 {} 次同步尝试失败", retryCount);
                    if (retryCount < maxRetries) {
                        log.info("等待 {} 秒后进行下次重试...", retryCount * 10);
                        Thread.sleep(retryCount * 10000); // 递增延迟重试
                    }
                }

            } catch (InterruptedException e) {
                log.error("同步任务被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("第 {} 次同步尝试发生异常", retryCount, e);
                if (retryCount < maxRetries) {
                    try {
                        log.info("等待 {} 秒后进行下次重试...", retryCount * 10);
                        Thread.sleep(retryCount * 10000);
                    } catch (InterruptedException ie) {
                        log.error("重试等待被中断", ie);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!success) {
            log.error("❌ 知识库数据同步最终失败，已重试 {} 次", maxRetries);
        }

        log.info("=== 知识库数据同步任务结束 ===");
    }

    /**
     * 定时执行的数据同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncKnowledgeFiles() {
        RLock lock = redissonClient.getLock("knowledge:sync:lock");
        try {
            // 尝试获取分布式锁，等待30秒，持有300秒
            if (lock.tryLock(30, 300, TimeUnit.SECONDS)) {
                log.info("获取到分布式锁，开始执行知识库数据同步...");
                performSyncOperation();
            } else {
                log.warn("未能获取到分布式锁，跳过本次知识库数据同步");
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("执行知识库数据同步时发生异常", e);
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
     * 执行实际的同步操作
     *
     * @return 同步是否成功
     */
    private boolean performSyncOperation() {
        try {
            log.info("开始调用知识库同步服务...");

            // 调用Service层的同步方法
            boolean result = knowledgeFileService.syncKnowledgeFiles();

            if (result) {
                log.info("知识库同步服务调用成功");
                return true;
            } else {
                log.error("知识库同步服务返回失败");
                return false;
            }

        } catch (Exception e) {
            log.error("调用知识库同步服务发生异常", e);
            return false;
        }
    }
}
