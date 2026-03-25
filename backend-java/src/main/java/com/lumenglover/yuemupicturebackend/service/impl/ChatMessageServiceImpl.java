package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.ChatMessageMapper;
import com.lumenglover.yuemupicturebackend.model.dto.chatmessage.ChatMessageAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.chatmessage.ChatMessageBatchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.ChatMessageService;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.SqlUtils;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public ChatMessageServiceImpl() {
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以处理日期时间
        objectMapper.registerModule(new JavaTimeModule());
        // 配置日期时间的序列化格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public Page<ChatMessage> getUserChatHistory(long userId, long otherUserId, long current, long size) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                        .or(w -> w.eq("senderId", userId).eq("receiverId", otherUserId))
                        .or(w -> w.eq("senderId", otherUserId).eq("receiverId", userId)))
                .eq("type", 1)
                .orderByDesc("createTime");

        Page<ChatMessage> page = this.page(new Page<>(current, size), queryWrapper);
        page.getRecords().forEach(this::fillMessageInfo);
        return page;
    }

    @Override
    public Page<ChatMessage> getPrivateChatHistory(long privateChatId, long current, long size) {
        String cacheKey = RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX + privateChatId + ":" + current + ":" + size;

        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (Exception e) {
                log.error("Failed to deserialize chat history from cache", e);
            }
        }

        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("privateChatId", privateChatId)
                .eq("type", 1)
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        Page<ChatMessage> page = this.page(new Page<>(current, size), queryWrapper);
        page.getRecords().forEach(this::fillMessageInfo);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(page),
                    RedisConstant.CHAT_HISTORY_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to serialize chat history to cache", e);
        }

        return page;
    }

    @Override
    public Page<ChatMessage> getPictureChatHistory(long pictureId, long current, long size) {
        String cacheKey = RedisConstant.PICTURE_CHAT_HISTORY_PREFIX + pictureId + ":" + current + ":" + size;

        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (Exception e) {
                log.error("Failed to deserialize chat history from cache", e);
            }
        }

        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureId)
                .eq("type", 2)  // 图片评论类型
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        Page<ChatMessage> page = this.page(new Page<>(current, size), queryWrapper);
        page.getRecords().forEach(this::fillMessageInfo);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(page),
                    RedisConstant.CHAT_HISTORY_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to serialize chat history to cache", e);
        }

        return page;
    }

    @Override
    public void markAsRead(long receiverId, long senderId) {
        this.update()
                .set("status", 1)
                .eq("receiverId", receiverId)
                .eq("senderId", senderId)
                .eq("status", 0)
                .update();
    }

    @Override
    public List<ChatMessage> getMessageReplies(long messageId) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("replyId", messageId)
                .orderByAsc("createTime");
        List<ChatMessage> replies = this.list(queryWrapper);
        replies.forEach(this::fillMessageInfo);
        return replies;
    }

    @Override
    public List<ChatMessage> getMessageThread(long messageId) {
        ChatMessage message = this.getById(messageId);
        if (message == null) {
            return null;
        }
        Long rootId = message.getRootId() != null ? message.getRootId() : messageId;

        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rootId", rootId)
                .or()
                .eq("id", rootId)
                .orderByAsc("createTime");

        List<ChatMessage> thread = this.list(queryWrapper);
        thread.forEach(this::fillMessageInfo);
        return thread;
    }

    @Override
    public Page<ChatMessage> getSpaceChatHistory(long spaceId, long current, long size) {
        String cacheKey = RedisConstant.SPACE_CHAT_HISTORY_PREFIX + spaceId + ":" + current + ":" + size;

        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (Exception e) {
                log.error("Failed to deserialize chat history from cache", e);
            }
        }

        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId)
                .eq("type", 3)  // 空间聊天类型
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        Page<ChatMessage> page = this.page(new Page<>(current, size), queryWrapper);
        page.getRecords().forEach(this::fillMessageInfo);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(page),
                    RedisConstant.CHAT_HISTORY_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to serialize chat history to cache", e);
        }

        return page;
    }

    @Override
    public boolean canUserChatInSpace(long userId, long spaceId) {
        return spaceUserService.isSpaceMember(userId, spaceId);
    }

    @Override
    public List<User> getSpaceMembers(long spaceId) {
        return spaceUserService.getSpaceMembers(spaceId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage reply(ChatMessage message, long replyToMessageId) {
        // 获取被回复的消息
        ChatMessage replyToMessage = this.getById(replyToMessageId);
        if (replyToMessage == null) {
            throw new RuntimeException("回复的消息不存在");
        }

        // 检查空间权限
        if (message.getSpaceId() != null) {
            if (!canUserChatInSpace(message.getSenderId(), message.getSpaceId())) {
                throw new RuntimeException("您不是该空间的成员，无法发送消息");
            }
        }

        // 设置回复消息的关联信息
        message.setReplyId(replyToMessageId);
        message.setRootId(replyToMessage.getRootId() != null ? replyToMessage.getRootId() : replyToMessageId);

        // 保存消息
        this.save(message);

        // 填充消息信息
        fillMessageInfo(message);
        return message;
    }

    @Override
    public void fillMessageInfo(ChatMessage message) {
        // 处理历史数据的消息类型和URL
        if (message.getMessageType() == null || message.getMessageType().isEmpty()) {
            message.setMessageType("text");  // 默认设置为文本类型
        }
        if (message.getMessageUrl() == null && !"text".equals(message.getMessageType())) {
            message.setMessageUrl("unknown");  // 非文本消息如果没有URL，设置为未知
        }
        if (message.getMessageSize() == null) {
            message.setMessageSize(0L);  // 如果没有大小信息，设置为0
        }
        if (message.getMessageLocation() == null) {
            message.setMessageLocation("未知位置");  // 如果没有位置信息，设置为未知位置
        }

        // 填充发送者信息
        User sender = userService.getById(message.getSenderId());
        if (sender != null) {
            // 清除敏感信息
            sender.setUserPassword(null);
            // 替换URL为自定义域名
            sender.setUserAvatar(VoUrlReplaceUtil.replaceUrl(sender.getUserAvatar()));
            message.setSender(sender);
        }

        // 如果是回复消息，填充被回复的消息信息
        if (message.getReplyId() != null) {
            ChatMessage replyMessage = this.getById(message.getReplyId());
            if (replyMessage != null) {
                // 递归填充回复消息的信息，但要避免无限递归
                if (replyMessage.getReplyId() == null) {
                    fillMessageInfo(replyMessage);
                } else {
                    // 只填充基本信息
                    User replySender = userService.getById(replyMessage.getSenderId());
                    if (replySender != null) {
                        replySender.setUserPassword(null);
                        // 替换URL为自定义域名
                        replySender.setUserAvatar(VoUrlReplaceUtil.replaceUrl(replySender.getUserAvatar()));
                        replyMessage.setSender(replySender);
                    }
                    // 处理回复消息的类型和URL
                    if (replyMessage.getMessageType() == null || replyMessage.getMessageType().isEmpty()) {
                        replyMessage.setMessageType("text");
                    }
                    if (replyMessage.getMessageUrl() == null && !"text".equals(replyMessage.getMessageType())) {
                        replyMessage.setMessageUrl("unknown");
                    }
                    if (replyMessage.getMessageSize() == null) {
                        replyMessage.setMessageSize(0L);
                    }
                }
                message.setReplyMessage(replyMessage);
            }
        }
    }

    /**
     * 清除相关的聊天记录缓存
     */
    private void clearChatHistoryCache(ChatMessage message) {
        if (message.getSpaceId() != null) {
            // 清除空间聊天缓存
            String pattern = RedisConstant.SPACE_CHAT_HISTORY_PREFIX + message.getSpaceId() + ":*";
            clearCacheByPattern(pattern);
        }

        if (message.getPictureId() != null) {
            // 清除图片评论缓存
            String pattern = RedisConstant.PICTURE_CHAT_HISTORY_PREFIX + message.getPictureId() + ":*";
            clearCacheByPattern(pattern);
        }

        if (message.getPrivateChatId() != null) {
            // 清除私聊记录缓存
            String pattern = RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX + message.getPrivateChatId() + ":*";
            clearCacheByPattern(pattern);
        }
    }

    /**
     * 根据pattern清除缓存
     */
    private void clearCacheByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 在保存或更新消息后清除对应图片的聊天数缓存
     */
    private void clearPictureChatCountCache(ChatMessage message) {
        if (message != null && message.getPictureId() != null) {
            String chatCountKey = String.format("picture:chatCount:%d", message.getPictureId());
            stringRedisTemplate.delete(chatCountKey);
        }
    }

    /**
     * 重写保存方法，在保存消息后清除相关缓存
     */
    @Override
    public boolean save(ChatMessage message) {
        boolean result = super.save(message);
        if (result) {
            clearChatHistoryCache(message);
            clearPictureChatCountCache(message);
        }
        return result;
    }

    /**
     * 重写更新方法，在更新消息后清除相关缓存
     */
    @Override
    public boolean updateById(ChatMessage message) {
        boolean result = super.updateById(message);
        if (result) {
            clearChatHistoryCache(message);
            clearPictureChatCountCache(message);
        }
        return result;
    }

    /**
     * 获取管理员查询条件构造器
     */
    public QueryWrapper<ChatMessage> getAdminQueryWrapper(ChatMessageAdminRequest chatMessageAdminRequest) {
        if (chatMessageAdminRequest == null) {
            return null;
        }
        Long id = chatMessageAdminRequest.getId();
        Long senderId = chatMessageAdminRequest.getSenderId();
        Long receiverId = chatMessageAdminRequest.getReceiverId();
        String content = chatMessageAdminRequest.getContent();
        Integer type = chatMessageAdminRequest.getType();
        Integer status = chatMessageAdminRequest.getStatus();
        Long spaceId = chatMessageAdminRequest.getSpaceId();
        Long pictureId = chatMessageAdminRequest.getPictureId();
        Long privateChatId = chatMessageAdminRequest.getPrivateChatId();
        String sortField = chatMessageAdminRequest.getSortField();
        String sortOrder = chatMessageAdminRequest.getSortOrder();

        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(senderId != null, "senderId", senderId);
        queryWrapper.eq(receiverId != null, "receiverId", receiverId);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.eq(type != null, "type", type);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(spaceId != null, "spaceId", spaceId);
        queryWrapper.eq(pictureId != null, "pictureId", pictureId);
        queryWrapper.eq(privateChatId != null, "privateChatId", privateChatId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 批量操作聊天消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationMessages(ChatMessageBatchRequest chatMessageBatchRequest) {
        List<Long> ids = chatMessageBatchRequest.getIds();
        String operation = chatMessageBatchRequest.getOperation();

        if (operation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 根据操作类型执行相应的批量操作
        boolean result = false;
        switch (operation) {
            case "delete":
                result = this.removeByIds(ids);
                break;
            case "recover":
                result = this.update()
                        .set("isDelete", 0)
                        .in("id", ids)
                        .update();
                break;
            case "physical":
                result = this.removeBatchByIds(ids);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return result;
    }
}
