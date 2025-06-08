package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件方式上传音频实现类
 */
@Component
@Slf4j
public class FileAudioUpload extends AudioUploadTemplate {

    // 音频相关常量
    private static final long MAX_AUDIO_SIZE = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOW_AUDIO_FORMAT_SET = Arrays.asList("mp3", "wav", "ogg", "m4a")
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    @Override
    protected void validAudio(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频文件不能为空");
        }

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        if (fileSize > MAX_AUDIO_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频大小不能超过 20MB");
        }

        // 2. 校验文件后缀（不区分大小写）
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (fileSuffix == null || !ALLOW_AUDIO_FORMAT_SET.contains(fileSuffix.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的音频格式，仅支持：" + String.join(", ", ALLOW_AUDIO_FORMAT_SET));
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败");
        }
    }

    @Override
    protected Integer getAudioDuration(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFileFormat format = AudioSystem.getAudioFileFormat(file);

            if (format instanceof AudioFileFormat) {
                long microseconds = (long) (format.getFrameLength() * 1_000_000.0 / format.getFormat().getFrameRate());
                return (int) (microseconds / 1_000_000L);
            }

            // 如果无法获取准确时长，尝试估算
            long frames = audioInputStream.getFrameLength();
            if (frames != -1) {
                float frameRate = audioInputStream.getFormat().getFrameRate();
                return (int) (frames / frameRate);
            }

            return 0;
        } catch (Exception e) {
            log.error("获取音频时长失败, error={}", e.getMessage());
            return 0;
        }
    }

    @Override
    protected String getMimeType(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        String contentType = multipartFile.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            // 如果无法获取Content-Type，根据文件后缀推断
            String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
            return "audio/" + suffix.toLowerCase();
        }
        return contentType;
    }

    @Override
    protected String calculateMd5(File file) {
        return DigestUtil.md5Hex(file);
    }
}
