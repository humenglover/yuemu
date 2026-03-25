package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsDeleteRequest;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsLikeRequest;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsBatchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Comments;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.CommentsVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.CommentsService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.lumenglover.yuemupicturebackend.model.dto.comments.CommentsAdminRequest;

@Slf4j
@RestController
@RequestMapping("/comments")
public class CommentsController {
    @Resource
    private CommentsService commentsService;

    @Resource
    private UserService userService;

    @Resource
    private CrawlerManager crawlerManager;

    /**
     * 查询指定图片的评论列表
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 评论列表（分页）
     */
    @PostMapping("/query")
    @RateLimiter(key = "comment_query", time = 60, count = 60, message = "评论查询过于频繁，请稍后再试")
    public BaseResponse<Page<CommentsVO>> queryComment(@RequestBody CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        // 用户权限校验
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            String userRole = loginUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        return ResultUtils.success(commentsService.queryComment(commentsQueryRequest, request));
    }

    /**
     * 添加评论
     * @param commentsAddRequest 评论内容请求
     * @param request HTTP请求
     * @return 添加结果
     */
    @PostMapping("/add")
    @RateLimiter(key = "comment_add", time = 60, count = 60, message = "评论添加过于频繁，请稍后再试")
    public BaseResponse<Boolean> addComment(@RequestBody CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.addComment(commentsAddRequest, request));
    }

    /**
     * 删除评论
     * @param commentsDeleteRequest 删除评论请求
     * @param request HTTP请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @RateLimiter(key = "comment_delete", time = 60, count = 60, message = "评论删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteComment(@RequestBody CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.deleteComment(commentsDeleteRequest, request));
    }

    /**
     * 点赞评论
     * @param commentslikeRequest 评论点赞请求
     * @param request HTTP请求
     * @return 点赞结果
     */
    @PostMapping("/like")
    @RateLimiter(key = "comment_like", time = 60, count = 60, message = "评论点赞过于频繁，请稍后再试")
    public BaseResponse<Boolean> likeComment(@RequestBody CommentsLikeRequest commentslikeRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.likeComment(commentslikeRequest, request));
    }

    /**
     * 获取未读评论列表
     * @param request HTTP请求
     * @return 未读评论列表
     */
    @GetMapping("/unread")
    @RateLimiter(key = "comment_unread", time = 60, count = 60, message = "未读评论查询过于频繁，请稍后再试")
    public BaseResponse<List<CommentsVO>> getUnreadComments(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检测普通请求
        crawlerManager.detectNormalRequest(request);

        List<CommentsVO> unreadComments = commentsService.getAndClearUnreadComments(loginUser.getId());
        return ResultUtils.success(unreadComments);
    }

    /**
     * 获取未读评论数量
     * @param request HTTP请求
     * @return 未读评论数量
     */
    @GetMapping("/unread/count")
    @RateLimiter(key = "comment_unread_count", time = 60, count = 60, message = "未读评论数量查询过于频繁，请稍后再试")
    public BaseResponse<Long> getUnreadCommentsCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检测普通请求
        crawlerManager.detectNormalRequest(request);

        return ResultUtils.success(commentsService.getUnreadCommentsCount(loginUser.getId()));
    }

    /**
     * 获取我的评论历史
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 我的评论历史（分页）
     */
    @PostMapping("/my/history")
    @RateLimiter(key = "comment_my_history", time = 60, count = 60, message = "我的评论历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<CommentsVO>> getMyCommentHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        Page<CommentsVO> commentHistory = commentsService.getMyCommentHistory(commentsQueryRequest, loginUser.getId());
        return ResultUtils.success(commentHistory);
    }

    /**
     * 获取评论我的历史
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 评论我的历史（分页）
     */
    @PostMapping("/commented/history")
    @RateLimiter(key = "comment_commented_history", time = 60, count = 60, message = "评论我的历史查询过于频繁，请稍后再试")
    public BaseResponse<Page<CommentsVO>> getCommentedHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        Page<CommentsVO> commentHistory = commentsService.getCommentedHistory(commentsQueryRequest, loginUser.getId());
        return ResultUtils.success(commentHistory);
    }

    /**
     * 分页获取评论列表（仅管理员可用）
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "comment_list_admin", time = 60, count = 60, message = "管理员评论列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<Comments>> listCommentsByPage(@RequestBody CommentsAdminRequest commentsAdminRequest) {
        long current = commentsAdminRequest.getCurrent();
        long size = commentsAdminRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<Comments> commentsPage = commentsService.page(new Page<>(current, size),
                commentsService.getAdminQueryWrapper(commentsAdminRequest));
        return ResultUtils.success(commentsPage);
    }

    /**
     * 根据 id 获取评论（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "comment_get", time = 60, count = 60, message = "评论详情查询过于频繁，请稍后再试")
    public BaseResponse<Comments> getCommentById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Comments comment = commentsService.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(comment);
    }

    /**
     * 更新评论（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional(rollbackFor = Exception.class)
    @RateLimiter(key = "comment_update", time = 60, count = 60, message = "评论更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateComment(@RequestBody Comments comment) {
        ThrowUtils.throwIf(comment == null || comment.getCommentId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = commentsService.updateById(comment);
        return ResultUtils.success(result);
    }

    /**
     * 批量操作评论（仅管理员可用）
     */
    @PostMapping("/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "comment_batch", time = 60, count = 60, message = "评论批量操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> batchOperationComments(@RequestBody CommentsBatchRequest commentsBatchRequest,
                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(commentsBatchRequest == null
                        || commentsBatchRequest.getIds() == null
                        || commentsBatchRequest.getIds().isEmpty(),
                ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = commentsService.batchOperationComments(commentsBatchRequest);
        return ResultUtils.success(result);
    }
}
