package com.lumenglover.yuemupicturebackend.model.entity.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;

import java.io.Serializable;
import java.util.Date;

@Document(indexName = "search_keyword")
@Data
public class EsSearchKeyword implements Serializable {

    @Id
    private String id;

    /**
     * 搜索关键词：支持中英文混合搜索
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text),
        otherFields = {
            @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String keyword;

    /**
     * 搜索类型
     */
    @Field(type = FieldType.Keyword)
    private String type;

    /**
     * 搜索次数
     */
    @Field(type = FieldType.Long)
    private Long count;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
