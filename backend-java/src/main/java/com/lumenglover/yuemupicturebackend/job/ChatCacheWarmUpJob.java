package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.mapper.ChatMessageMapper;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import com.lumenglover.yuemupicturebackend.service.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cn.hutool.core.util.RandomUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChatCacheWarmUpJob {

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 每天凌晨2点执行缓存预热
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void warmUpChatCache() {
        try {
            log.info("开始聊天记录缓存预热任务");

            // 获取一个月前的时间
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            Date oneMonthAgo = calendar.getTime();

            // 获取最近一个月的活跃聊天
            warmUpPrivateChats(oneMonthAgo);
            warmUpPictureChats(oneMonthAgo);
            warmUpSpaceChats(oneMonthAgo);

            log.info("聊天记录缓存预热任务完成");
        } catch (Exception e) {
            log.error("聊天记录缓存预热任务失败", e);
        }
    }

    private void warmUpPrivateChats(Date oneMonthAgo) {
        // 获取最近一个月有聊天记录的私聊ID列表
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT privateChatId")
                .ge("createTime", oneMonthAgo)
                .eq("type", 1)
                .eq("isDelete", 0)
                .isNotNull("privateChatId");

        List<ChatMessage> messages = chatMessageMapper.selectList(queryWrapper);
        Set<Long> privateChatIds = messages.stream()
                .map(ChatMessage::getPrivateChatId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 为每个私聊预热最近的几页数据
        for (Long privateChatId : privateChatIds) {
            try {
                warmUpRecentPages(privateChatId, RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX);
            } catch (Exception e) {
                log.error("私聊记录预热失败, privateChatId: {}", privateChatId, e);
            }
        }
    }

    private void warmUpPictureChats(Date oneMonthAgo) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT pictureId")
                .ge("createTime", oneMonthAgo)
                .eq("type", 2)
                .eq("isDelete", 0)
                .isNotNull("pictureId");

        List<ChatMessage> messages = chatMessageMapper.selectList(queryWrapper);
        Set<Long> pictureIds = messages.stream()
                .map(ChatMessage::getPictureId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long pictureId : pictureIds) {
            try {
                warmUpRecentPages(pictureId, RedisConstant.PICTURE_CHAT_HISTORY_PREFIX);
            } catch (Exception e) {
                log.error("图片评论预热失败, pictureId: {}", pictureId, e);
            }
        }
    }

    private void warmUpSpaceChats(Date oneMonthAgo) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT spaceId")
                .ge("createTime", oneMonthAgo)
                .eq("type", 3)
                .eq("isDelete", 0)
                .isNotNull("spaceId");

        List<ChatMessage> messages = chatMessageMapper.selectList(queryWrapper);
        Set<Long> spaceIds = messages.stream()
                .map(ChatMessage::getSpaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long spaceId : spaceIds) {
            try {
                warmUpRecentPages(spaceId, RedisConstant.SPACE_CHAT_HISTORY_PREFIX);
            } catch (Exception e) {
                log.error("空间聊天记录预热失败, spaceId: {}", spaceId, e);
            }
        }
    }

    /**
     * 预热最近的几页数据
     */
    private void warmUpRecentPages(Long id, String prefix) throws Exception {
        // 只预热前3页数据
        for (int i = 1; i <= 3; i++) {
            String cacheKey = prefix + id + ":" + i + ":20"; // 假设每页20条

            // 如果缓存已存在，跳过
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey))) {
                continue;
            }

            // 根据不同类型获取对应的聊天记录
            Object page;
            if (prefix.equals(RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX)) {
                page = chatMessageService.getPrivateChatHistory(id, i, 20);
            } else if (prefix.equals(RedisConstant.PICTURE_CHAT_HISTORY_PREFIX)) {
                page = chatMessageService.getPictureChatHistory(id, i, 20);
            } else {
                page = chatMessageService.getSpaceChatHistory(id, i, 20);
            }

            // 设置缓存，添加随机过期时间防止缓存雪崩
            stringRedisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(page),
                RedisConstant.CHAT_HISTORY_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                TimeUnit.SECONDS
            );
        }
    }
}
