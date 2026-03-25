package com.lumenglover.yuemupicturebackend.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.mapper.UserMapper;
import com.lumenglover.yuemupicturebackend.mapper.UserSignInRecordMapper;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.UserSignInRecord;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;

@Component
@Slf4j
public class UserSignInSyncJob {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserSignInRecordMapper userSignInRecordMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 测试环境：0 *除1 * * * ?每2分钟执行一次同步任务
     * 生产环境: 0 0 2 1 * ? (每月1号凌晨2点执行)
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncUserSignInRecords() {
        try {
            log.info("开始同步用户签到记录到MySQL, 当前时间: {}", LocalDate.now());

            // 获取当前年份（改为同步当前年份的数据，因为用户正在使用的是当前年份）
            int year = LocalDate.now().getYear();
            log.info("开始同步 {} 年的签到记录", year);

            // 获取所有用户
            List<User> userList = userMapper.selectList(null);
            if (CollUtil.isEmpty(userList)) {
                log.info("没有需要同步的用户数据");
                return;
            }

            int totalUsers = userList.size();
            int processedUsers = 0;
            int totalRecords = 0;

            for (User user : userList) {
                try {
                    processedUsers++;
                    String redisKey = RedisConstant.getUserSignInRedisKey(year, user.getId());

                    RBitSet signInBitSet = redissonClient.getBitSet(redisKey);
                    if (!signInBitSet.isExists()) {
                        log.debug("用户ID: {} 在Redis中没有签到记录", user.getId());
                        continue;
                    }

                    // 获取该用户当年的签到记录
                    QueryWrapper<UserSignInRecord> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", user.getId())
                            .eq("year", year)
                            .eq("isDelete", 0);

                    UserSignInRecord record = userSignInRecordMapper.selectOne(queryWrapper);
                    boolean isNewRecord = false;

                    if (record == null) {
                        record = new UserSignInRecord();
                        record.setId(IdWorker.getId());
                        record.setUserId(user.getId());
                        record.setYear(year);
                        record.setSignInData(new byte[46]); // 初始化366天的bitmap
                        isNewRecord = true;
                        log.info("为用户ID: {} 创建新的签到记录", user.getId());
                    }

                    // 将Redis中的bitmap数据同步到MySQL
                    BitSet bitSet = signInBitSet.asBitSet();
                    byte[] signInData = record.getSignInData();
                    int userRecords = 0;

                    for (int i = 1; i <= 366; i++) {
                        if (bitSet.get(i)) {
                            int byteIndex = (i - 1) / 8;
                            int bitIndex = (i - 1) % 8;
                            signInData[byteIndex] |= (1 << bitIndex);
                            userRecords++;
                            totalRecords++;
                        }
                    }

                    if (userRecords > 0) {
                        record.setSignInData(signInData);
                        try {
                            if (isNewRecord) {
                                userSignInRecordMapper.insert(record);
                                log.info("用户ID: {} 插入新的签到记录，共 {} 条签到", user.getId(), userRecords);
                            } else {
                                userSignInRecordMapper.updateById(record);
                                log.info("用户ID: {} 更新签到记录，共 {} 条签到", user.getId(), userRecords);
                            }
                        } catch (Exception e) {
                            log.error("保存用户ID: {} 的签到记录失败: {}", user.getId(), e.getMessage(), e);
                            throw e;
                        }
                    }
                } catch (Exception e) {
                    log.error("处理用户ID: {} 的签到记录时发生错误: {}", user.getId(), e.getMessage(), e);
                    throw e; // 抛出异常以触发事务回滚
                }
            }

            log.info("{} 年签到记录同步完成, 总处理用户数: {}, 总同步记录数: {}",
                    year, totalUsers, totalRecords);
        } catch (Exception e) {
            log.error("签到记录同步任务执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
