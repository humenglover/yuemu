package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.FileUploadService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 通用文件上传接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private UserService userService;

    @PostMapping("/upload/picture")
    public BaseResponse<PictureVO> commonUploadPicture(
            @ApiParam("图片文件") @RequestParam("file") MultipartFile file,
            @ApiParam("图片名称") @RequestParam(required = false) String name,
            @ApiParam("图片描述") @RequestParam(required = false) String description,
            @ApiParam("标签") @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = fileUploadService.uploadPicture(file, loginUser.getId(), name, description, tags);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/audio")
    public BaseResponse<AudioFileVO> commonUploadAudio(
            @ApiParam("音频文件") @RequestParam("file") MultipartFile file,
            @ApiParam("音频标题") @RequestParam(required = false) String title,
            @ApiParam("音频描述") @RequestParam(required = false) String description,
            @ApiParam("艺术家") @RequestParam(required = false) String artist,
            @ApiParam("标签") @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AudioFileVO audioFileVO = fileUploadService.uploadAudio(file, loginUser.getId(), title, description, artist, tags);
        return ResultUtils.success(audioFileVO);
    }
}
