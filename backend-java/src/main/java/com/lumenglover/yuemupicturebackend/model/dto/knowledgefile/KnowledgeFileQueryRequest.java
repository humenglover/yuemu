package com.lumenglover.yuemupicturebackend.model.dto.knowledgefile;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识库文件查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeFileQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
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
     * 文件类型(pdf,txt,docx,md等)
     */
    private String fileType;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 文件MD5哈希值
     */
    private String md5Hash;

    /**
     * 向量数量最小值
     */
    private Integer minVectorCount;

    /**
     * 向量数量最大值
     */
    private Integer maxVectorCount;

    /**
     * 开始上传时间
     */
    private Date startUploadTime;

    /**
     * 结束上传时间
     */
    private Date endUploadTime;

    /**
     * 搜索词（同时搜文件名等）
     */
    private String searchText;

    private static final long serialVersionUID = 1L;
}