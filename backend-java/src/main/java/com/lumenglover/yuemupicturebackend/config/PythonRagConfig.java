package com.lumenglover.yuemupicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python RAG服务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.python-service")
public class PythonRagConfig {
    /**
     * 是否启用Python RAG服务
     */
    private boolean enabled = true;

    /**
     * Python RAG服务基础URL
     */
    private String baseUrl = "http://127.0.0.1:8001";

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 300000;

    /**
     * 同步接口端点
     */
    private String syncEndpoint = "/api/rag/sync";

    /**
     * 流式接口端点
     */
    private String streamEndpoint = "/api/rag/stream";

    /**
     * 知识库管理接口配置
     */
    private KnowledgeEndpoints knowledge = new KnowledgeEndpoints();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getSyncEndpoint() {
        return syncEndpoint;
    }

    public void setSyncEndpoint(String syncEndpoint) {
        this.syncEndpoint = syncEndpoint;
    }

    public String getStreamEndpoint() {
        return streamEndpoint;
    }

    public void setStreamEndpoint(String streamEndpoint) {
        this.streamEndpoint = streamEndpoint;
    }

    public KnowledgeEndpoints getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(KnowledgeEndpoints knowledge) {
        this.knowledge = knowledge;
    }

    // 获取完整的API URL
    public String getFullSyncUrl() {
        return baseUrl + syncEndpoint;
    }

    public String getFullStreamUrl() {
        return baseUrl + streamEndpoint;
    }

    public String getFullKnowledgeUploadUrl() {
        return baseUrl + knowledge.getUploadEndpoint();
    }

    public String getFullKnowledgeListUrl() {
        return baseUrl + knowledge.getListEndpoint();
    }

    public String getFullKnowledgeDeleteUrl() {
        return baseUrl + knowledge.getDeleteEndpoint();
    }

    public String getFullKnowledgeClearAllUrl() {
        return baseUrl + knowledge.getClearAllEndpoint();
    }



    // 为了兼容性保留baseUrl的直接访问方法
    public String getUrl() {
        return baseUrl;
    }

    // 内部类定义知识库端点
    public static class KnowledgeEndpoints {
        private String uploadEndpoint = "/api/knowledge/upload";
        private String listEndpoint = "/api/knowledge/list";
        private String deleteEndpoint = "/api/knowledge/delete";
        private String clearAllEndpoint = "/api/knowledge/clear-all";
        private int maxFiles = 25;  // 知识库最大文件数量限制

        // Getters and Setters
        public String getUploadEndpoint() { return uploadEndpoint; }
        public void setUploadEndpoint(String uploadEndpoint) { this.uploadEndpoint = uploadEndpoint; }
        public String getListEndpoint() { return listEndpoint; }
        public void setListEndpoint(String listEndpoint) { this.listEndpoint = listEndpoint; }
        public String getDeleteEndpoint() { return deleteEndpoint; }
        public void setDeleteEndpoint(String deleteEndpoint) { this.deleteEndpoint = deleteEndpoint; }
        public String getClearAllEndpoint() { return clearAllEndpoint; }
        public void setClearAllEndpoint(String clearAllEndpoint) { this.clearAllEndpoint = clearAllEndpoint; }
        public int getMaxFiles() { return maxFiles; }
        public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    }
}
