package com.lumenglover.yuemupicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    private Retrieval retrieval = new Retrieval();
    private Augmentation augmentation = new Augmentation();
    private Generation generation = new Generation();
    private PythonService pythonService = new PythonService();

    @Data
    public static class Retrieval {
        /**
         * 向量检索TOP数量
         */
        private Integer topK = 3;

        /**
         * 余弦相似度阈值（0-1）
         */
        private Double similarityThreshold = 0.4;
    }

    @Data
    public static class Augmentation {
        /**
         * 最大保留上下文轮数
         */
        private Integer maxContextRounds = 5;

        /**
         * Prompt最大字符数
         */
        private Integer maxPromptLength = 2000;
    }

    @Data
    public static class Generation {
        /**
         * 大模型温度（降低随机性）
         */
        private Double temperature = 0.6;

        /**
         * 生成最大Token数
         */
        private Integer maxTokens = 1000;
    }

    @Data
    public static class PythonService {
        /**
         * 是否启用Python RAG服务
         */
        private Boolean enabled = true;

        /**
         * Python RAG服务基础URL
         */
        private String baseUrl = "http://127.0.0.1:8001";

        /**
         * 请求超时时间（毫秒）
         */
        private Long timeout = 300000L;

        /**
         * 同步接口端点
         */
        private String syncEndpoint = "/api/rag/sync";

        /**
         * 流式接口端点
         */
        private String streamEndpoint = "/api/rag/stream";

        /**
         * 摘要生成端点
         */
        private String summarizeEndpoint = "/api/rag/summarize";

        private Yolo yolo = new Yolo();

        private Ai ai = new Ai();

        @Data
        public static class Yolo {
            private String detectEndpoint = "/api/yolo/detect";
            private String detectUrlEndpoint = "/api/yolo/detect-url";
        }

        @Data
        public static class Ai {
            private String removeBgEndpoint = "/api/ai/remove_bg";
            private String faceBlurEndpoint = "/api/ai/face_blur";
            private String changeBackgroundEndpoint = "/api/ai/change_background";
        }
    }
}
