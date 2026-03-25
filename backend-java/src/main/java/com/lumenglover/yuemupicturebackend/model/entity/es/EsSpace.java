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

@Document(indexName = "space")
@Data
public class EsSpace implements Serializable {
    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 空间名称：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String spaceName;

    /**
     * 空间类型：0-私有 1-团队
     */
    @Field(type = FieldType.Integer)
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @Field(type = FieldType.Integer)
    private Integer spaceLevel;

    /**
     * 创建用户 id
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    /**
     * 编辑时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date editTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date updateTime;

    /**
     * 是否删除
     */
    @Field(type = FieldType.Integer)
    private Integer isDelete;

    /**
     * 是否推荐 0-否 1-是
     */
    @Field(type = FieldType.Integer)
    private Integer isRecommended;

    /**
     * 空间封面图
     */
    @Field(type = FieldType.Text)
    private String spaceCover;

    /**
     * 空间简介
     */
    @Field(type = FieldType.Text)
    private String spaceDesc;

    private static final long serialVersionUID = 1L;
}
