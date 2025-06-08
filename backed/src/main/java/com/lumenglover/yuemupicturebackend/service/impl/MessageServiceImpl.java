package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.MessageMapper;
import com.lumenglover.yuemupicturebackend.model.dto.message.AddMessage;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Message;
import com.lumenglover.yuemupicturebackend.model.vo.MessageVO;
import com.lumenglover.yuemupicturebackend.service.MessageService;
import com.lumenglover.yuemupicturebackend.utils.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 鹿梦
 * @description 针对表【message(留言板表)】的数据库操作Service实现
 * @createDate 2025-01-03 16:28:14
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RateLimiter rateLimiter;

    private static final String MESSAGE_CACHE_KEY = "message:top500";
    private static final long CACHE_EXPIRE_TIME = 5; // 缓存过期时间（分钟）

    @Override
    public Boolean addMessage(AddMessage addMessage) {
        // 检查频率限制
        if (!rateLimiter.allowMessageAdd(addMessage.getIp())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送太频繁，请稍后再试");
        }

        Message message = new Message();
        message.setContent(addMessage.getContent());
        message.setIp(addMessage.getIp());
        boolean success = this.save(message);

        // 如果添加成功，删除缓存
        if (success) {
            redisTemplate.delete(MESSAGE_CACHE_KEY);
        }

        return success;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MessageVO> getTop500() {
        // 检查频率限制
        if (!rateLimiter.allowMessageQuery("system")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询太频繁，请稍后再试");
        }

        // 尝试从缓存获取
        List<MessageVO> cachedMessages = (List<MessageVO>) redisTemplate.opsForValue().get(MESSAGE_CACHE_KEY);
        if (cachedMessages != null) {
            return cachedMessages;
        }

        // 缓存未命中，从数据库获取
        List<MessageVO> messages = this.baseMapper.getTop500();

        // 存入缓存
        redisTemplate.opsForValue().set(MESSAGE_CACHE_KEY, messages, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

        return messages;
    }

    @Override
    public Page<Message> page(MessageQueryRequest messageQueryRequest) {
        // 检查频率限制
        if (!rateLimiter.allowMessageQuery("system")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询太频繁，请稍后再试");
        }

        long current = messageQueryRequest.getCurrent();
        long size = messageQueryRequest.getPageSize();
        Page<Message> messagePage = new Page<>(current, size);
        QueryWrapper<Message> queryWrapper = this.getQueryWrapper(messageQueryRequest);
        return this.page(messagePage, queryWrapper);
    }

    @Override
    public QueryWrapper<Message> getQueryWrapper(MessageQueryRequest messageQueryRequest) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        if (messageQueryRequest == null) {
            return queryWrapper;
        }

        String content = messageQueryRequest.getContent();
        String ip = messageQueryRequest.getIp();
        String sortField = messageQueryRequest.getSortField();
        String sortOrder = messageQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.eq(StringUtils.isNotBlank(ip), "ip", ip);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean deleteMessage(long id) {
        // 判断是否存在
        Message message = this.getById(id);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean success = this.removeById(id);

        // 如果删除成功，删除缓存
        if (success) {
            redisTemplate.delete(MESSAGE_CACHE_KEY);
        }

        return success;
    }
}




