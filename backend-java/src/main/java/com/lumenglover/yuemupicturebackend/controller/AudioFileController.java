package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.audio.AudioUploadRequest;
import com.lumenglover.yuemupicturebackend.model.dto.audio.AudioQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.service.AudioFileService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 音频文件接口
 */
@RestController
@RequestMapping("/audio")
@Slf4j
public class AudioFileController {

    @Resource
    private AudioFileService audioFileService;

    @Resource
    private UserService userService;

    /**
     * 上传音频文件
     */
    @PostMapping("/upload")
    @RateLimiter(key = "audio_upload", time = 3600, count = 60, message = "音频上传过于频繁，请稍后再试")
    public BaseResponse<AudioFileVO> uploadAudio(
            @ApiParam(value = "音频文件", required = true) @RequestParam("file") MultipartFile file,
            AudioUploadRequest request,
            HttpServletRequest httpRequest) {
        // 获取当前登录用户
        Long userId = userService.getLoginUser(httpRequest).getId();
        // 记录上传开始
        log.info("开始上传音频文件, filename={}, userId={}", file.getOriginalFilename(), userId);

        AudioFileVO audioFileVO = audioFileService.uploadAudio(file, userId,
                request.getTitle(), request.getDescription(),
                request.getArtist(), request.getAlbum(),
                request.getGenre(), request.getSpaceId());

        // 记录上传成功
        log.info("音频文件上传成功, filename={}, fileId={}", file.getOriginalFilename(), audioFileVO.getId());
        return ResultUtils.success(audioFileVO);
    }

    /**
     * 删除音频文件
     *
     * @param id      音频文件ID
     * @param request HTTP请求
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    @RateLimiter(key = "audio_delete", time = 60, count = 20, message = "音频删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteAudio(
            @ApiParam(value = "音频文件ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        log.info("开始删除音频文件, fileId={}, userId={}", id, userId);
        boolean result = audioFileService.deleteAudio(id, userId);
        log.info("音频文件删除{}, fileId={}", result ? "成功" : "失败", id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取音频列表
     */
    @GetMapping("/list/page")
    @RateLimiter(key = "audio_list", time = 60, count = 30, message = "音频列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<AudioFileVO>> listAudioByPage(AudioQueryRequest audioQueryRequest,
                                                           HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        audioQueryRequest.setUserId(userId);
        Page<AudioFileVO> page = audioFileService.listAudioVOByPage(audioQueryRequest, request);
        return ResultUtils.success(page);
    }

    /**
     * 根据id获取音频
     */
    @GetMapping("/{id}")
    @RateLimiter(key = "audio_get", time = 60, count = 50, message = "音频详情查询过于频繁，请稍后再试")
    public BaseResponse<AudioFileVO> getAudioById(@PathVariable long id, HttpServletRequest request) {
        AudioFileVO audioFileVO = audioFileService.getAudioVOById(id, request);
        return ResultUtils.success(audioFileVO);
    }

    /**
     * 管理员获取所有音频列表
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AudioFile>> listAudioByPageAdmin(AudioQueryRequest audioQueryRequest) {
        // 管理员查看所有音频，不设置 userId 过滤
        Page<AudioFile> page = audioFileService.listAudioByPage(audioQueryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 管理员批量删除音频
     */
    @DeleteMapping("/admin/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteAudio(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = audioFileService.removeBatchByIds(ids);
        return ResultUtils.success(result);
    }
}
