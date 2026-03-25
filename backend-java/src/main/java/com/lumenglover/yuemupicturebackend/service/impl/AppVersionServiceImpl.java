package com.lumenglover.yuemupicturebackend.service.impl;

import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.model.entity.AppVersion;
import com.lumenglover.yuemupicturebackend.mapper.AppVersionMapper;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.AppVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;

@Service
@Slf4j
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Resource
    private CosManager cosManager;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public AppVersion getLatestVersion() {
        return this.lambdaQuery()
                .eq(AppVersion::getStatus, 1)
                .orderByDesc(AppVersion::getVersionCode)
                .last("limit 1")
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadNewVersion(MultipartFile file, AppVersion appVersion, User loginUser) {
        // 校验权限
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()),
                ErrorCode.NO_AUTH_ERROR);

        // 创建目录
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 上传文件
        String fileName = "yuemu_" + appVersion.getVersion() + ".apk";
        String filePath = uploadPath + File.separator + fileName;
        File destFile = new File(filePath);
        try {
            file.transferTo(destFile);

            // 设置APK信息
            appVersion.setApkPath(filePath);
            appVersion.setApkSize(file.getSize());
            appVersion.setStatus(1);

            // 保存版本信息
            boolean success = this.save(appVersion);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        } catch (Exception e) {
            log.error("上传新版本失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败");
        }
    }

    @Override
    public Page<AppVersion> getVersionHistory(long current, long pageSize) {
        return this.lambdaQuery()
                .eq(AppVersion::getStatus, 1)
                .orderByDesc(AppVersion::getVersionCode)
                .page(new Page<>(current, pageSize));
    }
}
