package com.lumenglover.yuemupicturebackend.config;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求包装过滤器
 *
 * @author pine
 */
@Order(1)
@Component
@Slf4j
public class HttpRequestWrapperFilter implements Filter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            String contentType = servletRequest.getHeader(Header.CONTENT_TYPE.getValue());

            // 统计网站总访问量
            incrementTotalViewCount();

            if (ContentType.JSON.getValue().equals(contentType)) {
                chain.doFilter(new RequestWrapper(servletRequest), response);
            } else {
                chain.doFilter(request, response);
            }
        }
    }

    /**
     * 增加网站总访问量
     */
    private void incrementTotalViewCount() {
        try {
            stringRedisTemplate.opsForValue().increment(RedisConstant.TOTAL_VIEW_COUNT_KEY);
        } catch (Exception e) {
            log.error("增加网站总访问量失败", e);
        }
    }

}
