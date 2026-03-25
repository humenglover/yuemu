package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.ShareRecordVO;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareRequest;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareQueryRequest;
import com.lumenglover.yuemupicturebackend.service.ShareRecordService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.lumenglover.yuemupicturebackend.model.entity.ShareRecord;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.share.ShareBatchRequest;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareRecordController {

    @Resource
    private ShareRecordService shareRecordService;

    @Resource
    private UserService userService;

    /**
     * 通用分享接口
     */
    @PostMapping("/do")
    @RateLimiter(key = "share_do", time = 60, count = 20, message = "分享操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> doShare(@RequestBody ShareRequest shareRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        try {
            CompletableFuture<Boolean> future = shareRecordService.doShare(shareRequest, loginUser.getId());
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in doShare controller: ", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 获取分享状态
     */
    @GetMapping("/status/{targetType}/{targetId}")
    @RateLimiter(key = "share_status", time = 60, count = 30, message = "分享状态查询过于频繁，请稍后再试")
    public BaseResponse<Boolean> getShareStatus(
            @PathVariable("targetType") Integer targetType,
            @PathVariable("targetId") Long targetId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean isShared = shareRecordService.isContentShared(targetId, targetType, loginUser.getId());
        return ResultUtils.success(isShared);
    }

    /**
     * 获取未读分享消息
     */
    @GetMapping("/unread")
    @RateLimiter(key = "share_unread", time = 60, count = 30, message = "未读分享查询过于频繁，请稍后再试")
    public BaseResponse<List<ShareRecordVO>> getUnreadShares(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        List<ShareRecordVO> unreadShares = shareRecordService.getAndClearUnreadShares(loginUser.getId());
        return ResultUtils.success(unreadShares);
    }

    /**
     * 获取用户被分享历史
     */
    @PostMapping("/history")
    @RateLimiter(key = "share_history", time = 60, count = 20, message = "分享历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<ShareRecordVO>> getShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> shareHistory = shareRecordService.getUserShareHistory(shareQueryRequest, loginUser.getId());
        return ResultUtils.success(shareHistory);
    }

    /**
     * 获取我的分享历史
     */
    @PostMapping("/my/history")
    @RateLimiter(key = "share_my_history", time = 60, count = 20, message = "我的分享历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<ShareRecordVO>> getMyShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> shareHistory = shareRecordService.getMyShareHistory(shareQueryRequest, loginUser.getId());
        return ResultUtils.success(shareHistory);
    }

    @GetMapping("/unread/count")
    @RateLimiter(key = "share_unread_count", time = 60, count = 50, message = "未读分享数量查询过于频繁，请稍后再试")
    public BaseResponse<Long> getUnreadSharesCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(shareRecordService.getUnreadSharesCount(loginUser.getId()));
    }

    /**
     * 分页获取分享记录列表（仅管理员可用）
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ShareRecord>> listSharesByPage(@RequestBody ShareAdminRequest shareAdminRequest) {
        long current = shareAdminRequest.getCurrent();
        long size = shareAdminRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<ShareRecord> sharesPage = shareRecordService.page(new Page<>(current, size),
                shareRecordService.getAdminQueryWrapper(shareAdminRequest));
        return ResultUtils.success(sharesPage);
    }

    /**
     * 根据 id 获取分享记录（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ShareRecord> getShareById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ShareRecord share = shareRecordService.getById(id);
        ThrowUtils.throwIf(share == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(share);
    }

    /**
     * 更新分享记录（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> updateShare(@RequestBody ShareRecord share) {
        ThrowUtils.throwIf(share == null || share.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = shareRecordService.updateById(share);
        return ResultUtils.success(result);
    }

    /**
     * 批量操作分享记录（仅管理员可用）
     */
    @PostMapping("/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchOperationShares(@RequestBody ShareBatchRequest shareBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(shareBatchRequest == null
                        || shareBatchRequest.getIds() == null
                        || shareBatchRequest.getIds().isEmpty(),
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = shareRecordService.batchOperationShares(shareBatchRequest);
        return ResultUtils.success(result);
    }
}
