package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@TableName(value = "search_keyword")
@Document(indexName = "search_keyword")
public class SearchKeyword {

    @Id
    private String id;

    /**
     * 搜索关键词
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
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
    @TableField(fill = FieldFill.INSERT)
    @Field(type = FieldType.Date)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Field(type = FieldType.Date)
    private Date updateTime;
}
