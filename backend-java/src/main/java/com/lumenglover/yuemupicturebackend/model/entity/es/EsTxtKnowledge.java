package com.lumenglover.yuemupicturebackend.model.entity.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@Document(indexName = "txt_knowledge_base")
public class EsTxtKnowledge {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String textChunk;

    @Field(type = FieldType.Keyword)
    private String txtName;

    @Field(type = FieldType.Keyword)
    private String md5;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'||epoch_millis")
    private Date createTime;
}
