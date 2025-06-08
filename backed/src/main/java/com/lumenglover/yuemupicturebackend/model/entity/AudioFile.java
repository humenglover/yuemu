package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 音频文件实体类
 */
@TableName(value = "audio_file")
@Data
public class AudioFile implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 上传用户id
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 音频时长(秒)
     */
    private Integer duration;

    /**
     * 文件MIME类型
     */
    private String mimeType;

    /**
     * 文件MD5值
     */
    private String md5;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 音频标题
     */
    private String title;

    /**
     * 音频描述
     */
    private String description;

    /**
     * 艺术家/作者
     */
    private String artist;

    /**
     * 专辑名称
     */
    private String album;

    /**
     * 音乐类型/风格
     */
    private String genre;

    /**
     * 所属空间ID
     */
    private Long spaceId;

    /**
     * 播放次数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
