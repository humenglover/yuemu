package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import lombok.Data;
import cn.hutool.json.JSONUtil;

import java.io.IOException;
import java.util.List;
/**
 * 论坛帖子表
 * @TableName post
 */
@Data
@TableName("post")
public class Post implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String category;

    @JsonDeserialize(using = TagsDeserializer.class)
    private String tags;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;

    private Integer status;

    private String reviewMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;



    @TableField(exist = false)
    private UserVO user;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 收藏数
     */
    private Long favoriteCount;

    /**
     * 热榜分数
     */
    @TableField("hotScore")
    private Double hotScore;

    /**
     * 封面图URL
     */
    @TableField(insertStrategy = FieldStrategy.DEFAULT)
    private String coverUrl;

    /**
     * 是否为草稿：0-非草稿 1-草稿
     */
    private Integer isDraft;

    /**
     * 是否允许收藏：1-允许、0-禁止
     */
    @TableField("allowCollect")
    private Integer allowCollect;

    /**
     * 是否允许点赞：1-允许、0-禁止
     */
    @TableField("allowLike")
    private Integer allowLike;

    /**
     * 是否允许评论：1-允许、0-禁止
     */
    @TableField("allowComment")
    private Integer allowComment;

    /**
     * 是否允许分享：1-允许、0-禁止
     */
    @TableField("allowShare")
    private Integer allowShare;

    public static class TagsDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonToken currToken = p.getCurrentToken();
            if (currToken == JsonToken.START_ARRAY) {
                // 如果是数组，将其转换为JSON字符串
                List<String> tagsList = p.readValueAs(new TypeReference<List<String>>() {});
                return JSONUtil.toJsonStr(tagsList);
            } else if (currToken == JsonToken.VALUE_STRING) {
                // 如果是字符串，直接返回
                return p.getValueAsString();
            } else {
                return null;
            }
        }
    }
}
