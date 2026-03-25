package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.message.AddMessage;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageBatchRequest;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Message;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.MessageVO;
import com.lumenglover.yuemupicturebackend.service.MessageService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    @Resource
    private SensitiveUtil sensitiveUtil;

    /**
     * 添加留言
     */
    @PostMapping("/add")
    @RateLimiter(key = "message_add", time = 60, count = 5, message = "留言发送过于频繁，请稍后再试")
    public BaseResponse<Boolean> sendMessage(@RequestBody AddMessage addMessage, HttpServletRequest request) {
        // 获取真实IP地址
        String ip = getIpAddress(request);

        // 敏感词过滤
        String filteredContent = SensitiveUtil.filter(addMessage.getContent());
        addMessage.setContent(filteredContent);

        addMessage.setIp(ip);
        return ResultUtils.success(messageService.addMessage(addMessage));
    }

    /**
     * 删除留言
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMessage(@RequestParam("id") long id) {
        return ResultUtils.success(messageService.deleteMessage(id));
    }

    /**
     * 分页获取留言列表
     */
    @PostMapping("/list/page")
    @RateLimiter(key = "message_list", time = 60, count = 30, message = "留言列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<Message>> listMessageByPage(@RequestBody MessageQueryRequest messageQueryRequest,
                                                         HttpServletRequest request) {
        return ResultUtils.success(messageService.page(messageQueryRequest));
    }

    /**
     * 获取时间排名前500的留言
     */
    @PostMapping("/getTop500")
    @RateLimiter(key = "message_top500", time = 60, count = 20, message = "获取Top500留言过于频繁，请稍后再试")
    public BaseResponse<List<MessageVO>> getTop500(HttpServletRequest request) {
        return ResultUtils.success(messageService.getTop500());
    }

    /**
     * 分页获取留言列表（仅管理员可用）
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Message>> listAdminMessagesByPage(@RequestBody MessageAdminRequest messageAdminRequest) {
        long current = messageAdminRequest.getCurrent();
        long size = messageAdminRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<Message> messagesPage = messageService.page(new Page<>(current, size),
                messageService.getAdminQueryWrapper(messageAdminRequest));
        return ResultUtils.success(messagesPage);
    }

    /**
     * 根据 id 获取留言（仅管理员可用）
     */
    @GetMapping("/get/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Message> getAdminMessageById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Message message = messageService.getById(id);
        ThrowUtils.throwIf(message == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(message);
    }

    /**
     * 批量操作留言（仅管理员可用）
     */
    @PostMapping("/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchAdminOperationMessages(@RequestBody MessageBatchRequest messageBatchRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(messageBatchRequest == null
                        || messageBatchRequest.getIds() == null
                        || messageBatchRequest.getIds().isEmpty(),
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = messageService.batchOperationMessages(messageBatchRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            // 处理本地 IPv6 地址
            if ("0:0:0:0:0:0:0:1".equals(ip)) {
                ip = "127.0.0.1";
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
}
