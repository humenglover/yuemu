package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.LikeRecordVO;
import com.lumenglover.yuemupicturebackend.service.LikeRecordService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.lumenglover.yuemupicturebackend.model.entity.LikeRecord;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeBatchRequest;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/like")
@Slf4j
public class LikeRecordController {

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserService userService;

    /**
     * 通用点赞接口
     */
    @PostMapping("/do")
    @RateLimiter(key = "like_do", time = 60, count = 20, message = "点赞操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> doLike(@RequestBody LikeRequest likeRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        try {
            CompletableFuture<Boolean> future = likeRecordService.doLike(likeRequest, loginUser.getId());
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in doLike controller: ", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 获取点赞状态
     */
    @GetMapping("/status/{targetType}/{targetId}")
    @RateLimiter(key = "like_status", time = 60, count = 30, message = "点赞状态查询过于频繁，请稍后再试")
    public BaseResponse<Boolean> getLikeStatus(
            @PathVariable("targetType") Integer targetType,
            @PathVariable("targetId") Long targetId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean isLiked = likeRecordService.isContentLiked(targetId, targetType, loginUser.getId());
        return ResultUtils.success(isLiked);
    }

    /**
     * 获取未读点赞消息
     */
    @GetMapping("/unread")
    @RateLimiter(key = "like_unread", time = 60, count = 20, message = "未读点赞消息查询过于频繁，请稍后再试")
    public BaseResponse<List<LikeRecordVO>> getUnreadLikes(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        List<LikeRecordVO> unreadLikes = likeRecordService.getAndClearUnreadLikes(loginUser.getId());
        return ResultUtils.success(unreadLikes);
    }

    /**
     * 获取用户被点赞历史
     */
    @PostMapping("/history")
    @RateLimiter(key = "like_history", time = 60, count = 25, message = "用户被点赞历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<LikeRecordVO>> getLikeHistory(@RequestBody LikeQueryRequest likeQueryRequest,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = likeQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<LikeRecordVO> likeHistory = likeRecordService.getUserLikeHistory(likeQueryRequest, loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    /**
     * 获取我的点赞历史
     */
    @PostMapping("/my/history")
    @RateLimiter(key = "like_my_history", time = 60, count = 25, message = "我的点赞历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<LikeRecordVO>> getMyLikeHistory(@RequestBody LikeQueryRequest likeQueryRequest,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = likeQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<LikeRecordVO> likeHistory = likeRecordService.getMyLikeHistory(likeQueryRequest, loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    @GetMapping("/unread/count")
    @RateLimiter(key = "like_unread_count", time = 60, count = 30, message = "未读点赞消息数量查询过于频繁，请稍后再试")
    public BaseResponse<Long> getUnreadLikesCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(likeRecordService.getUnreadLikesCount(loginUser.getId()));
    }

    /**
     * 分页获取点赞记录列表（仅管理员可用）
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "like_list_admin", time = 60, count = 15, message = "管理员点赞列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<LikeRecord>> listLikesByPage(@RequestBody LikeAdminRequest likeAdminRequest) {
        long current = likeAdminRequest.getCurrent();
        long size = likeAdminRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<LikeRecord> likesPage = likeRecordService.page(new Page<>(current, size),
                likeRecordService.getAdminQueryWrapper(likeAdminRequest));
        return ResultUtils.success(likesPage);
    }

    /**
     * 根据 id 获取点赞记录（仅管理员可用）
     */
    @GetMapping("/get/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "like_get_admin", time = 60, count = 20, message = "点赞记录详情查询过于频繁，请稍后再试")
    public BaseResponse<LikeRecord> getLikeById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        LikeRecord like = likeRecordService.getById(id);
        ThrowUtils.throwIf(like == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(like);
    }

    /**
     * 更新点赞记录（仅管理员可用）
     */
    @PostMapping("/update/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional(rollbackFor = Exception.class)
    @RateLimiter(key = "like_update_admin", time = 60, count = 10, message = "点赞记录更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateLike(@RequestBody LikeRecord like) {
        ThrowUtils.throwIf(like == null || like.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = likeRecordService.updateById(like);
        return ResultUtils.success(result);
    }

    /**
     * 批量操作点赞记录（仅管理员可用）
     */
    @PostMapping("/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "like_batch", time = 60, count = 10, message = "点赞批量操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> batchOperationLikes(@RequestBody LikeBatchRequest likeBatchRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(likeBatchRequest == null
                        || likeBatchRequest.getIds() == null
                        || likeBatchRequest.getIds().isEmpty(),
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = likeRecordService.batchOperationLikes(likeBatchRequest);
        return ResultUtils.success(result);
    }
}
