package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.AppVersion;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface AppVersionService extends IService<AppVersion> {
    /**
     * 获取最新版本信息
     */
    AppVersion getLatestVersion();

    /**
     * 上传新版本
     */
    void uploadNewVersion(MultipartFile file, AppVersion appVersion, User loginUser);

    /**
     * 分页获取版本历史
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 版本历史列表
     */
    Page<AppVersion> getVersionHistory(long current, long pageSize);
}
