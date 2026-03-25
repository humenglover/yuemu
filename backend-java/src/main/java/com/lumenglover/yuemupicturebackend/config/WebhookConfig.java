package com.lumenglover.yuemupicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookConfig {
    /**
     * Webhook回调URL列表
     */
    private List<String> callbackUrls;

    /**
     * 是否启用Webhook
     */
    private boolean enabled = false;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 5000;
}
