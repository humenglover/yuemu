package com.lumenglover.yuemupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 文件图片上传
 */
@Slf4j
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    /**
     * 允许的图片格式（小写）
     */
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");

        // 2. 获取安全的文件后缀（处理大小写、空值、多后缀等异常）
        String originalFilename = multipartFile.getOriginalFilename();
        String fileSuffix = getFileSuffixSafe(originalFilename);

        // 调试日志（可根据需要保留）
        log.info("图片校验 - 文件名：{}，解析后缀：{}", originalFilename, fileSuffix);

        // 3. 校验文件后缀
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                String.format("文件类型错误，仅支持 %s 格式", String.join("、", ALLOW_FORMAT_LIST)));

        // 【可选增强】通过文件内容校验真实图片类型（防止改后缀的恶意文件）
        validPictureByContent(multipartFile);
    }

    /**
     * 安全获取文件后缀（处理各种异常情况）
     * @param originalFilename 原始文件名
     * @return 小写的文件后缀（无点）
     */
    private String getFileSuffixSafe(String originalFilename) {
        // 处理文件名null/空值
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("文件名为空，使用默认后缀 jpg");
            return "jpg";
        }

        // 处理文件名含多个点的情况（取最后一个点后的内容）
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            // 无后缀或后缀为空（如 "image."）
            log.warn("文件无有效后缀，文件名：{}，使用默认后缀 jpg", originalFilename);
            return "jpg";
        }

        // 提取后缀并转小写，去除前后空格
        String suffix = originalFilename.substring(lastDotIndex + 1).trim().toLowerCase(Locale.ROOT);

        // 处理后缀过长的异常情况
        if (suffix.length() > 10) {
            log.warn("文件后缀过长，文件名：{}，使用默认后缀 jpg", originalFilename);
            return "jpg";
        }

        return suffix;
    }

    /**
     * 【增强】通过文件内容校验真实图片类型（防止改后缀的恶意文件）
     * @param multipartFile 上传文件
     */
    private void validPictureByContent(MultipartFile multipartFile) {
        try {
            // 读取文件前8个字节（魔数）
            byte[] header = new byte[8];
            int readBytes = multipartFile.getInputStream().read(header);
            // 校验读取结果
            if (readBytes < 3) {
                throw new IOException("文件内容过短，无法识别图片类型");
            }

            // 转换为16进制字符串（大写）
            String headerHex = bytesToHex(header).toUpperCase(Locale.ROOT);

            // 常见图片文件魔数（文件头）
            boolean isJpeg = headerHex.startsWith("FFD8FF"); // JPEG/JPG
            boolean isPng = headerHex.startsWith("89504E47"); // PNG
            boolean isWebp = headerHex.startsWith("52494646"); // WEBP

            // 校验是否为合法图片
            if (!(isJpeg || isPng || isWebp)) {
                log.warn("文件后缀合法但内容不是图片，文件头：{}，文件名：{}",
                        headerHex.substring(0, 8), multipartFile.getOriginalFilename());
                throw new RuntimeException("文件内容不是有效的图片格式");
            }

        } catch (Exception e) {
            log.error("校验图片内容失败", e);
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR,
                    "文件内容校验失败，请上传有效的图片文件（jpeg/png/jpg/webp）");
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 使用流复制而不是 transferTo，因为 transferTo 可能导致 Tomcat 临时文件被删除，
        // 从而影响后面 YOLO 检测读取字节流的过程。
        FileUtil.writeFromStream(multipartFile.getInputStream(), file);
    }
}
