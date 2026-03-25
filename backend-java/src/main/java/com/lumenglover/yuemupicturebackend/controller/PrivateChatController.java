package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.PrivateChatService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/private_chat")
@Slf4j
public class PrivateChatController {

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    private UserService userService;

    /**
     * 分页获取私聊列表
     */
    @PostMapping("/list/page")
    @RateLimiter(key = "private_chat_list", time = 60, count = 25, message = "私聊列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<PrivateChat>> listPrivateChatByPage(@RequestBody PrivateChatQueryRequest privateChatQueryRequest,
                                                                 HttpServletRequest request) {
        if (privateChatQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long current = privateChatQueryRequest.getCurrent();
        long size = privateChatQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<PrivateChat> privateChatPage = privateChatService.page(new Page<>(current, size),
                privateChatService.getQueryWrapper(privateChatQueryRequest, loginUser), request);
        return ResultUtils.success(privateChatPage);
    }

    /**
     * 清空未读消息数
     */
    @PostMapping("/clear_unread/{targetUserId}/{isSender}")
    @RateLimiter(key = "private_chat_clear_unread", time = 60, count = 20, message = "清空未读消息过于频繁，请稍后再试")
    public BaseResponse<Boolean> clearUnreadCount(@PathVariable Long targetUserId,@PathVariable boolean isSender,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 清除当前用户的未读消息数
        privateChatService.clearUnreadCount(loginUser.getId(), targetUserId, isSender);
        return ResultUtils.success(true);
    }

    /**
     * 更新聊天类型（好友/私信）
     */
    @PostMapping("/update_type/{targetUserId}")
    @RateLimiter(key = "private_chat_update_type", time = 60, count = 15, message = "更新聊天类型过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateChatType(@PathVariable Long targetUserId,
                                                @RequestParam Boolean isFriend,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        privateChatService.updateChatType(loginUser.getId(), targetUserId, isFriend);
        return ResultUtils.success(true);
    }

    /**
     * 创建或更新私聊
     */
    @PostMapping("/create_update")
    @RateLimiter(key = "private_chat_create_update", time = 60, count = 10, message = "创建或更新私聊过于频繁，请稍后再试")
    public BaseResponse<PrivateChat> createOrUpdatePrivateChat(@RequestParam Long targetUserId,
                                                               @RequestParam(required = false) String lastMessage,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        // 不能和自己私聊
        ThrowUtils.throwIf(targetUserId.equals(loginUser.getId()), ErrorCode.PARAMS_ERROR, "不能和自己私聊");

        // 检查目标用户是否存在
        User targetUser = userService.getById(targetUserId);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "目标用户不存在");

        // 检查目标用户是否允许私聊
        if (targetUser.getAllowPrivateChat() != null && targetUser.getAllowPrivateChat() == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "对方不允许私聊");
        }

        PrivateChat privateChat = privateChatService.createOrUpdatePrivateChat(loginUser.getId(), targetUserId, lastMessage);
        return ResultUtils.success(privateChat);
    }

    /**
     * 删除私聊
     */
    @PostMapping("/delete/{privateChatId}")
    @RateLimiter(key = "private_chat_delete", time = 60, count = 15, message = "删除私聊过于频繁，请稍后再试")
    public BaseResponse<Boolean> deletePrivateChat(@PathVariable Long privateChatId,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(privateChatId == null || privateChatId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = privateChatService.deletePrivateChat(privateChatId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 修改私聊名称
     */
    @PostMapping("/update_name/{privateChatId}")
    @RateLimiter(key = "private_chat_update_name", time = 60, count = 15, message = "修改私聊名称过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateChatName(@PathVariable Long privateChatId,
                                                @RequestParam String chatName,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(privateChatId == null || privateChatId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(chatName.length() > 50, ErrorCode.PARAMS_ERROR, "聊天名称过长");

        User loginUser = userService.getLoginUser(request);
        privateChatService.updateChatName(privateChatId, chatName, loginUser);
        return ResultUtils.success(true);
    }
}
