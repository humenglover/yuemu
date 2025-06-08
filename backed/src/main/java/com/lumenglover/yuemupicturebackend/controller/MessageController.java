package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.message.AddMessage;
import com.lumenglover.yuemupicturebackend.model.dto.message.MessageQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Message;
import com.lumenglover.yuemupicturebackend.model.vo.MessageVO;
import com.lumenglover.yuemupicturebackend.service.MessageService;
import com.lumenglover.yuemupicturebackend.utils.RateLimiter;
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
    private RateLimiter rateLimiter;

    /**
     * 添加留言
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> sendMessage(@RequestBody AddMessage addMessage, HttpServletRequest request) {
        // 获取真实IP地址
        String ip = getIpAddress(request);

        // 检查IP频率限制
        if (!rateLimiter.allowMessageAdd(ip)) {
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.TOO_MANY_REQUEST, "发送太频繁，请稍后再试");
        }

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
    public BaseResponse<Page<Message>> listMessageByPage(@RequestBody MessageQueryRequest messageQueryRequest,
                                                         HttpServletRequest request) {
        String ip = getIpAddress(request);
        if (!rateLimiter.allowMessageQuery(ip)) {
            return (BaseResponse<Page<Message>>) ResultUtils.error(ErrorCode.TOO_MANY_REQUEST, "查询太频繁，请稍后再试");
        }
        return ResultUtils.success(messageService.page(messageQueryRequest));
    }

    /**
     * 获取时间排名前500的留言
     */
    @PostMapping("/getTop500")
    public BaseResponse<List<MessageVO>> getTop500(HttpServletRequest request) {
        String ip = getIpAddress(request);
        if (!rateLimiter.allowMessageQuery(ip)) {
            return (BaseResponse<List<MessageVO>>) ResultUtils.error(ErrorCode.TOO_MANY_REQUEST, "查询太频繁，请稍后再试");
        }
        return ResultUtils.success(messageService.getTop500());
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
