package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * 音频上传模板类
 */
@Slf4j
public abstract class AudioUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传音频
     *
     * @param inputSource      文件
     * @param uploadPathPrefix 上传路径前缀
     * @param userId          用户ID
     * @return 音频文件信息
     */
    public AudioFile uploadAudio(Object inputSource, String uploadPathPrefix, Long userId) {
        // 1. 校验音频
        validAudio(inputSource);

        // 2. 音频上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix.replaceFirst("^/", ""), uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);

            // 4. 上传音频到对象存储
            cosManager.putAudioObject(uploadPath, file);
            String fileUrl = cosClientConfig.getHost() + "/" + uploadPath;

            // 5. 创建音频文件记录
            AudioFile audioFile = new AudioFile();
            audioFile.setUserId(userId);
            audioFile.setFileName(originalFilename);
            audioFile.setFileUrl(fileUrl);
            audioFile.setFilePath(uploadPath);
            audioFile.setFileSize(FileUtil.size(file));
            audioFile.setDuration(getAudioDuration(file));
            audioFile.setMimeType(getMimeType(inputSource));
            audioFile.setMd5(calculateMd5(file));
            audioFile.setViewCount(0L);
            audioFile.setLikeCount(0L);

            return audioFile;

        } catch (Exception e) {
            log.error("音频上传到对象存储失败, path={}, error={}", uploadPath, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频上传失败");
        } finally {
            // 6. 临时文件清理
            deleteTempFile(file);
        }
    }

    /**
     * 校验音频文件
     */
    protected abstract void validAudio(Object inputSource);

    /**
     * 获取原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理文件并生成临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取音频时长（秒）
     */
    protected abstract Integer getAudioDuration(File file);

    /**
     * 获取MIME类型
     */
    protected abstract String getMimeType(Object inputSource);

    /**
     * 计算MD5值
     */
    protected abstract String calculateMd5(File file);

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
