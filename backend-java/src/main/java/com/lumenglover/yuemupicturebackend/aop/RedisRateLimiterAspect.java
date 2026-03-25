package com.lumenglover.yuemupicturebackend.aop;

import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisRateLimiterAspect {

    private static final String RATE_LIMIT_LUA_SCRIPT =
            "local key = KEYS[1] " +
                    "local limit = tonumber(ARGV[1]) " +
                    "local window = tonumber(ARGV[2]) " +
                    "if not limit or not window then " +
                    "    return -1 " +
                    "end " +
                    "local current = redis.call('get', key) " +
                    "local count = 0 " +
                    "if current then " +
                    "    count = tonumber(current) + 1 " +
                    "else " +
                    "    count = 1 " +
                    "end " +
                    "if count <= limit then " +
                    "    redis.call('setex', key, window, count) " +
                    "    return count " +
                    "else " +
                    "    return count " +
                    "end ";

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT =
            RedisScript.of(RATE_LIMIT_LUA_SCRIPT, Long.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    @Value("${rate-limit.anonymous-limit:512}")
    private int anonymousLimit;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint point, RateLimiter rateLimiter) throws Throwable {
        String baseKey = rateLimiter.key();
        int timeWindow = rateLimiter.time();
        int maxCount = rateLimiter.count();
        String limitMessage = rateLimiter.message();

        // 替换 isBlank() 为 Java 8 兼容逻辑：判断是否为 null/空字符串/全空格
        if (baseKey == null || baseKey.trim().isEmpty() || timeWindow <= 0 || maxCount <= 0) {
            log.warn("限流注解参数不合法：baseKey={}, timeWindow={}, maxCount={}", baseKey, timeWindow, maxCount);
            return point.proceed();
        }

        HttpServletRequest request = getHttpServletRequest(point);
        String userId = getUserId(request);
        String limitKey = buildSafeLimitKey(baseKey, userId);

        // 对于匿名用户，使用配置的匿名用户限流阈值
        int effectiveMaxCount = maxCount;
        if ("anonymous".equals(userId)) {
            effectiveMaxCount = anonymousLimit; // 匿名用户使用配置的限流阈值
        }

        try {
            Long result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(limitKey),
                    String.valueOf(effectiveMaxCount),
                    String.valueOf(timeWindow)
            );

            if (result == null) {
                log.error("Redis限流脚本执行返回空，key={}", limitKey);
                return point.proceed();
            }

            if (result == -1) {
                log.warn("Redis限流脚本参数错误，key={}, maxCount={}, timeWindow={}", limitKey, effectiveMaxCount, timeWindow);
                return point.proceed();
            }

            if (result > effectiveMaxCount) {
                log.info("用户触发限流，userId={}, key={}, 当前请求数={}, 限流阈值={}",
                        userId, limitKey, result, effectiveMaxCount);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, limitMessage);
            }

            log.debug("用户请求通过限流，userId={}, key={}, 当前请求数={}, 限流阈值={}",
                    userId, limitKey, result, effectiveMaxCount);
            return point.proceed();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis限流执行异常，key={}", limitKey, e);
            return point.proceed();
        }
    }

    private HttpServletRequest getHttpServletRequest(ProceedingJoinPoint point) {
        if (point.getArgs() == null || point.getArgs().length == 0) {
            return null;
        }
        for (Object arg : point.getArgs()) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }
        return null;
    }

    private String getUserId(HttpServletRequest request) {
        // 优先从传入的request参数获取，如果没有则从线程上下文中获取
        HttpServletRequest httpRequest = request;
        if (httpRequest == null) {
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    httpRequest = attributes.getRequest();
                }
            } catch (Exception e) {
                log.debug("从线程上下文获取HTTP请求失败", e);
            }
        }

        if (httpRequest == null) {
            return "anonymous";
        }

        try {
            User loginUser = userService.getLoginUser(httpRequest);
            return loginUser != null ? loginUser.getId().toString() : "anonymous";
        } catch (Exception e) {
            log.debug("获取登录用户失败，使用匿名标识", e);
            return "anonymous";
        }
    }

    private String buildSafeLimitKey(String baseKey, String userId) {
        // 兼容 baseKey 为 null 的情况
        String safeBaseKey = (baseKey == null) ? "" : baseKey.replaceAll("[^a-zA-Z0-9:_]", "_");
        String safeUserId = userId.replaceAll("[^a-zA-Z0-9:_]", "_");
        return RATE_LIMIT_KEY_PREFIX + safeBaseKey + ":" + safeUserId;
    }
}
