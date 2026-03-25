package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * 通用文件上传模板类
 */
@Slf4j
public abstract class FileUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件
     *
     * @param inputSource      文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传后的文件路径信息
     */
    public String uploadFile(Object inputSource, String uploadPathPrefix) {
        // 1. 校验文件
        validFile(inputSource);

        // 2. 文件上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);

            // 4. 上传文件到对象存储
            cosManager.putObject(uploadPath, file);
            String fileUrl = cosClientConfig.getHost() + uploadPath;

            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传到对象存储失败, path={}, error={}", uploadPath, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            // 5. 临时文件清理
            deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     */
    protected abstract void validFile(Object inputSource);

    /**
     * 获取原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理文件并生成临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 清理临时文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
