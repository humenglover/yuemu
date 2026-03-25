package com.lumenglover.yuemupicturebackend.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * AI服务限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * 限流key，支持SpEL表达式
     */
    String key() default "ai_rate_limit:";

    /**
     * 限流时间范围，单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 15;

    /**
     * 限流时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 超出限制时的提示信息
     */
    String message() default "访问过于频繁，请稍后再试";
}
