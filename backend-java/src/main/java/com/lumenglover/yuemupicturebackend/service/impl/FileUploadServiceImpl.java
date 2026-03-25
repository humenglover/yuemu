package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.upload.AudioUploadTemplate;
import com.lumenglover.yuemupicturebackend.manager.upload.FileAudioUpload;
import com.lumenglover.yuemupicturebackend.manager.upload.FilePictureUpload;
import com.lumenglover.yuemupicturebackend.manager.upload.PictureUploadTemplate;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.PictureReviewStatusEnum;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.AudioFileService;
import com.lumenglover.yuemupicturebackend.service.FileUploadService;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 通用文件上传服务实现类
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private AudioFileService audioFileService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private FileAudioUpload fileAudioUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PictureVO uploadPicture(Object inputSource, Long userId, String name, String description, String tags) {
        // 校验参数
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传内容不能为空");

        // 获取用户信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 上传图片
        String uploadPathPrefix = String.format("public/%s", userId);
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = filePictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造图片信息
        Picture picture = new Picture();
        picture.setSpaceId(-2L); // 通用上传接口使用-2作为特殊标识
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setName(StrUtil.isNotBlank(name) ? name : uploadPictureResult.getPicName());
        picture.setIntroduction(description);
        picture.setTags(tags);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setUserId(userId);
        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        picture.setReviewMessage("通用上传接口自动审核通过");
        picture.setReviewTime(new Date());

        // 保存到数据库
        return transactionTemplate.execute(status -> {
            boolean result = pictureService.save(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            return PictureVO.objToVo(picture);
        });
    }

    @Override
    public AudioFileVO uploadAudio(Object inputSource, Long userId, String title, String description, String artist, String tags) {
        // 校验参数
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传内容不能为空");

        // 获取用户信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 上传音频
        String uploadPathPrefix = String.format("audio/%s", userId);
        AudioFile audioFile = fileAudioUpload.uploadAudio(inputSource, uploadPathPrefix, userId);

        // 补充音频信息
        audioFile.setSpaceId(-2L); // 通用上传接口使用-2作为特殊标识
        audioFile.setTitle(title);
        audioFile.setDescription(description);
        audioFile.setArtist(artist);

        // 保存到数据库
        return transactionTemplate.execute(status -> {
            boolean result = audioFileService.save(audioFile);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "音频上传失败");
            return AudioFileVO.objToVo(audioFile);
        });
    }
}
