package com.lumenglover.yuemupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 允许的图片格式（小写）
     */
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp", "gif");

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                getFileSuffixSafe(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 计算宽高
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            // 返回可访问的地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }

    }

    /**
     * 校验文件（增强版）
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");

        // 2. 获取安全的文件后缀（处理大小写、空值、多后缀）
        String fileSuffix = getFileSuffixSafe(multipartFile.getOriginalFilename());

        // 3. 校验文件后缀（容错处理）
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                String.format("文件类型错误，仅支持 %s 格式", String.join("、", ALLOW_FORMAT_LIST)));

        // 【可选】增强校验：通过文件内容判断真实类型（防止改后缀的恶意文件）
        validPictureByContent(multipartFile);
    }

    /**
     * 安全获取文件后缀（处理各种异常情况）
     * @param originalFilename 原始文件名
     * @return 小写的文件后缀（无点）
     */
    private String getFileSuffixSafe(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "jpg"; // 默认后缀
        }

        // 处理文件名含多个点的情况（取最后一个点后的内容）
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            // 无后缀或后缀为空（如 "image."）
            log.warn("文件无有效后缀，文件名：{}", originalFilename);
            return "jpg"; // 默认后缀
        }

        // 提取后缀并转小写，去除前后空格
        String suffix = originalFilename.substring(lastDotIndex + 1).trim().toLowerCase(Locale.ROOT);

        // 处理特殊情况（如后缀过长）
        if (suffix.length() > 10) {
            log.warn("文件后缀过长，文件名：{}", originalFilename);
            return "jpg";
        }

        return suffix;
    }

    /**
     * 【增强】通过文件内容校验真实图片类型（防止改后缀的恶意文件）
     * @param multipartFile
     */
    private void validPictureByContent(MultipartFile multipartFile) {
        try {
            // 获取文件前几个字节判断文件类型
            byte[] header = new byte[8];
            multipartFile.getInputStream().read(header);
            String headerHex = bytesToHex(header).toUpperCase(Locale.ROOT);

            // 常见图片文件的魔数（文件头）
            String jpegHex = "FFD8FF";
            String pngHex = "89504E47";
            String gifHex = "47494638";
            String webpHex = "52494646";

            // 校验文件头
            boolean isImage = headerHex.startsWith(jpegHex)
                    || headerHex.startsWith(pngHex)
                    || headerHex.startsWith(gifHex)
                    || headerHex.startsWith(webpHex);

            if (!isImage) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是有效的图片格式");
            }
        } catch (Exception e) {
            log.error("校验文件内容失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容校验失败，请上传有效的图片文件");
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

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
