package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.model.dto.yolo.YoloResponseDTO;
import com.lumenglover.yuemupicturebackend.service.YoloService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/yolo")
@Slf4j
public class YoloController {

    @Resource
    private YoloService yoloService;

    @PostMapping("/detect")
    public BaseResponse<YoloResponseDTO> detectObjects(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return (BaseResponse<YoloResponseDTO>) ResultUtils.error(400, "文件不能为空");
        }
        try {
            YoloResponseDTO result = yoloService.detectObjects(file);
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("YOLO detection failed", e);
            return (BaseResponse<YoloResponseDTO>) ResultUtils.error(500, "检测失败：" + e.getMessage());
        }
    }

    @PostMapping("/detect/url")
    public BaseResponse<YoloResponseDTO> detectObjectsByUrl(@RequestParam("imageUrl") String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return (BaseResponse<YoloResponseDTO>) ResultUtils.error(400, "图片链接不能为空");
        }
        try {
            YoloResponseDTO result = yoloService.detectObjectsFromUrl(imageUrl);
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("YOLO detection by URL failed", e);
            return (BaseResponse<YoloResponseDTO>) ResultUtils.error(500, "检测失败：" + e.getMessage());
        }
    }
}
