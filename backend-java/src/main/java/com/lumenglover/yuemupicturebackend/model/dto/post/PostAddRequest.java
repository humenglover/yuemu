package com.lumenglover.yuemupicturebackend.model.dto.post;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.util.List;

@Data
public class PostAddRequest {
    private Long id;

    private String title;

    // 使用 Markdown 格式的内容，图片使用 ![alt](url) 格式
    private String content;

    private String category;

    @JsonDeserialize(using = TagsDeserializer.class)
    private List<String> tags;



    // 封面图URL
    private String coverUrl;

    public static class TagsDeserializer extends JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonToken currToken = p.getCurrentToken();
            if (currToken == JsonToken.START_ARRAY) {
                // 如果是数组，直接将其转换为List
                return p.readValueAs(new TypeReference<List<String>>() {});
            } else if (currToken == JsonToken.VALUE_STRING) {
                // 如果是字符串，尝试解析为JSON数组
                String value = p.getValueAsString();
                if (value != null && value.trim().startsWith("[")) {
                    try {
                        return JSONUtil.toList(value, String.class);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }
}
