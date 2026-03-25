package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 知识库文件上传实现类
 */
@Slf4j
public class FileKnowledgeUpload extends FileUploadTemplate {

    private MultipartFile multipartFile;

    public FileKnowledgeUpload(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }

    @Override
    protected void validFile(Object inputSource) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        // 检查文件扩展名
        String fileExtension = FileUtil.extName(originalFilename).toLowerCase();
        if (!isValidExtension(fileExtension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("不支持的文件类型: %s，仅支持PDF、TXT、DOCX、MD格式", fileExtension));
        }

        // 限制文件大小（例如100MB）
        final long MAX_FILE_SIZE = 100 * 1024 * 1024;
        if (multipartFile.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过100MB");
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        multipartFile.transferTo(file);
    }

    /**
     * 检查文件扩展名是否有效
     */
    private boolean isValidExtension(String extension) {
        return extension.equals("pdf") ||
               extension.equals("txt") ||
               extension.equals("docx") ||
               extension.equals("md");
    }
}
