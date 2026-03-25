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

@Document(indexName = "picture")
@Data
public class EsPicture implements Serializable {
    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 图片 url
     */
    @Field(type = FieldType.Keyword)
    private String url;

    /**
     * 缩略图 url
     */
    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    /**
     * 图片名称：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String name;

    /**
     * 简介：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String introduction;

    /**
     * 分类
     */
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

    /**
     * 图片体积
     */
    @Field(type = FieldType.Long)
    private Long picSize;

    /**
     * 图片宽度
     */
    @Field(type = FieldType.Integer)
    private Integer picWidth;

    /**
     * 图片高度
     */
    @Field(type = FieldType.Integer)
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @Field(type = FieldType.Double)
    private Double picScale;

    /**
     * 图片格式
     */
    @Field(type = FieldType.Keyword)
    private String picFormat;

    /**
     * 图片主色调
     */
    @Field(type = FieldType.Keyword)
    private String picColor;

    /**
     * 创建用户 id
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 评论数
     */
    @Field(type = FieldType.Long)
    private Long commentCount;

    /**
     * 点赞数
     */
    @Field(type = FieldType.Long)
    private Long likeCount;

    /**
     * 分享数
     */
    @Field(type = FieldType.Long)
    private Long shareCount;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    @Field(type = FieldType.Integer)
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Field(type = FieldType.Text)
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    @Field(type = FieldType.Long)
    private Long reviewerId;

    /**
     * 空间 id
     */
    @Field(type = FieldType.Long)
    private Long spaceId;

    /**
     * 审核时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date reviewTime;

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
     * 是否为草稿：0-非草稿 1-草稿
     */
    @Field(type = FieldType.Integer)
    private Integer isDraft;

    /**
     * 浏览量
     */
    @Field(type = FieldType.Long)
    private Long viewCount;

    private static final long serialVersionUID = 1L;
}
