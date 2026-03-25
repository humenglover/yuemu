package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.upload.FilePictureUpload;
import com.lumenglover.yuemupicturebackend.manager.upload.PictureUploadTemplate;
import com.lumenglover.yuemupicturebackend.manager.upload.UrlPictureUpload;
import com.lumenglover.yuemupicturebackend.mapper.TimeAlbumMapper;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import com.lumenglover.yuemupicturebackend.model.dto.picture.PictureUploadRequest;
import com.lumenglover.yuemupicturebackend.model.dto.timealbum.TimeAlbumHeartWallRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.TimeAlbum;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.PictureReviewStatusEnum;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.service.TimeAlbumService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.ColorTransformUtils;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.utils.TencentCloudImageAuditUtil;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 时光相册服务实现类
 */
@Service
@Slf4j
public class TimeAlbumServiceImpl extends ServiceImpl<TimeAlbumMapper, TimeAlbum>
        implements TimeAlbumService {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private TencentCloudImageAuditUtil tencentCloudImageAuditUtil;

    @Resource
    private SensitiveUtil sensitiveUtil;

    @Override
    public long createTimeAlbum(TimeAlbum timeAlbum, long loginUserId, long loveBoardId) {
        // 校验参数
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(timeAlbum.getAlbumName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "相册名称不能为空");
        }

        // 如果设置了密码，进行MD5加密
        String password = timeAlbum.getPassword();
        if (StringUtils.isNotBlank(password)) {
            // 使用MD5加密密码
            String encryptedPassword = DigestUtil.md5Hex(password);
            timeAlbum.setPassword(encryptedPassword);
        }

        // 设置用户ID和恋爱板ID
        timeAlbum.setUserId(loginUserId);
        timeAlbum.setLoveBoardId(loveBoardId);
        // 保存相册
        boolean result = this.save(timeAlbum);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建相册失败");
        }
        return timeAlbum.getId();
    }

    @Override
    public boolean deleteTimeAlbum(long id, long loginUserId, long loveBoardId) {
        // 校验相册是否存在且属于当前用户和恋爱板
        TimeAlbum timeAlbum = this.getById(id);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!timeAlbum.getUserId().equals(loginUserId) || !timeAlbum.getLoveBoardId().equals(loveBoardId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除相册
        return this.removeById(id);
    }

    @Override
    public boolean updateTimeAlbum(TimeAlbum timeAlbum, long loginUserId, long loveBoardId) {
        // 校验参数
        if (timeAlbum == null || timeAlbum.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验相册是否存在且属于当前用户和恋爱板
        TimeAlbum oldTimeAlbum = this.getById(timeAlbum.getId());
        if (oldTimeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!oldTimeAlbum.getUserId().equals(loginUserId) || !oldTimeAlbum.getLoveBoardId().equals(loveBoardId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 更新相册
        return this.updateById(timeAlbum);
    }

    @Override
    public TimeAlbum getTimeAlbumById(long id, Long userId, String password) {
        // 查询相册
        TimeAlbum timeAlbum = this.getById(id);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 如果是公开相册，直接返回
        if (timeAlbum.getIsPublic() == 1) {
            // 替换URL为自定义域名
            timeAlbum.replaceUrlWithCustomDomain();
            return timeAlbum;
        }

        // 如果是私密相册
        // 1. 如果是所有者访问
        if (userId != null && userId.equals(timeAlbum.getUserId())) {
            // 替换URL为自定义域名
            timeAlbum.replaceUrlWithCustomDomain();
            return timeAlbum;
        }

        // 2. 如果是其他人访问，需要验证密码
        if (StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请输入相册密码");
        }

        // 对输入的密码进行MD5加密后再比较
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(timeAlbum.getPassword())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "相册密码错误");
        }

        // 替换URL为自定义域名
        timeAlbum.replaceUrlWithCustomDomain();
        return timeAlbum;
    }

    /**
     * 上传相册图片
     */
    private PictureVO uploadTimeAlbumPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 校验相册是否存在
        Long albumId = pictureUploadRequest.getSpaceId();
        if (albumId != null) {
            TimeAlbum timeAlbum = this.getById(albumId);
            if (timeAlbum == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
            }
            if (!timeAlbum.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
            }
        }

        // 上传图片，得到图片信息
        String uploadPathPrefix = String.format("album/%s", albumId);

        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(albumId); // 使用相册id作为空间id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        // 设置图片简介
        String introduction = pictureUploadRequest.getIntroduction();
        if (StrUtil.isNotBlank(introduction)) {
            picture.setIntroduction(introduction);
        }
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setUserId(loginUser.getId());

        // 设置为审核中状态，用于机器审核
        picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        picture.setReviewMessage("时光相册图片机器审核中");
        picture.setIsDraft(0); // 相册图片不需要草稿状态

        // 保存到数据库
        boolean result = pictureService.save(picture);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败");
        }

        // 进行机器审核
        try {
            // 获取完整的图片信息用于审核
            Picture pictureForAudit = pictureService.getById(picture.getId());

            // 执行腾讯云图片审核
            performTencentCloudImageAudit(pictureForAudit, loginUser);

            // 审核完成后更新数据库
            boolean auditResult = pictureService.updateById(pictureForAudit);
            if (!auditResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片审核结果更新失败");
            }

            // 如果审核不通过，删除数据库记录
            if (pictureForAudit.getReviewStatus() != null &&
                    pictureForAudit.getReviewStatus().equals(PictureReviewStatusEnum.REJECT.getValue())) {
                log.warn("时光相册图片审核未通过，删除图片记录，图片ID: {}", pictureForAudit.getId());
                pictureService.removeById(pictureForAudit.getId());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片审核未通过: " + pictureForAudit.getReviewMessage());
            }

            // 返回审核通过的图片信息
            return PictureVO.objToVo(pictureForAudit);
        } catch (Exception e) {
            log.error("时光相册图片审核失败，删除图片记录，图片ID: {}", picture.getId(), e);
            // 审核过程中出现异常，删除已保存的图片记录
            pictureService.removeById(picture.getId());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片审核未通过: " + e.getMessage());
        }
    }

    /**
     * 执行腾讯云图片审核
     * @param picture 图片实体
     * @param loginUser 当前登录用户
     */
    private void performTencentCloudImageAudit(Picture picture, User loginUser) {
        try {
            // 对图片的名称和简介进行敏感词过滤
            String filteredName = SensitiveUtil.filter(picture.getName());
            if (filteredName != null && !filteredName.equals(picture.getName())) {
                picture.setName(filteredName);
                log.info("时光相册图片名称已过滤，ID: {}", picture.getId());
            }

            String filteredIntroduction = SensitiveUtil.filter(picture.getIntroduction());
            if (filteredIntroduction != null && !filteredIntroduction.equals(picture.getIntroduction())) {
                picture.setIntroduction(filteredIntroduction);
                log.info("时光相册图片简介已过滤，ID: {}", picture.getId());
            }

            String filteredTags = SensitiveUtil.filter(picture.getTags());
            if (filteredTags != null && !filteredTags.equals(picture.getTags())) {
                picture.setTags(filteredTags);
                log.info("时光相册图片标签已过滤，ID: {}", picture.getId());
            }

            // 从配置中获取审核策略类型
            String bizType = cosClientConfig.getAuditBizType();
            if (bizType == null) {
                bizType = ""; // 默认为空，使用系统默认审核策略
            }

            // 对图片进行审核
            ImageAuditingResponse auditResponse =
                    tencentCloudImageAuditUtil.auditImageByUrl(picture.getUrl(), bizType);

            boolean isCompliant = tencentCloudImageAuditUtil.isImageCompliant(auditResponse);
            String auditLabel = tencentCloudImageAuditUtil.getAuditLabel(auditResponse);
            Integer auditScore = tencentCloudImageAuditUtil.getAuditScore(auditResponse);

            if (isCompliant) {
                // 图片合规，自动通过审核
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewMessage("时光相册图片审核: 自动通过 (标签: " + auditLabel + ", 分数: " + auditScore + ")");
                picture.setReviewerId(0L); // 系统审核
                picture.setReviewTime(new Date());
                log.info("时光相册图片审核通过，ID: {}, URL: {}, 标签: {}, 分数: {}",
                        picture.getId(), picture.getUrl(), auditLabel, auditScore);
            } else {
                // 图片不合规，审核拒绝
                picture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());
                picture.setReviewMessage("时光相册图片审核: 拒绝 (标签: " + auditLabel + ", 分数: " + auditScore + ")");
                picture.setReviewerId(0L); // 系统审核
                picture.setReviewTime(new Date());
                log.warn("时光相册图片审核未通过，ID: {}, URL: {}, 标签: {}, 分数: {}",
                        picture.getId(), picture.getUrl(), auditLabel, auditScore);
            }
        } catch (Exception e) {
            log.error("腾讯云图片审核服务调用失败，图片ID: {}, URL: {}, 错误: {}",
                    picture.getId(), picture.getUrl(), e.getMessage());

            // 如果审核服务调用失败，设置为审核拒绝状态
            picture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());
            picture.setReviewMessage("图片审核服务异常，审核拒绝: " + e.getMessage());
            picture.setReviewerId(0L); // 系统审核
            picture.setReviewTime(new Date());
        }
    }

    @Override
    public boolean deleteHeartWallPicture(long pictureId, long albumId, User loginUser) {
        // 校验相册是否存在且属于当前用户
        TimeAlbum timeAlbum = this.getById(albumId);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        }
        if (!timeAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
        }

        // 校验照片是否存在且属于该相册
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "照片不存在");
        }
        if (!picture.getSpaceId().equals(albumId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "照片不属于该相册");
        }

        // 直接使用 MyBatis-Plus 的删除方法
        return pictureService.removeById(pictureId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PictureVO> uploadHeartWallPictures(TimeAlbumHeartWallRequest request, long loginUserId) {
        if (request == null || request.getAlbumId() == null || request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取登录用户信息
        User loginUser = userService.getById(loginUserId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 校验相册是否存在且属于当前用户
        TimeAlbum timeAlbum = this.getById(request.getAlbumId());
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        }
        if (!timeAlbum.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
        }

        // 检查照片数量限制
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", request.getAlbumId());
        long currentCount = pictureService.count(queryWrapper);
        int newCount = request.getFiles().size();
        if (currentCount + newCount > 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "相册照片数量已达上限（100张）");
        }

        // 如果是覆盖模式，先删除原有的爱心墙图片
        if (Boolean.TRUE.equals(request.getOverride())) {
            List<Picture> oldPictures = pictureService.list(queryWrapper);
            if (!oldPictures.isEmpty()) {
                oldPictures.forEach(picture -> pictureService.deletePicture(picture.getId(), loginUser));
            }
        }

        // 上传新的图片
        List<PictureVO> uploadedPictures = new ArrayList<>();
        for (MultipartFile file : request.getFiles()) {
            PictureUploadRequest uploadRequest = new PictureUploadRequest();
            uploadRequest.setSpaceId(request.getAlbumId());
            uploadRequest.setPicName("爱心墙_" + System.currentTimeMillis());

            try {
                // 使用新的上传方法
                PictureVO pictureVO = uploadTimeAlbumPicture(file, uploadRequest, loginUser);
                uploadedPictures.add(pictureVO);
            } catch (BusinessException e) {
                log.warn("时光相册图片上传失败，跳过该图片，继续处理其他图片，错误信息: {}", e.getMessage());
                // 继续处理下一张图片，不中断整个上传过程
                continue;
            }
        }

        return uploadedPictures;
    }

    @Override
    public List<Picture> getHeartWallPictures(long albumId, Long userId, String password) {
        // 校验相册是否存在并检查访问权限
        TimeAlbum timeAlbum = this.getTimeAlbumById(albumId, userId, password);

        // 查询爱心墙图片
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", albumId)
                .orderByAsc("id");

        List<Picture> pictures = pictureService.list(queryWrapper);
        // 替换URL为自定义域名
        pictures.forEach(Picture::replaceUrlWithCustomDomain);
        return pictures;
    }

    @Override
    public boolean setAlbumPassword(Long albumId, String password, Long loginUserId) {
        // 校验参数
        if (albumId == null || StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取相册信息
        TimeAlbum timeAlbum = this.getById(albumId);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        }

        // 验证权限
        if (!timeAlbum.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
        }

        // 只有当相册是公开的（isPublic = 1）时才允许设置密码
        if (timeAlbum.getIsPublic() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "相册已设置密码，请使用修改密码功能");
        }

        // 加密密码
        String encryptedPassword = DigestUtil.md5Hex(password);
        timeAlbum.setPassword(encryptedPassword);
        // 设置为私密相册
        timeAlbum.setIsPublic(0);

        return this.updateById(timeAlbum);
    }

    @Override
    public boolean updateAlbumPassword(Long albumId, String oldPassword, String newPassword, Long loginUserId) {
        // 校验参数
        if (albumId == null || StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取相册信息
        TimeAlbum timeAlbum = this.getById(albumId);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        }

        // 验证权限
        if (!timeAlbum.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
        }

        // 验证原密码
        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword);
        if (!encryptedOldPassword.equals(timeAlbum.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        // 加密新密码并更新
        String encryptedNewPassword = DigestUtil.md5Hex(newPassword);
        timeAlbum.setPassword(encryptedNewPassword);

        return this.updateById(timeAlbum);
    }

    @Override
    public boolean removeAlbumPassword(Long albumId, String password, Long loginUserId) {
        // 校验参数
        if (albumId == null || StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取相册信息
        TimeAlbum timeAlbum = this.getById(albumId);
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        }

        // 验证权限
        if (!timeAlbum.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该相册");
        }

        // 验证密码
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(timeAlbum.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 清除密码并设置为公开
        timeAlbum.setPassword(null);
        timeAlbum.setIsPublic(1);

        return this.updateById(timeAlbum);
    }
}
