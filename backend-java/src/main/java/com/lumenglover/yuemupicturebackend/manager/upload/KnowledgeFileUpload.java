package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

/**
 * 专门用于知识库文件上传的实现类
 */
@Slf4j
@Component
public class KnowledgeFileUpload {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传知识库文件到指定路径
     *
     * @param file 要上传的MultipartFile
     * @param uploadPath 上传路径
     * @return 完整的文件URL
     */
    public String uploadKnowledgeFile(MultipartFile file, String uploadPath) {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("knowledge_temp_", ".tmp");
            // 将MultipartFile的内容写入临时文件
            file.transferTo(tempFile);
            // 上传临时文件到对象存储
            cosManager.putObject(uploadPath, tempFile);
            // 返回完整URL
            return cosClientConfig.getHost() + uploadPath;
        } catch (Exception e) {
            log.error("上传知识库文件到对象存储失败, path={}, error={}", uploadPath, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "知识库文件上传失败");
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("临时知识库文件删除失败: " + tempFile.getAbsolutePath());
                }
            }
        }
    }
}
