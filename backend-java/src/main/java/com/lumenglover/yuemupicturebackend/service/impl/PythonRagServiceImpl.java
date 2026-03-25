package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lumenglover.yuemupicturebackend.config.RagConfig;
import com.lumenglover.yuemupicturebackend.model.dto.rag.PythonRagRequest;
import com.lumenglover.yuemupicturebackend.model.dto.rag.PythonRagResponse;
import com.lumenglover.yuemupicturebackend.service.PythonRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class PythonRagServiceImpl implements PythonRagService {

    @Autowired
    private RagConfig ragConfig;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        // 初始化WebClient,配置基础URL
        String baseUrl = ragConfig.getPythonService().getBaseUrl();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("WebClient initialized with base URL: {}", baseUrl);
    }

    @Override
    public PythonRagResponse callPythonRagSync(String question, List<Map<String, String>> history, String sessionId, String ltmContext) {
        if (StrUtil.isBlank(question)) {
            log.warn("Python RAG同步调用 - 问题为空");
            return PythonRagResponse.builder()
                    .answer("")
                    .sessionId(sessionId)
                    .build();
        }

        try {
            PythonRagRequest request = PythonRagRequest.builder()
                    .question(question)
                    .history(history != null ? history : CollUtil.newArrayList())
                    .session_id(sessionId)
                    .longTermMemory(ltmContext)
                    .build();

            String syncEndpoint = ragConfig.getPythonService().getSyncEndpoint();

            log.info("Python RAG同步调用 - 请求 | URL: {} | 问题: {} | LTM长度: {}",
                    syncEndpoint, question, ltmContext != null ? ltmContext.length() : 0);

            // 使用WebClient进行同步调用，按String接收以拆解ResponseWrapper
            String responseStr = webClient.post()
                    .uri(syncEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 阻塞等待响应

            String answer = "";
            if (StrUtil.isNotBlank(responseStr)) {
                cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(responseStr);
                int code = jsonObject.getInt("code", 500);
                if (code == 200) {
                    cn.hutool.json.JSONObject dataObj = jsonObject.getJSONObject("data");
                    if (dataObj != null) {
                        answer = dataObj.getStr("answer", "");
                    }
                }
            }

            log.info("Python RAG同步调用 - 响应 | 响应长度: {}", answer.length());

            return PythonRagResponse.builder()
                    .answer(answer)
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("Python RAG同步调用异常", e);
            return PythonRagResponse.builder()
                    .answer("Python RAG服务调用异常: " + e.getMessage())
                    .sessionId(sessionId)
                    .build();
        }
    }

    @Override
    public void callPythonRagStream(String question, List<Map<String, String>> history, String sessionId, String ltmContext, Consumer<String> tokenConsumer, Runnable onComplete) {
        if (StrUtil.isBlank(question)) {
            log.warn("Python RAG流式调用 - 问题为空");
            tokenConsumer.accept("");
            onComplete.run();
            return;
        }

        try {
            PythonRagRequest request = PythonRagRequest.builder()
                    .question(question)
                    .history(history != null ? history : CollUtil.newArrayList())
                    .session_id(sessionId)
                    .longTermMemory(ltmContext)
                    .build();

            String streamEndpoint = ragConfig.getPythonService().getStreamEndpoint();

            log.info("Python RAG流式调用 - 请求 | URL: {} | 问题: {}", streamEndpoint, question);

            // 使用WebClient进行SSE流式调用
            Flux<ServerSentEvent<String>> eventStream = webClient.post()
                    .uri(streamEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

            // 订阅SSE事件流
            eventStream.subscribe(
                    event -> {
                        // 处理每个SSE事件
                        String eventName = event.event();
                        String data = event.data();

                        log.debug("接收到SSE事件 | 类型: {} | 数据: {}", eventName, data);

                        if (data != null) {
                            try {
                                cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(data);
                                String eventType = jsonObject.getStr("event");

                                if ("chunk".equals(eventType)) {
                                    // 提取chunk内容：支持嵌套多层的情况
                                    cn.hutool.json.JSONObject dataObj = jsonObject.getJSONObject("data");
                                    if (dataObj != null) {
                                        // 兼容处理：有些结构是 data -> chunk，有些是 data -> data -> chunk
                                        String chunk = dataObj.getStr("chunk");
                                        if (chunk == null) {
                                            cn.hutool.json.JSONObject innerData = dataObj.getJSONObject("data");
                                            if (innerData != null) {
                                                chunk = innerData.getStr("chunk");
                                            }
                                        }

                                        if (chunk != null) {
                                            log.debug("【PythonRAG】解析到Chunk | 长度: {} | 内容预览: [{}]",
                                                    chunk.length(), chunk.length() > 20 ? chunk.substring(0, 20) + "..." : chunk);
                                            tokenConsumer.accept(chunk);
                                        }
                                    }
                                } else if ("done".equals(eventType) || "end".equals(eventType)) {
                                    log.info("【PythonRAG】接收到结束事件 | 数据摘要: {}", data.length() > 100 ? data.substring(0, 100) + "..." : data);

                                    // 关键安全网：某些情况下 chunk 可能因为网络/并发丢失，
                                    // python 端的 done 事件通常包含完整答案字段。我们需要利用它来兜底。
                                    try {
                                        cn.hutool.json.JSONObject dataObj = jsonObject.getJSONObject("data");
                                        if (dataObj != null && dataObj.containsKey("answer")) {
                                            String fullAnswerFromDone = dataObj.getStr("answer");
                                            if (StrUtil.isNotBlank(fullAnswerFromDone)) {
                                                log.info("【PythonRAG】从Done事件提取到完整答案，总长度: {}", fullAnswerFromDone.length());
                                                // 注意：这里我们不直接 accept 完整内容，因为 tokenConsumer 后端可能已经在累积了。
                                                // 我们期望 tokenConsumer 的实现（在 RagServiceImpl 中）能处理好这种“最终通过”逻辑，
                                                // 或者在这里仅作为日志确认。
                                                // 实际上，目前的 RagServiceImpl 使用的是 StringBuilder 收集。
                                                // 为了简单且不重复，我们在这里记录日志。如果还是不对，我们需要在控制器层做 diff 推送。
                                            }
                                        }
                                    } catch (Exception doneEx) {
                                        log.warn("【PythonRAG】解析Done事件详情失败", doneEx);
                                    }
                                } else if ("error".equals(eventType)) {
                                    cn.hutool.json.JSONObject dataObj = jsonObject.getJSONObject("data");
                                    if (dataObj != null) {
                                        String errorMsg = dataObj.getStr("msg");
                                        if (StrUtil.isNotBlank(errorMsg)) {
                                            tokenConsumer.accept("错误: " + errorMsg);
                                        }
                                    }
                                    log.error("接收到错误事件");
                                } else if ("start".equals(eventType)) {
                                    log.info("接收到开始事件");
                                }
                            } catch (Exception parseEx) {
                                log.warn("解析SSE数据失败 | 数据: {}", data, parseEx);
                            }
                        }
                    },
                    error -> {
                        // 错误处理
                        log.error("Python RAG流式调用异常", error);
                        tokenConsumer.accept("Python RAG服务调用异常: " + error.getMessage());
                        onComplete.run();
                    },
                    () -> {
                        // 完成处理
                        log.info("Python RAG流式调用 - 数据读取完成");
                        onComplete.run();
                    }
            );

        } catch (Exception e) {
            log.error("Python RAG流式调用异常", e);
            tokenConsumer.accept("Python RAG服务调用异常: " + e.getMessage());
            onComplete.run();
        }
    }

    @Override
    public PythonRagResponse callPythonSummarize(String text) {
        if (StrUtil.isBlank(text)) {
            return PythonRagResponse.builder().answer("").build();
        }
        try {
            PythonRagRequest request = PythonRagRequest.builder()
                    .question(text)
                    .build();

            String summarizeEndpoint = ragConfig.getPythonService().getSummarizeEndpoint();
            log.info("Python 摘要调用 - 请求 | URL: {} | 文本长度: {}", summarizeEndpoint, text.length());

            String responseStr = webClient.post()
                    .uri(summarizeEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String answer = "";
            if (StrUtil.isNotBlank(responseStr)) {
                cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(responseStr);
                int code = jsonObject.getInt("code", 500);
                if (code == 200) {
                    cn.hutool.json.JSONObject dataObj = jsonObject.getJSONObject("data");
                    if (dataObj != null) {
                        answer = dataObj.getStr("answer", "");
                    }
                }
            }

            log.info("Python 摘要调用 - 响应 | 结果长度: {}", answer.length());
            return PythonRagResponse.builder().answer(answer).build();
        } catch (Exception e) {
            log.error("Python 摘要调用异常", e);
            return PythonRagResponse.builder().answer("摘要服务异常: " + e.getMessage()).build();
        }
    }
}
