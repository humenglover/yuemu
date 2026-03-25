package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.websocket.MessageWebSocketHandler;
import com.lumenglover.yuemupicturebackend.mapper.CommentsMapper;
import com.lumenglover.yuemupicturebackend.mapper.PictureMapper;
import com.lumenglover.yuemupicturebackend.model.dto.comments.*;
import com.lumenglover.yuemupicturebackend.model.entity.Comments;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.CommentUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.CommentsVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.utils.ip.AddressUtils;
import com.lumenglover.yuemupicturebackend.utils.ServletUtils;
import com.lumenglover.yuemupicturebackend.utils.PicturePermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements CommentsService {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;



    @Resource
    @Lazy
    private MessageWebSocketHandler messageWebSocketHandler;

    @Resource
    private SystemNotifyService systemNotifyService;

    @Resource
    private PicturePermissionUtils picturePermissionUtils;

    @Override
    public Boolean addComment(CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取目标内容所属用户ID
        Long targetUserId;
        switch (commentsAddRequest.getTargetType()) {
            case 1: // 图片
                Picture picture = pictureService.getById(commentsAddRequest.getTargetId());
                if (picture == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
                }

                // 检查用户是否有权限评论此图片
                boolean canComment = picturePermissionUtils.canCommentPicture(picture, user);
                if (!canComment) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限评论此图片");
                }

                targetUserId = picture.getUserId();
                break;
            case 2: // 帖子
                Post post = postService.getById(commentsAddRequest.getTargetId());
                if (post == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
                }

                // 检查帖子评论权限
                boolean hasCommentPermission = postService.checkPostPermission(post, "comment");
                if (!hasCommentPermission) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该帖子不允许评论");
                }

                targetUserId = post.getUserId();
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的评论类型");
        }

        Comments comments = new Comments();
        BeanUtils.copyProperties(commentsAddRequest, comments);
        // 敏感词过滤
        String filteredContent = SensitiveUtil.filter(commentsAddRequest.getContent());
        if (StrUtil.isBlank(filteredContent)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能全为敏感词汇");
        }
        comments.setContent(filteredContent);
        comments.setUserId(user.getId());  // 设置评论用户ID
        comments.setTargetUserId(targetUserId);
        comments.setIsRead(user.getId().equals(targetUserId) ? 1 : 0);
        comments.setLikeCount(0L);  // 设置初始点赞数
        comments.setDislikeCount(0L);  // 设置初始点踩数
        comments.setIsDelete(0);  // 设置未删除状态

        // 获取用户IP并解析位置信息（只获取省份）
        String userIP = ServletUtils.getClientIP(request);
        String location = "未知位置"; // 默认位置
        try {
            location = AddressUtils.getRealAddressByIP(userIP, AddressUtils.AddressLevel.ONLY_PROVINCE);
        } catch (Exception e) {
            log.warn("获取用户IP位置信息失败，IP：{}，错误：{}", userIP, e.getMessage());
        }
        comments.setLocation(location);

        // 设置根评论ID
        if (comments.getParentCommentId() == 0) {
            // 顶级评论：先保存评论，再更新rootCommentId为自己
            boolean result = save(comments);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
            }
            // 更新rootCommentId为自身的commentId
            Long commentId = comments.getCommentId();
            if (commentId != null) {
                comments.setRootCommentId(commentId);
                updateById(comments);
            }
        } else {
            // 子评论：根评论ID = 父评论的根评论ID
            Comments parentComment = this.getById(comments.getParentCommentId());
            if (parentComment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
            }
            comments.setRootCommentId(parentComment.getRootCommentId());

            boolean result = save(comments);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
            }
        }

        // 更新评论数
        updateCommentCount(commentsAddRequest.getTargetId(), commentsAddRequest.getTargetType(), 1);

        // 通过WebSocket推送消息给目标用户
        if (targetUserId != null && !targetUserId.equals(user.getId())) {
            sendCommentWebSocketNotification(targetUserId.toString());
        }

        // 如果是回复评论（有父评论），向被回复的用户发送通知
        if (commentsAddRequest.getParentCommentId() != null && commentsAddRequest.getParentCommentId() > 0) {
            // 查询父评论的用户ID
            Comments parentComment = this.getById(commentsAddRequest.getParentCommentId());
            if (parentComment != null && !parentComment.getUserId().equals(user.getId())) {
                // 只有当被回复用户不是当前用户，也不是目标内容的作者时才发送通知
                if (!parentComment.getUserId().equals(targetUserId)) {
                    // 发送系统通知给被回复的用户
                    sendCommentReplyNotification(parentComment.getUserId(), user, commentsAddRequest.getParentCommentId(), comments.getContent(), commentsAddRequest.getTargetType(), commentsAddRequest.getTargetId());
                }
            }
        }

        return true;
    }

    /**
     * 通过WebSocket推送消息给目标用户
     */
    private void sendCommentWebSocketNotification(String targetUserId) {
        if (messageWebSocketHandler == null) {
            log.warn("WebSocket处理器未初始化，无法推送通知给用户 {}", targetUserId);
            return;
        }
        try {
            messageWebSocketHandler.sendUnreadCountToUser(targetUserId);
        } catch (Exception e) {
            log.error("通过WebSocket推送评论通知给用户 {} 失败", targetUserId, e);
        }
    }

    /**
     * 发送评论回复通知
     */
    private void sendCommentReplyNotification(Long targetUserId, User currentUser, Long parentCommentId, String content, Integer targetType, Long targetId) {
        try {
            // 创建系统通知
            SystemNotify systemNotify = new SystemNotify();
            systemNotify.setTitle("评论回复通知");
            systemNotify.setNotifyType("COMMENT_REPLY");
            systemNotify.setReceiverType("SPECIFIC_USER");
            systemNotify.setReceiverId(targetUserId.toString());
            systemNotify.setSenderId(currentUser.getId().toString());
            systemNotify.setSenderType("USER");
            systemNotify.setIsEnabled(1);
            systemNotify.setIsGlobal(0);
            systemNotify.setReadStatus(0);

            // 获取被回复的评论内容，用于通知
            Comments parentComment = this.getById(parentCommentId);
            String parentCommentContent = "";
            if (parentComment != null && parentComment.getContent() != null) {
                parentCommentContent = parentComment.getContent();
                if (parentCommentContent.length() > 20) {
                    parentCommentContent = parentCommentContent.substring(0, 20) + "...";
                }
            }

            // 根据目标类型生成通知内容
            String targetTypeName = targetType == 1 ? "图片" : "帖子";
            systemNotify.setContent(String.format("用户 %s 回复了您在 %s 下的评论：\"%s\"，回复内容为：\"%s\"。点击查看详情。",
                    currentUser.getUserName(), targetTypeName, parentCommentContent, content));

            // 设置关联业务类型和ID，便于前端跳转
            systemNotify.setRelatedBizType("COMMENT");
            systemNotify.setRelatedBizId(parentCommentId.toString());

            // 保存通知
            systemNotifyService.addSystemNotify(systemNotify);

            // 通过WebSocket推送通知给被回复用户
            sendCommentWebSocketNotification(targetUserId.toString());
        } catch (Exception e) {
            log.error("发送评论回复通知给用户 {} 失败", targetUserId, e);
        }
    }

    /**
     * 更新评论数
     */
    private void updateCommentCount(Long targetId, Integer targetType, int delta) {
        if (delta == 0) {
            return;
        }
        switch (targetType) {
            case 1: // 图片
                pictureService.update(new LambdaUpdateWrapper<Picture>()
                        .setSql("commentCount = commentCount + " + delta)
                        .eq(Picture::getId, targetId)
                        .ge(Picture::getCommentCount, -delta)); // 防止评论数为负
                break;
            case 2: // 帖子
                postService.update(new LambdaUpdateWrapper<Post>()
                        .setSql("commentCount = commentCount + " + delta)
                        .eq(Post::getId, targetId)
                        .ge(Post::getCommentCount, -delta)); // 防止评论数为负
                break;
            default:
                log.error("不支持的目标类型: {}, 目标ID: {}, 变化量: {}", targetType, targetId, delta);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        // 判断是否登录
        User user = userService.getLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取评论信息
        Comments comment = this.getById(commentsDeleteRequest.getCommentId());
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

        // 校验权限（只能删除自己的评论）
        if (!user.getId().equals(comment.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 计算要删除的评论总数（当前评论 + 所有子评论）
        int childCommentCount = countCommentsRecursively(comment.getCommentId());
        int deletedCommentCount = childCommentCount + 1;

        // 批量删除评论及其子评论
        boolean success = deleteCommentsRecursively(comment.getCommentId());
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除评论失败");
        }

        // 更新目标内容的评论数
        updateCommentCount(comment.getTargetId(), comment.getTargetType(), -deletedCommentCount);

        return true;
    }

    /**
     * 统计评论的所有子评论数量（不包含自身）
     */
    private int countCommentsRecursively(Long commentId) {
        // 一次性查询所有子评论
        Set<Long> allChildCommentIds = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(commentId);

        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            QueryWrapper<Comments> childQuery = new QueryWrapper<>();
            childQuery.eq("parentCommentId", parentId)
                    .eq("isDelete", 0);
            List<Comments> childComments = this.list(childQuery);
            List<Long> childIds = childComments.stream()
                    .map(Comments::getCommentId)
                    .collect(Collectors.toList());
            allChildCommentIds.addAll(childIds);
            queue.addAll(childIds); // 继续查询子评论的子评论
        }

        return allChildCommentIds.size();
    }

    /**
     * 批量删除评论及其所有子评论
     */
    private boolean deleteCommentsRecursively(Long commentId) {
        // 一次性查询所有待删除的评论ID
        Set<Long> commentIdsToDelete = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(commentId);

        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            commentIdsToDelete.add(parentId);

            // 批量查询当前层级的所有子评论
            QueryWrapper<Comments> childQuery = new QueryWrapper<>();
            childQuery.eq("parentCommentId", parentId)
                    .eq("isDelete", 0);
            List<Comments> childComments = this.list(childQuery);

            // 将子评论ID加入集合和队列，继续向下查询
            List<Long> childIds = childComments.stream()
                    .map(Comments::getCommentId)
                    .collect(Collectors.toList());
            commentIdsToDelete.addAll(childIds);
            queue.addAll(childIds);
        }

        // 批量逻辑删除
        if (!commentIdsToDelete.isEmpty()) {
            return this.update(new LambdaUpdateWrapper<Comments>()
                    .set(Comments::getIsDelete, 1)
                    .in(Comments::getCommentId, commentIdsToDelete));
        }
        return true;
    }

    @Override
    public Page<CommentsVO> queryComment(CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        if (currentUser == null) {
            return new Page<>(commentsQueryRequest.getCurrent(), commentsQueryRequest.getPageSize(), 0);
        }

        long current = commentsQueryRequest.getCurrent();
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(commentsQueryRequest.getTargetId() == null, ErrorCode.PARAMS_ERROR, "目标id不能为空");

        // 1. 分页查询顶级评论（parentId=0）
        QueryWrapper<Comments> topQuery = new QueryWrapper<>();
        topQuery.eq("targetId", commentsQueryRequest.getTargetId())
                .eq("targetType", Optional.ofNullable(commentsQueryRequest.getTargetType()).orElse(1))
                .eq("parentCommentId", 0)
                .eq("isDelete", 0)
                .orderByDesc("createTime");
        Page<Comments> topCommentsPage = this.page(new Page<>(current, size), topQuery);

        if (topCommentsPage.getRecords().isEmpty()) {
            return new PageDTO<>(current, size, topCommentsPage.getTotal());
        }

        // 2. 收集所有顶级评论ID，查询其所有层级的子评论
        List<Long> topCommentIds = topCommentsPage.getRecords().stream()
                .map(Comments::getCommentId)
                .collect(Collectors.toList());

        // 递归查询所有子评论（所有层级）
        Set<Long> allChildCommentIds = new HashSet<>();
        Queue<Long> queue = new LinkedList<>(topCommentIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            QueryWrapper<Comments> childQuery = new QueryWrapper<>();
            childQuery.eq("parentCommentId", parentId)
                    .eq("targetId", commentsQueryRequest.getTargetId())
                    .eq("targetType", Optional.ofNullable(commentsQueryRequest.getTargetType()).orElse(1))
                    .eq("isDelete", 0);
            List<Comments> childComments = this.list(childQuery);
            List<Long> childIds = childComments.stream()
                    .map(Comments::getCommentId)
                    .collect(Collectors.toList());
            allChildCommentIds.addAll(childIds);
            queue.addAll(childIds); // 继续查询子评论的子评论
        }

        // 3. 查询所有子评论详情
        List<Comments> allChildComments = new ArrayList<>();
        if (!allChildCommentIds.isEmpty()) {
            allChildComments = this.listByIds(allChildCommentIds);
        }

        // 4. 构建父子映射（parentId -> 子评论列表）
        Map<Long, List<Comments>> parentToChildrenMap = allChildComments.stream()
                .collect(Collectors.groupingBy(Comments::getParentCommentId));

        // 5. 批量查询所有用户信息（顶级评论 + 所有子评论）
        Set<Long> allUserIds = new HashSet<>();
        topCommentsPage.getRecords().forEach(c -> allUserIds.add(c.getUserId()));
        allChildComments.forEach(c -> allUserIds.add(c.getUserId()));
        Map<Long, User> userMap = userService.listByIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (k1, k2) -> k1));

        // 6. 构建评论VO（包含多级子评论）
        List<CommentsVO> commentsVOList = topCommentsPage.getRecords().stream()
                .map(topComment -> buildCommentVOWithChildren(topComment, parentToChildrenMap, userMap))
                .collect(Collectors.toList());

        // 7. 封装分页结果
        Page<CommentsVO> resultPage = new Page<>(current, size, topCommentsPage.getTotal());
        resultPage.setRecords(commentsVOList);
        return resultPage;
    }

    /**
     * 递归构建评论VO（包含所有层级的子评论）
     */
    private CommentsVO buildCommentVOWithChildren(Comments comment, Map<Long, List<Comments>> parentToChildrenMap, Map<Long, User> userMap) {
        CommentsVO vo = new CommentsVO();
        BeanUtils.copyProperties(comment, vo);

        // 设置评论用户信息
        User commentUser = userMap.get(comment.getUserId());
        if (commentUser != null) {
            vo.setCommentUser(CommentUserVO.objToVo(commentUser));
        }

        // 递归构建子评论
        List<Comments> childComments = parentToChildrenMap.getOrDefault(comment.getCommentId(), Collections.emptyList());
        if (!childComments.isEmpty()) {
            List<CommentsVO> childVOs = childComments.stream()
                    .sorted(Comparator.comparing(Comments::getCreateTime).reversed()) // 子评论按时间倒序
                    .map(child -> buildCommentVOWithChildren(child, parentToChildrenMap, userMap))
                    .collect(Collectors.toList());
            vo.setChildren(childVOs);
        }
        return vo;
    }

    @Override
    public Boolean likeComment(CommentsLikeRequest commentsLikeRequest, HttpServletRequest request) {
        // 1. 校验登录
        User user = userService.getLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 校验参数
        ThrowUtils.throwIf(commentsLikeRequest.getCommentId() == null, ErrorCode.PARAMS_ERROR, "评论id不能为空");
        Comments comment = this.getById(commentsLikeRequest.getCommentId());
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

        // 3. 构建安全的更新条件（避免SQL注入）
        LambdaUpdateWrapper<Comments> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Comments::getCommentId, commentsLikeRequest.getCommentId());

        // 4. 处理点赞/点踩（使用参数绑定，避免SQL注入）
        Long likeCount = commentsLikeRequest.getLikeCount();
        Long dislikeCount = commentsLikeRequest.getDislikeCount();

        // 参数校验，防止恶意输入
        if (likeCount != null && likeCount != 0) {
            // 限制点赞/点踩变化值范围，防止溢出
            if (Math.abs(likeCount) > 10) {
                likeCount = likeCount > 0 ? 1L : -1L; // 限制每次只能变化1
            }
            int safeLikeCount = Math.toIntExact(Math.max(-1, Math.min(1, likeCount)));
            updateWrapper.setSql("likeCount = likeCount + " + safeLikeCount);
        }
        if (dislikeCount != null && dislikeCount != 0) {
            // 限制点踩变化值范围，防止溢出
            if (Math.abs(dislikeCount) > 10) {
                dislikeCount = dislikeCount > 0 ? 1L : -1L; // 限制每次只能变化1
            }
            int safeDislikeCount = Math.toIntExact(Math.max(-1, Math.min(1, dislikeCount)));
            updateWrapper.setSql("dislikeCount = dislikeCount + " + safeDislikeCount);
        }

        // 5. 执行更新
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CommentsVO> getAndClearUnreadComments(Long userId) {
        // 1. 查询未读评论
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isDelete", 0)
                .ne("userId", userId)
                .orderByDesc("createTime");
        List<Comments> unreadComments = this.list(queryWrapper);
        if (CollUtil.isEmpty(unreadComments)) {
            return Collections.emptyList();
        }

        // 2. 批量查询关联数据
        // 2.1 评论用户信息
        Set<Long> commentUserIds = unreadComments.stream()
                .map(Comments::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> commentUserMap = userService.listByIds(commentUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 2.2 目标内容（图片/帖子）信息
        Set<Long> targetIds = unreadComments.stream()
                .map(Comments::getTargetId)
                .collect(Collectors.toSet());
        Map<Long, Picture> pictureMap = pictureService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p));
        Map<Long, Post> postMap = postService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 2.3 目标内容作者信息
        Set<Long> targetUserIds = new HashSet<>();
        pictureMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        postMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        Map<Long, User> targetUserMap = userService.listByIds(targetUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 收集所有未读评论ID，用于后续批量处理
        List<Long> unreadCommentIds = unreadComments.stream()
                .map(Comments::getCommentId)
                .collect(Collectors.toList());

        // 4. 构建返回VO（包含多级子评论）
        List<CommentsVO> result = unreadComments.stream().map(comment -> {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(comment, vo);

            // 设置评论用户信息
            User commentUser = commentUserMap.get(comment.getUserId());
            if (commentUser != null) {
                vo.setCommentUser(CommentUserVO.objToVo(commentUser));
            }

            // 设置目标内容信息
            if (comment.getTargetType() == 1) {
                Picture picture = pictureMap.get(comment.getTargetId());
                if (picture != null) {
                    vo.setPicture(PictureVO.objToVo(picture));
                    User pictureUser = targetUserMap.get(picture.getUserId());
                    if (pictureUser != null) {
                        vo.getPicture().setUser(userService.getUserVO(pictureUser));
                    }
                }
            } else if (comment.getTargetType() == 2) {
                Post post = postMap.get(comment.getTargetId());
                if (post != null) {
                    User postUser = targetUserMap.get(post.getUserId());
                    if (postUser != null) {
                        post.setUser(userService.getUserVO(postUser));
                    }
                    vo.setPost(post);
                }
            }

            // 构建多级子评论
            vo.setChildren(getAllChildrenCommentsVO(comment.getCommentId(), commentUserMap));
            return vo;
        }).collect(Collectors.toList());

        // 5. 批量标记为已读
        if (!unreadCommentIds.isEmpty()) {
            this.update(new LambdaUpdateWrapper<Comments>()
                    .set(Comments::getIsRead, 1)
                    .in(Comments::getCommentId, unreadCommentIds));
        }

        return result;
    }

    /**
     * 获取评论的所有层级子评论VO
     */
    private List<CommentsVO> getAllChildrenCommentsVO(Long parentCommentId, Map<Long, User> userMap) {
        // 1. 批量查询该根评论下的所有子评论（通过rootCommentId）
        Comments rootComment = this.getById(parentCommentId);
        if (rootComment == null) {
            return Collections.emptyList();
        }

        QueryWrapper<Comments> allChildQuery = new QueryWrapper<>();
        allChildQuery.eq("rootCommentId", rootComment.getRootCommentId())
                .eq("isDelete", 0)
                .ne("commentId", rootComment.getCommentId()); // 排除根评论自身
        List<Comments> allChildComments = this.list(allChildQuery);

        // 2. 构建父子映射
        Map<Long, List<Comments>> parentToChildrenMap = allChildComments.stream()
                .collect(Collectors.groupingBy(Comments::getParentCommentId));

        // 3. 递归构建VO
        return buildChildrenVO(parentCommentId, parentToChildrenMap, userMap);
    }

    /**
     * 递归构建子评论VO
     */
    private List<CommentsVO> buildChildrenVO(Long parentId, Map<Long, List<Comments>> parentToChildrenMap, Map<Long, User> userMap) {
        List<Comments> directChildren = parentToChildrenMap.getOrDefault(parentId, Collections.emptyList());
        return directChildren.stream()
                .map(child -> {
                    CommentsVO vo = new CommentsVO();
                    BeanUtils.copyProperties(child, vo);
                    User childUser = userMap.get(child.getUserId());
                    if (childUser != null) {
                        vo.setCommentUser(CommentUserVO.objToVo(childUser));
                    }
                    // 递归构建深层子评论
                    vo.setChildren(buildChildrenVO(child.getCommentId(), parentToChildrenMap, userMap));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCommentsCount(Long userId) {
        return this.count(new QueryWrapper<Comments>()
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .eq("isDelete", 0)
                .ne("userId", userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadComments(Long userId) {
        this.update(new LambdaUpdateWrapper<Comments>()
                .set(Comments::getIsRead, 1)
                .eq(Comments::getTargetUserId, userId)
                .eq(Comments::getIsRead, 0)
                .eq(Comments::getIsDelete, 0));
    }

    @Override
    public Page<CommentsVO> getMyCommentHistory(CommentsQueryRequest commentsQueryRequest, Long userId) {
        long current = commentsQueryRequest.getCurrent();
        long size = commentsQueryRequest.getPageSize();

        // 构建查询条件
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("isDelete", 0);
        if (commentsQueryRequest.getTargetType() != null) {
            queryWrapper.eq("targetType", commentsQueryRequest.getTargetType());
        }
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<Comments> commentsPage = this.page(new Page<>(current, size), queryWrapper);
        if (commentsPage.getRecords().isEmpty()) {
            Page<CommentsVO> emptyPage = new Page<>(current, size, commentsPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 批量查询关联数据
        List<Comments> commentsList = commentsPage.getRecords();
        Set<Long> targetIds = commentsList.stream()
                .map(Comments::getTargetId)
                .collect(Collectors.toSet());
        Set<Long> userIds = commentsList.stream()
                .map(Comments::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Picture> pictureMap = pictureService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p));
        Map<Long, Post> postMap = postService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 目标内容作者信息
        Set<Long> targetUserIds = new HashSet<>();
        pictureMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        postMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        Map<Long, User> targetUserMap = userService.listByIds(targetUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO
        List<CommentsVO> records = commentsList.stream().map(comment -> {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(comment, vo);
            vo.setCommentUser(CommentUserVO.objToVo(userMap.get(comment.getUserId())));

            if (comment.getTargetType() == 1) {
                Picture picture = pictureMap.get(comment.getTargetId());
                if (picture != null) {
                    vo.setPicture(PictureVO.objToVo(picture));
                    User pictureUser = targetUserMap.get(picture.getUserId());
                    if (pictureUser != null) {
                        vo.getPicture().setUser(userService.getUserVO(pictureUser));
                    }
                }
            } else if (comment.getTargetType() == 2) {
                Post post = postMap.get(comment.getTargetId());
                if (post != null) {
                    User postUser = targetUserMap.get(post.getUserId());
                    if (postUser != null) {
                        post.setUser(userService.getUserVO(postUser));
                    }
                    vo.setPost(post);
                }
            }
            return vo;
        }).collect(Collectors.toList());

        Page<CommentsVO> voPage = new Page<>(current, size, commentsPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Page<CommentsVO> getCommentedHistory(CommentsQueryRequest commentsQueryRequest, Long userId) {
        long current = commentsQueryRequest.getCurrent();
        long size = commentsQueryRequest.getPageSize();

        // 构建查询条件
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isDelete", 0)
                .ne("userId", userId);
        if (commentsQueryRequest.getTargetType() != null) {
            queryWrapper.eq("targetType", commentsQueryRequest.getTargetType());
        }
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<Comments> commentsPage = this.page(new Page<>(current, size), queryWrapper);
        if (commentsPage.getRecords().isEmpty()) {
            Page<CommentsVO> emptyPage = new Page<>(current, size, commentsPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 批量查询关联数据
        List<Comments> commentsList = commentsPage.getRecords();
        Set<Long> targetIds = commentsList.stream()
                .map(Comments::getTargetId)
                .collect(Collectors.toSet());
        Set<Long> commentUserIds = commentsList.stream()
                .map(Comments::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> commentUserMap = userService.listByIds(commentUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Picture> pictureMap = pictureService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p));
        Map<Long, Post> postMap = postService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 目标内容作者信息
        Set<Long> targetUserIds = new HashSet<>();
        pictureMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        postMap.values().forEach(p -> targetUserIds.add(p.getUserId()));
        Map<Long, User> targetUserMap = userService.listByIds(targetUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO
        List<CommentsVO> records = commentsList.stream().map(comment -> {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(comment, vo);
            vo.setCommentUser(CommentUserVO.objToVo(commentUserMap.get(comment.getUserId())));

            if (comment.getTargetType() == 1) {
                Picture picture = pictureMap.get(comment.getTargetId());
                if (picture != null) {
                    vo.setPicture(PictureVO.objToVo(picture));
                    User pictureUser = targetUserMap.get(picture.getUserId());
                    if (pictureUser != null) {
                        vo.getPicture().setUser(userService.getUserVO(pictureUser));
                    }
                }
            } else if (comment.getTargetType() == 2) {
                Post post = postMap.get(comment.getTargetId());
                if (post != null) {
                    User postUser = targetUserMap.get(post.getUserId());
                    if (postUser != null) {
                        post.setUser(userService.getUserVO(postUser));
                    }
                    vo.setPost(post);
                }
            }
            return vo;
        }).collect(Collectors.toList());

        Page<CommentsVO> voPage = new Page<>(current, size, commentsPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public QueryWrapper<Comments> getQueryWrapper(CommentsQueryRequest commentsQueryRequest) {
        if (commentsQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        if (commentsQueryRequest.getCommentId() != null) {
            queryWrapper.eq("commentId", commentsQueryRequest.getCommentId());
        }
        if (commentsQueryRequest.getUserId() != null) {
            queryWrapper.eq("userId", commentsQueryRequest.getUserId());
        }
        if (commentsQueryRequest.getTargetId() != null) {
            queryWrapper.eq("targetId", commentsQueryRequest.getTargetId());
        }
        if (commentsQueryRequest.getTargetType() != null) {
            queryWrapper.eq("targetType", commentsQueryRequest.getTargetType());
        }
        if (StringUtils.isNotBlank(commentsQueryRequest.getContent())) {
            queryWrapper.like("content", commentsQueryRequest.getContent());
        }

        // 排序
        if (StringUtils.isNotBlank(commentsQueryRequest.getSortField())) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equals(commentsQueryRequest.getSortOrder());
            queryWrapper.orderBy(true, isAsc, commentsQueryRequest.getSortField());
        } else {
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationComments(CommentsBatchRequest commentsBatchRequest) {
        List<Long> ids = commentsBatchRequest.getIds();
        String operation = commentsBatchRequest.getOperation();
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "评论id列表为空");

        // 查询所有评论
        List<Comments> commentsList = this.listByIds(ids);
        ThrowUtils.throwIf(commentsList.size() != ids.size(), ErrorCode.NOT_FOUND_ERROR, "部分评论不存在");

        // 统计所有需要操作的评论（包含子评论）
        Set<Long> allCommentIds = new HashSet<>();
        Map<Long, Comments> commentMap = commentsList.stream()
                .collect(Collectors.toMap(Comments::getCommentId, c -> c));

        // 递归收集所有子评论ID
        for (Long commentId : ids) {
            allCommentIds.add(commentId);
            Queue<Long> queue = new LinkedList<>();
            queue.offer(commentId);
            while (!queue.isEmpty()) {
                Long currentId = queue.poll();
                QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("parentCommentId", currentId)
                        .eq("isDelete", 0);
                List<Comments> childComments = this.list(queryWrapper);
                childComments.forEach(child -> {
                    allCommentIds.add(child.getCommentId());
                    queue.offer(child.getCommentId());
                });
            }
        }

        // 执行批量操作
        boolean result = false;
        int delta = 0;
        LambdaUpdateWrapper<Comments> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Comments::getCommentId, allCommentIds);

        switch (operation) {
            case "delete":
                updateWrapper.set(Comments::getIsDelete, 1);
                delta = -1;
                break;
            case "restore":
                updateWrapper.set(Comments::getIsDelete, 0);
                delta = 1;
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的操作");
        }
        result = this.update(updateWrapper);

        // 批量更新评论数
        if (result && delta != 0) {
            // 按目标类型+目标ID分组
            Map<String, Set<Long>> targetCommentMap = new HashMap<>();
            for (Long commentId : allCommentIds) {
                Comments c = commentMap.get(commentId);
                if (c == null) c = this.getById(commentId);
                if (c != null) {
                    Integer targetType = c.getTargetType();
                    Long targetId = c.getTargetId();
                    if (targetType != null && targetId != null) {
                        String key = targetType + "_" + targetId;
                        targetCommentMap.computeIfAbsent(key, k -> new HashSet<>()).add(commentId);
                    }
                }
            }

            // 批量更新评论数
            for (Map.Entry<String, Set<Long>> entry : targetCommentMap.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Integer targetType = Integer.valueOf(parts[0]);
                Long targetId = Long.valueOf(parts[1]);
                int totalCount = entry.getValue().size();
                updateCommentCount(targetId, targetType, delta * totalCount);
            }
        }

        return result;
    }

    @Override
    public QueryWrapper<Comments> getAdminQueryWrapper(CommentsAdminRequest commentsAdminRequest) {
        if (commentsAdminRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        if (commentsAdminRequest.getCommentId() != null) {
            queryWrapper.eq("commentId", commentsAdminRequest.getCommentId());
        }
        if (commentsAdminRequest.getUserId() != null) {
            queryWrapper.eq("userId", commentsAdminRequest.getUserId());
        }
        if (commentsAdminRequest.getTargetId() != null) {
            queryWrapper.eq("targetId", commentsAdminRequest.getTargetId());
        }
        if (commentsAdminRequest.getTargetType() != null) {
            queryWrapper.eq("targetType", commentsAdminRequest.getTargetType());
        }
        if (StringUtils.isNotBlank(commentsAdminRequest.getContent())) {
            queryWrapper.like("content", commentsAdminRequest.getContent());
        }
        if (commentsAdminRequest.getIsDelete() != null) {
            queryWrapper.eq("isDelete", commentsAdminRequest.getIsDelete());
        }

        // 排序
        if (StringUtils.isNotBlank(commentsAdminRequest.getSortField())) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equals(commentsAdminRequest.getSortOrder());
            queryWrapper.orderBy(true, isAsc, commentsAdminRequest.getSortField());
        } else {
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

    @Override
    public Page<CommentsVO> getAllCommentsByUserId(Long userId, long current, long pageSize) {
        // 构建查询条件
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        // 分页查询
        Page<Comments> commentsPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<Comments> commentsList = commentsPage.getRecords();
        if (commentsList.isEmpty()) {
            Page<CommentsVO> emptyPage = new Page<>(current, pageSize, commentsPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 批量查询关联数据
        Set<Long> userIds = commentsList.stream()
                .map(Comments::getUserId)
                .collect(Collectors.toSet());
        Set<Long> targetIds = commentsList.stream()
                .map(Comments::getTargetId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Picture> pictureMap = pictureService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p));
        Map<Long, Post> postMap = postService.listByIds(targetIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 转换为VO
        List<CommentsVO> commentsVOList = commentsList.stream()
                .map(comment -> convertToVO(comment, userMap, pictureMap, postMap))
                .collect(Collectors.toList());

        // 封装分页结果
        Page<CommentsVO> commentsVOPage = new Page<>(current, pageSize, commentsPage.getTotal());
        commentsVOPage.setRecords(commentsVOList);
        return commentsVOPage;
    }

    @Override
    public boolean markCommentAsRead(Long id) {
        return this.update(new LambdaUpdateWrapper<Comments>()
                .set(Comments::getIsRead, 1)
                .eq(Comments::getCommentId, id)
                .eq(Comments::getIsRead, 0)
                .eq(Comments::getIsDelete, 0));
    }

    /**
     * 转换为VO对象
     */
    private CommentsVO convertToVO(Comments comments, Map<Long, User> userMap, Map<Long, Picture> pictureMap, Map<Long, Post> postMap) {
        if (comments == null) {
            return null;
        }

        CommentsVO vo = new CommentsVO();
        BeanUtils.copyProperties(comments, vo);

        // 安全获取用户信息
        User user = userMap.get(comments.getUserId());
        if (user != null) {
            vo.setCommentUser(CommentUserVO.objToVo(user));
        }

        if (comments.getTargetType() == 1) {
            Picture picture = pictureMap.get(comments.getTargetId());
            if (picture != null) {
                vo.setPicture(PictureVO.objToVo(picture));
            }
        } else if (comments.getTargetType() == 2) {
            Post post = postMap.get(comments.getTargetId());
            if (post != null) {
                vo.setPost(post);
            }
        }

        return vo;
    }
}
