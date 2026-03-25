package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.comments.*;
import com.lumenglover.yuemupicturebackend.model.entity.Comments;
import com.lumenglover.yuemupicturebackend.model.vo.CommentsVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 鹿梦
 * @description 针对表【comments】的数据库操作Service
 * @createDate 2024-12-29 18:26:42
 */
public interface CommentsService extends IService<Comments> {

    Boolean addComment(CommentsAddRequest commentsAddRequest, HttpServletRequest request);


    Boolean deleteComment(CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request);


    Page<CommentsVO> queryComment(CommentsQueryRequest commentsQueryRequest, HttpServletRequest request);

    Boolean likeComment(CommentsLikeRequest commentslikeRequest, HttpServletRequest request);

    /**
     * 获取并清除用户未读的评论消息
     *
     * @param userId 用户ID
     * @return 未读的评论消息列表
     */
    List<CommentsVO> getAndClearUnreadComments(Long userId);

    /**
     * 获取用户未读评论数
     */
    long getUnreadCommentsCount(Long userId);

    /**
     * 清除用户所有未读评论状态
     */
    void clearAllUnreadComments(Long userId);

    Page<CommentsVO> getCommentedHistory(CommentsQueryRequest commentsQueryRequest, Long id);

    Page<CommentsVO> getMyCommentHistory(CommentsQueryRequest commentsQueryRequest, Long id);

    /**
     * 获取查询条件
     * @param commentsQueryRequest
     * @return
     */
    QueryWrapper<Comments> getQueryWrapper(CommentsQueryRequest commentsQueryRequest);

    /**
     * 批量操作评论
     * @param commentsBatchRequest
     * @return
     */
    boolean batchOperationComments(CommentsBatchRequest commentsBatchRequest);

    /**
     * 管理员获取查询条件
     * @param commentsAdminRequest
     * @return
     */
    QueryWrapper<Comments> getAdminQueryWrapper(CommentsAdminRequest commentsAdminRequest);

    /**
     * 获取用户所有的评论（包括已读和未读）
     * @param userId 用户ID
     * @param current 当前页
     * @param pageSize 页面大小
     * @return 分页的评论列表
     */
    Page<CommentsVO> getAllCommentsByUserId(Long userId, long current, long pageSize);

    /**
     * 标记单个评论为已读
     * @param id 评论ID
     * @return 操作结果
     */
    boolean markCommentAsRead(Long id);
}
