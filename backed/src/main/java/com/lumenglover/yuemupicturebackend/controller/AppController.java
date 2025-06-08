package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.model.entity.AppVersion;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.AppVersionService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;

@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String DOWNLOAD_COUNT_KEY = "app:download:count:%s";
    private static final int DOWNLOAD_LIMIT = 5;
    private static final long DOWNLOAD_LIMIT_DURATION = 1; // 1 hour

    /**
     * 上传新版本
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> uploadNewVersion(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("appVersion") String appVersionJson,
                                                  HttpServletRequest request) {
        if (file == null || StrUtil.isBlank(appVersionJson)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 解析版本信息
        AppVersion appVersion;
        try {
            appVersion = JSONUtil.toBean(appVersionJson, AppVersion.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本信息格式错误");
        }

        User loginUser = userService.getLoginUser(request);
        appVersionService.uploadNewVersion(file, appVersion, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取最新版本信息
     */
    @GetMapping("/version")
    public BaseResponse<Map<String, Object>> getLatestVersion() {
        AppVersion latestVersion = appVersionService.getLatestVersion();
        if (latestVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "暂无版本信息");
        }

        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("version", latestVersion.getVersion());
        versionInfo.put("versionCode", latestVersion.getVersionCode());
        versionInfo.put("updateTime", latestVersion.getCreateTime());
        versionInfo.put("description", latestVersion.getDescription());
        versionInfo.put("forceUpdate", latestVersion.getIsForce() == 1);
        versionInfo.put("downloadUrl", "/api/app/download");
        versionInfo.put("apkSize", latestVersion.getApkSize());

        return ResultUtils.success(versionInfo);
    }

    /**
     * 下载APK
     */
    @GetMapping("/download")
    public void downloadApp(HttpServletResponse response, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 检查下载次数限制
        String downloadKey = String.format(DOWNLOAD_COUNT_KEY, userId);
        String downloadCountStr = stringRedisTemplate.opsForValue().get(downloadKey);
        int downloadCount = downloadCountStr != null ? Integer.parseInt(downloadCountStr) : 0;

        if (downloadCount >= DOWNLOAD_LIMIT) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载次数已达上限，请稍后再试");
        }

        AppVersion latestVersion = appVersionService.getLatestVersion();
        if (latestVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "暂无版本信息");
        }

        File file = new File(latestVersion.getApkPath());
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "APK文件不存在");
        }

        try {
            String fileName = "yuemu_" + latestVersion.getVersion() + ".apk";

            // 设置响应头
            response.setContentType("application/vnd.android.package-archive");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            // 从本地文件读取并写入响应
            try (FileInputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
            }

            // 增加下载计数
            stringRedisTemplate.opsForValue().increment(downloadKey);
            stringRedisTemplate.expire(downloadKey, DOWNLOAD_LIMIT_DURATION, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("APK下载失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载失败");
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/history")
    public BaseResponse<Page<AppVersion>> getVersionHistory(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 参数校验
        ThrowUtils.throwIf(current < 1, ErrorCode.PARAMS_ERROR, "页码不能小于1");
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 20, ErrorCode.PARAMS_ERROR, "每页大小必须在1-20之间");

        // 获取版本历史
        Page<AppVersion> versionHistory = appVersionService.getVersionHistory(current, pageSize);

        // 处理返回结果，脱敏处理
        Page<AppVersion> result = new Page<>(versionHistory.getCurrent(), versionHistory.getSize(), versionHistory.getTotal());
        List<AppVersion> records = versionHistory.getRecords().stream().map(version -> {
            // 不返回文件路径等敏感信息
            version.setApkPath(null);
            return version;
        }).collect(Collectors.toList());
        result.setRecords(records);

        return ResultUtils.success(result);
    }
}
