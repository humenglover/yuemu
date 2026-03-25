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

@Document(indexName = "user")
@Data
public class EsUser implements Serializable {
    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 账号：支持英文和数字搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private String userAccount;

    /**
     * 用户昵称：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String userName;

    /**
     * 用户头像
     */
    @Field(type = FieldType.Keyword)
    private String userAvatar;

    /**
     * 用户简介：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    @Field(type = FieldType.Keyword)
    private String userRole;

    /**
     * 编辑时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date editTime;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    /**
     * 用户性别
     */
    @Field(type = FieldType.Keyword)
    private String gender;

    /**
     * 所在地区
     */
    @Field(type = FieldType.Keyword)
    private String region;

    /**
     * 生日
     */
    @Field(type = FieldType.Date, format = DateFormat.date)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date birthday;

    /**
     * 个人标签
     */
    @Field(type = FieldType.Keyword)
    private String userTags;

    /**
     * 主页背景图URL
     */
    @Field(type = FieldType.Keyword)
    private String homepageBg;

    /**
     * 个性签名
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String personalSign;

    /**
     * 主题偏好
     */
    @Field(type = FieldType.Keyword)
    private String themePreference;

    /**
     * 内容可见性设置
     */
    @Field(type = FieldType.Keyword)
    private String visibilitySetting;

    /**
     * 兴趣领域
     */
    @Field(type = FieldType.Keyword)
    private String interestField;

    /**
     * 最后活跃时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date lastActiveTime;

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

    private static final long serialVersionUID = 1L;
}
