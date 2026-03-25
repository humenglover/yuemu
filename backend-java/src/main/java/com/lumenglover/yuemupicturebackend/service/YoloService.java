package com.lumenglover.yuemupicturebackend.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lumenglover.yuemupicturebackend.config.RagConfig;
import com.lumenglover.yuemupicturebackend.model.dto.yolo.YoloResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YoloService {

    @Autowired
    private RagConfig ragConfig;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        // 初始化 WebClient，配置基础 URL 并增加缓冲区限制 (10MB)
        String baseUrl = ragConfig.getPythonService().getBaseUrl();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        log.info("YoloService WebClient initialized with base URL: {}", baseUrl);
    }

    /**
     * 调用 Python YOLO 服务进行目标检测（文件上传）
     */
    public YoloResponseDTO detectObjects(MultipartFile file) throws Exception {
        String detectEndpoint = ragConfig.getPythonService().getYolo().getDetectEndpoint();
        log.info("Calling Python YOLO API (WebClient): {}", detectEndpoint);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // 关键：ByteArrayResource 需要重写 getFilename() 或者在 builder 中指定文件名，以便 FastAPI 识别为文件
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();

        builder.part("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        }).contentType(MediaType.IMAGE_JPEG);

        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        String response = webClient.post()
                .uri(detectEndpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(multipartBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(response);
    }

    /**
     * 调用 Python YOLO 服务进行 URL 图片目标检测
     */
    public YoloResponseDTO detectObjectsFromUrl(String imageUrl) throws Exception {
        String detectUrlEndpoint = ragConfig.getPythonService().getYolo().getDetectUrlEndpoint();
        log.info("Detecting objects from URL (WebClient): {}", imageUrl);

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(detectUrlEndpoint)
                        .queryParam("url", imageUrl)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(response);
    }

    /**
     * 解析 Python 服务的响应
     */
    private YoloResponseDTO parseResponse(String jsonResponse) {
        log.info("YOLO API response: {}", jsonResponse);
        if (StrUtil.isBlank(jsonResponse)) {
            throw new RuntimeException("YOLO API returned empty response");
        }

        JSONObject jsonObject = JSON.parseObject(jsonResponse);
        // FastAPI 校验错误处理
        if (jsonObject.containsKey("detail")) {
            throw new RuntimeException("YOLO API Validation Error: " + jsonObject.getString("detail"));
        }

        if (jsonObject.getInteger("code") == null || jsonObject.getInteger("code") != 200) {
            String msg = jsonObject.getString("message");
            if (msg == null) msg = jsonResponse;
            throw new RuntimeException("YOLO detection failed: " + msg);
        }
        JSONObject data = jsonObject.getJSONObject("data");
        return data.toJavaObject(YoloResponseDTO.class);
    }

    /**
     * 获取检测到的所有标签（去重）
     */
    public List<String> getLabels(MultipartFile file) throws Exception {
        YoloResponseDTO response = detectObjects(file);
        if (response == null || response.getDetections() == null) {
            return List.of();
        }
        return response.getDetections().stream()
                .map(YoloResponseDTO.YoloDetection::getLabel)
                .distinct()
                .collect(Collectors.toList());
    }
}
