package com.lumenglover.yuemupicturebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.CredentialsProvider;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableScheduling
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final String[] DATE_FORMATS = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    private RestHighLevelClient client;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient elasticsearchClient() {
        // 解析URI并创建HttpHost
        String cleanUri = uris.replace("http://", "").replace("https://", "");
        String[] hostParts = cleanUri.split(":");
        String hostname = hostParts[0];
        int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 9200;

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, port, "http")
        );

        // 配置请求头
        builder.setDefaultHeaders(new org.apache.http.Header[]{
                new org.apache.http.message.BasicHeader("Content-Type", "application/json"),
                new org.apache.http.message.BasicHeader("Accept", "application/json")
        });

        // 配置认证信息
        if ("prod".equals(activeProfile) && username != null && !username.isEmpty()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(100)
                    .setKeepAliveStrategy((response, context) -> 300000)
                    .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                        if (response.getStatusLine().getStatusCode() >= 500) {
                            log.warn("Received server error from Elasticsearch");
                        }
                    }));
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(100)
                    .setKeepAliveStrategy((response, context) -> 300000)
                    .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                        if (response.getStatusLine().getStatusCode() >= 500) {
                            log.warn("Received server error from Elasticsearch");
                        }
                    }));
        }

        // 配置连接超时和Socket超时
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(5000)
                        .setSocketTimeout(300000)
                        .setConnectionRequestTimeout(0)
        );

        // 添加失败监听器
        builder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(Node node) {
                log.error("Node {} failed, attempting to reconnect", node.getName());
            }
        });

        client = new RestHighLevelClient(builder);
        return client;
    }

    /**
     * 定时检查ES连接状态
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void checkConnection() {
        if (client != null) {
            try {
                boolean isConnected = client.ping(RequestOptions.DEFAULT);
                if (isConnected) {
                    log.debug("Elasticsearch connection is healthy");
                } else {
                    log.warn("Lost connection to Elasticsearch, attempting to reconnect...");
                    reconnect();
                }
            } catch (IOException e) {
                log.error("Error checking Elasticsearch connection: {}", e.getMessage());
                reconnect();
            }
        } else {
            log.warn("Elasticsearch client is null, attempting to initialize...");
            client = elasticsearchClient();
        }
    }

    /**
     * 重连机制
     */
    private synchronized void reconnect() {
        int maxRetries = 3;
        int retryCount = 0;
        boolean connected = false;

        while (!connected && retryCount < maxRetries) {
            try {
                log.info("Attempting reconnection, try {}/{}", retryCount + 1, maxRetries);
                TimeUnit.SECONDS.sleep(5); // 等待5秒后重试

                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        log.warn("Error closing old client", e);
                    }
                }

                client = elasticsearchClient();
                if (client.ping(RequestOptions.DEFAULT)) {
                    connected = true;
                    log.info("Successfully reconnected to Elasticsearch");
                }
            } catch (Exception e) {
                retryCount++;
                log.error("Retry {} failed: {}", retryCount, e.getMessage());
            }
        }

        if (!connected) {
            log.error("Failed to reconnect to Elasticsearch after {} attempts", maxRetries);
        }
    }

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
                Arrays.asList(new DateToStringConverter(), new StringToDateConverter())
        );
    }

    @WritingConverter
    static class DateToStringConverter implements Converter<Date, String> {
        @Override
        public String convert(Date source) {
            if (source == null) {
                return null;
            }
            return String.format("%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tLZ", source);
        }
    }

    @ReadingConverter
    static class StringToDateConverter implements Converter<String, Date> {
        @Override
        public Date convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            try {
                return DateUtils.parseDate(source, DATE_FORMATS);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Failed to parse date: " + source, e);
            }
        }
    }
}
