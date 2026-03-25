package com.lumenglover.yuemupicturebackend.model.entity.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;

import java.io.Serializable;
import java.util.Date;

@Document(indexName = "post")
@Data
public class EsPost implements Serializable {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 标题：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String title;

    /**
     * 内容：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 标签：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String tags;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Long)
    private Long likeCount;

    @Field(type = FieldType.Long)
    private Long commentCount;

    /**
     * 分享数
     */
    @Field(type = FieldType.Long)
    private Long shareCount;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Text)
    private String reviewMessage;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date updateTime;

    @Field(type = FieldType.Integer)
    private Integer isDelete;



    private static final long serialVersionUID = 1L;
}
