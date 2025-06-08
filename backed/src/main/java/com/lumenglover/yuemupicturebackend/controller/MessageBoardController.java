package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;

import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.MessageBoard;
import com.lumenglover.yuemupicturebackend.service.MessageBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 祝福板接口
 */
@RestController
@RequestMapping("/message-board")
@Slf4j
public class MessageBoardController {

    @Resource
    private MessageBoardService messageBoardService;

    @PostMapping("/add")
    public BaseResponse<Boolean> addMessage(@RequestBody MessageBoard messageBoard, HttpServletRequest request) {
        if (messageBoard == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String content = messageBoard.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "祝福内容不能为空");
        }
        if (messageBoard.getOwnerId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "祝福板主人ID不能为空");
        }
        // 获取客户端信息，并限制长度
        messageBoard.setIpAddress(request.getRemoteAddr());
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 255) {
            userAgent = userAgent.substring(0, 255);
        }
        messageBoard.setBrowser(userAgent);
        return ResultUtils.success(messageBoardService.addMessage(messageBoard));
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<MessageBoard>> listMessagesByPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "祝福板主人ID不能为空");
        }
        return ResultUtils.success(messageBoardService.listMessagesByPage(current, size, ownerId));
    }

    @PostMapping("/like/{id}")
    public BaseResponse<Boolean> likeMessage(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(messageBoardService.likeMessage(id));
    }

    @PostMapping("/status")
    public BaseResponse<Boolean> updateMessageStatus(@RequestParam Long id, @RequestParam Integer status) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值不合法");
        }
        return ResultUtils.success(messageBoardService.updateMessageStatus(id, status));
    }

    @DeleteMapping("/{id}")

    public BaseResponse<Boolean> deleteMessage(@PathVariable Long id, @RequestParam Long ownerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (ownerId == null || ownerId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "祝福板主人ID不能为空");
        }
        return ResultUtils.success(messageBoardService.deleteMessage(id, ownerId));
    }
}
