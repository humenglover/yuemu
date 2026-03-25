package com.lumenglover.yuemupicturebackend.model.entity.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;

/**
 * ES 存储的超长记忆索引模型
 */
@Data
@Document(indexName = "rag_memory")
public class EsRagMemory implements Serializable {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Id
    private String id;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 会话ID
     */
    @Field(type = FieldType.Long)
    private Long sessionId;

    /**
     * 记忆内容（摘要或关键对话片段）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 消息类型 0-摘要 1-对话片段
     */
    @Field(type = FieldType.Integer)
    private Integer memoryType;

    /**
     * 摘要层级：0-基础摘要，1-超级摘要
     */
    @Field(type = FieldType.Integer)
    private Integer summaryLevel;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
