package com.lumenglover.yuemupicturebackend.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;

/**
 * 反爬管理
 */
@Component
@Slf4j
public class CrawlerManager {

    @Resource
    private CounterManager counterManager;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Value("${spring.mail.admin}")
    private String adminEmail;

    /**
     * 检测普通请求
     */
    public void detectNormalRequest(HttpServletRequest request) {
        detectRequest(request, CrawlerConstant.WARN_COUNT, CrawlerConstant.BAN_COUNT);
    }

    /**
     * 检测高频操作请求
     */
    public void detectFrequentRequest(HttpServletRequest request) {
        detectRequest(request, CrawlerConstant.WARN_COUNT / 2, CrawlerConstant.BAN_COUNT / 2);
    }

    /**
     * 检测下载请求
     */
    public void detectDownloadRequest(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String downloadKey = String.format("picture:download:%s", clientIp);
        long downloadCount = counterManager.incrAndGetCounter(downloadKey, 1, TimeUnit.HOURS);
        if (downloadCount > 50) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "下载次数超出限制");
        }
    }

    /**
     * 检测浏览请求
     */
    public boolean detectViewRequest(HttpServletRequest request, Long pictureId) {
        if (request == null) {
            return true;
        }
        String clientIp = getClientIpAddress(request);
        String ipViewKey = String.format("picture:view:ip:%s:%d", clientIp, pictureId);
        long ipViewCount = counterManager.incrAndGetCounter(ipViewKey, 30, TimeUnit.SECONDS);
        return ipViewCount <= 1;
    }

    /**
     * 通用请求检测
     */
    private void detectRequest(HttpServletRequest request, int warnCount, int banCount) {
        User loginUser = null;
        String key;
        String identifier;

        try {
            loginUser = userService.isLogin(request);
            if (loginUser != null) {
                Long loginUserId = loginUser.getId();
                identifier = String.valueOf(loginUserId);
                key = String.format("user:access:%s", loginUserId);
            } else {
                identifier = getClientIpAddress(request);
                key = String.format("ip:access:%s", identifier);
            }
        } catch (Exception e) {
            log.info("获取用户信息异常，默认为未登录用户");
            identifier = getClientIpAddress(request);
            key = String.format("ip:access:%s", identifier);
        }

        // 统计访问次数
        long count = counterManager.incrAndGetCounter(key, 3, TimeUnit.MINUTES, CrawlerConstant.EXPIRE_TIME);

        // 是否封禁或告警
        if (count > banCount) {
            if (loginUser != null) {
                // 管理员只记录警告日志，不进行封禁
                if (userService.isAdmin(loginUser)) {
                    log.warn("警告：管理员访问频繁, userId={}, count={}", loginUser.getId(), count);
                    return;
                }
                // 对于普通用户，封号并踢下线
                User updateUser = new User();
                updateUser.setId(loginUser.getId());
                updateUser.setUserRole(CrawlerConstant.BAN_ROLE);
                userService.updateById(updateUser);

                // 发送爬虫警告邮件
                try {
                    sendCrawlerWarningEmail(loginUser, identifier, count);
                } catch (Exception e) {
                    log.error("发送爬虫警告邮件失败", e);
                }

                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问次数过多，已被封号");
            } else {
                // 对于未登录用户，封禁 IP
                banIp(identifier);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问次数过多，IP 已被封禁");
            }
        } else if (count > warnCount) {
            // 记录警告日志
            if (loginUser != null) {
                log.warn("警告：用户访问频繁, userId={}, count={}", loginUser.getId(), count);
            } else {
                log.warn("警告：IP访问频繁, ip={}, count={}", identifier, count);
            }
        }
    }

    /**
     * 发送爬虫警告邮件
     */
    private void sendCrawlerWarningEmail(User user, String ipAddress, long accessCount) throws IOException {
        ClassPathResource resource = new ClassPathResource("html/crawler_warning.html");
        String template;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            template = content.toString();
        }

        // 替换模板中的变量
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String emailContent = template
                .replace(":userId", String.valueOf(user.getId()))
                .replace(":userAccount", user.getUserAccount())
                .replace(":userEmail", user.getEmail())
                .replace(":ipAddress", ipAddress)
                .replace(":accessCount", String.valueOf(accessCount))
                .replace(":detectTime", sdf.format(new Date()));

        // 发送邮件
        emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
    }

    /**
     * 封禁IP
     */
    private void banIp(String ip) {
        String banKey = String.format("ban:ip:%s", ip);
        stringRedisTemplate.opsForValue().set(banKey, "1", 24, TimeUnit.HOURS);
    }

    /**
     * 检查IP是否被封禁
     */
    private boolean isIpBanned(String ip) {
        String banKey = String.format("ban:ip:%s", ip);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(banKey));
    }

    /**
     * 获取客户端IP
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
