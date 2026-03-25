package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.websocket.MessageWebSocketHandler;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.MessageCenterVO;
import com.lumenglover.yuemupicturebackend.model.vo.CommentsVO;
import com.lumenglover.yuemupicturebackend.model.vo.LikeRecordVO;
import com.lumenglover.yuemupicturebackend.model.vo.ShareRecordVO;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.service.CommentsService;
import com.lumenglover.yuemupicturebackend.service.LikeRecordService;
import com.lumenglover.yuemupicturebackend.service.ShareRecordService;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/message")
@Slf4j
public class MessageCenterController {

    @Resource
    private UserService userService;

    @Resource
    private CommentsService commentsService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private ShareRecordService shareRecordService;

    @Resource
    private SystemNotifyService systemNotifyService;

    @Resource
    private MessageWebSocketHandler messageWebSocketHandler;

    /**
     * 统一查询接口：按类型查询用户的所有评论、点赞、分享、系统信息
     * @param type 消息类型：comment(评论)、like(点赞)、share(分享)、system(系统通知)
     * @param current 当前页
     * @param pageSize 页面大小
     * @param request HTTP请求
     * @return 分页数据
     */
    @GetMapping("/list")
    @RateLimiter(key = "message_center_list", time = 60, count = 25, message = "消息中心列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<?>> getMessageList(
            @RequestParam("type") String type,
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        try {
            switch (type) {
                case "comment":
                    return ResultUtils.success(commentsService.getAllCommentsByUserId(loginUser.getId(), current, pageSize));
                case "like":
                    return ResultUtils.success(likeRecordService.getAllLikesByUserId(loginUser.getId(), current, pageSize));
                case "share":
                    return ResultUtils.success(shareRecordService.getAllSharesByUserId(loginUser.getId(), current, pageSize));
                case "system":
                    return ResultUtils.success(systemNotifyService.getAllNotifiesByUserId(String.valueOf(loginUser.getId()), current, pageSize));
                default:
                    return (BaseResponse<Page<?>>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "不支持的消息类型");
            }
        } catch (Exception e) {
            log.error("获取消息列表失败: ", e);
            return (BaseResponse<Page<?>>) ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取消息列表失败");
        }
    }

    /**
     * 统一清除单个未读接口：用户传递类型和ID清除对应的单个未读
     * @param type 消息类型：comment(评论)、like(点赞)、share(分享)、system(系统通知)
     * @param id 消息ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/read/{type}/{id}")
    @RateLimiter(key = "message_center_read_single", time = 60, count = 20, message = "消息中心单个标记已读过于频繁，请稍后再试")
    public BaseResponse<Boolean> markSingleAsRead(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        try {
            boolean result = false;
            switch (type) {
                case "comment":
                    result = commentsService.markCommentAsRead(id);
                    break;
                case "like":
                    result = likeRecordService.markLikeAsRead(id);
                    break;
                case "share":
                    result = shareRecordService.markShareAsRead(id);
                    break;
                case "system":
                    result = systemNotifyService.markAsRead(id, String.valueOf(loginUser.getId()));
                    break;
                default:
                    return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.PARAMS_ERROR, "不支持的消息类型");
            }

            // 清除成功后通知WebSocket更新
            if (result) {
                sendWebSocketNotification(loginUser);
            }

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("标记消息为已读失败: ", e);
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.SYSTEM_ERROR, "标记消息为已读失败");
        }
    }

    /**
     * 获取所有未读消息总数和单个类型未读总数
     * @param request HTTP请求
     * @return 未读消息统计
     */
    @GetMapping("/unread/count/all")
    @RateLimiter(key = "message_center_unread_count", time = 60, count = 30, message = "消息中心未读统计查询过于频繁，请稍后再试")
    public BaseResponse<MessageCenterVO> getAllUnreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        MessageCenterVO messageCenterVO = new MessageCenterVO();

        // 获取各类型未读数
        long unreadComments = commentsService.getUnreadCommentsCount(loginUser.getId());
        long unreadLikes = likeRecordService.getUnreadLikesCount(loginUser.getId());
        long unreadShares = shareRecordService.getUnreadSharesCount(loginUser.getId());
        long unreadSystemNotifies = systemNotifyService.getUserUnreadCount(loginUser.getId().toString());

        // 设置数据
        messageCenterVO.setUnreadComments(unreadComments);
        messageCenterVO.setUnreadLikes(unreadLikes);
        messageCenterVO.setUnreadShares(unreadShares);
        messageCenterVO.setUnreadSystemNotifies(unreadSystemNotifies);
        messageCenterVO.setTotalUnread(unreadComments + unreadLikes + unreadShares + unreadSystemNotifies);

        return ResultUtils.success(messageCenterVO);
    }

    /**
     * 清理所有未读消息
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/read/all")
    @RateLimiter(key = "message_center_read_all", time = 60, count = 10, message = "消息中心全部标记已读过于频繁，请稍后再试")
    public BaseResponse<Boolean> markAllAsRead(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        try {
            // 异步处理清除所有未读消息
            CompletableFuture.runAsync(() -> {
                try {
                    // 清除所有类型的未读状态
                    commentsService.clearAllUnreadComments(loginUser.getId());
                    likeRecordService.clearAllUnreadLikes(loginUser.getId());
                    shareRecordService.clearAllUnreadShares(loginUser.getId());
                    systemNotifyService.markAllAsRead(loginUser.getId().toString());
                    // 所有消息清除完成后，推送一次WebSocket更新
                    messageWebSocketHandler.sendUnreadCountToUser(loginUser.getId().toString());
                } catch (Exception e) {
                    log.error("异步清除未读消息失败: ", e);
                }
            });

            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in markAllAsRead: ", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 通过WebSocket推送消息给用户
     * @param user 用户信息
     */
    private void sendWebSocketNotification(User user) {
        try {
            // 获取最新的未读消息数
            MessageCenterVO messageCenterVO = new MessageCenterVO();

            // 获取各类型未读数
            long unreadComments = commentsService.getUnreadCommentsCount(user.getId());
            long unreadLikes = likeRecordService.getUnreadLikesCount(user.getId());
            long unreadShares = shareRecordService.getUnreadSharesCount(user.getId());
            long unreadSystemNotifies = systemNotifyService.getUserUnreadCount(user.getId().toString());

            // 设置数据
            messageCenterVO.setUnreadComments(unreadComments);
            messageCenterVO.setUnreadLikes(unreadLikes);
            messageCenterVO.setUnreadShares(unreadShares);
            messageCenterVO.setUnreadSystemNotifies(unreadSystemNotifies);
            messageCenterVO.setTotalUnread(unreadComments + unreadLikes + unreadShares + unreadSystemNotifies);

            // 发送WebSocket通知
            messageWebSocketHandler.sendUnreadCountToUser(user.getId().toString());
        } catch (Exception e) {
            log.error("发送WebSocket通知失败", e);
        }
    }
}
