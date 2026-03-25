package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.KnowledgeFile;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class KnowledgeFileVO implements Serializable {

    /**
     * 知识库文件ID
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
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 文件大小格式化显示（如：1.2MB）
     */
    private String fileSizeDisplay;

    /**
     * 上传用户名
     */
    private String userName;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     */
    public static KnowledgeFileVO objToVo(KnowledgeFile knowledgeFile) {
        if (knowledgeFile == null) {
            return null;
        }
        KnowledgeFileVO knowledgeFileVO = new KnowledgeFileVO();
        BeanUtils.copyProperties(knowledgeFile, knowledgeFileVO);

        // 格式化文件大小显示
        if (knowledgeFile.getFileSize() != null) {
            knowledgeFileVO.setFileSizeDisplay(formatFileSize(knowledgeFile.getFileSize()));
        }

        return knowledgeFileVO;
    }

    /**
     * 格式化文件大小显示
     */
    private static String formatFileSize(Long size) {
        if (size == null) {
            return "0B";
        }

        double bytes = size.doubleValue();
        if (bytes < 1024) {
            return String.format("%.0fB", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024 * 1024));
        } else {
            return String.format("%.1fGB", bytes / (1024 * 1024 * 1024));
        }
    }
}
