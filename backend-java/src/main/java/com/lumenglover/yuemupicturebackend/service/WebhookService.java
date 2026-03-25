package com.lumenglover.yuemupicturebackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenglover.yuemupicturebackend.config.WebhookConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class WebhookService {

    @Resource
    private WebhookConfig webhookConfig;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 异步发送Webhook通知
     * @param eventType 事件类型
     * @param data 事件数据
     */
    public void sendWebhookAsync(String eventType, Object data) {
        if (!webhookConfig.isEnabled() || webhookConfig.getCallbackUrls() == null || webhookConfig.getCallbackUrls().isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            sendWebhook(eventType, data);
        });
    }

    /**
     * 发送Webhook通知
     * @param eventType 事件类型
     * @param data 事件数据
     */
    private void sendWebhook(String eventType, Object data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构造请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 向所有配置的URL发送请求
        for (String url : webhookConfig.getCallbackUrls()) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                log.info("Webhook发送成功，URL: {}, 状态码: {}", url, response.getStatusCode());
            } catch (Exception e) {
                log.error("Webhook发送失败，URL: {}", url, e);
            }
        }
    }
}
