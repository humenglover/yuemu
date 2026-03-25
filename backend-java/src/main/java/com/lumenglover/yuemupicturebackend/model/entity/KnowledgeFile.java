package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 知识库文件表
 * @TableName knowledge_file
 */
@TableName(value = "knowledge_file")
@Data
public class KnowledgeFile implements Serializable {
    /**
     * 知识库文件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storedName;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型(pdf,txt,docx,md等)
     */
    private String fileType;

    /**
     * 上传时间
     */
    @TableField(value = "uploadTime")
    private Date uploadTime;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 文件MD5哈希值
     */
    private String md5Hash;

    /**
     * 向量数量
     */
    private Integer vectorCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
