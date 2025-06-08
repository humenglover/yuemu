package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.esdao.EsPostDao;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CounterManager;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.mapper.PostMapper;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostAttachmentRequest;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostAddRequest;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsPost;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private PostAttachmentService postAttachmentService;

    @Resource
    private UserService userService;

    @Resource
    private UserfollowsService userfollowsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EsPostDao esPostDao;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    @Lazy
    private ShareRecordService shareRecordService;

    @Resource
    private CrawlerManager crawlerManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPost(PostAddRequest postAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(postAddRequest == null, ErrorCode.PARAMS_ERROR);

        String title = postAddRequest.getTitle();
        String content = postAddRequest.getContent();
        List<PostAttachmentRequest> attachments = postAddRequest.getAttachments();

        // 标题校验
        ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(title.length() > 100, ErrorCode.PARAMS_ERROR, "标题最多100字");

        // 内容校验
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");

        // 处理内容中的图片标记
        if (CollUtil.isNotEmpty(attachments)) {
            for (int i = 0; i < attachments.size(); i++) {
                PostAttachmentRequest attach = attachments.get(i);
                if (attach.getType() == 1) { // 图片类型
                    String marker = "{img-" + (i + 1) + "}";
                    // 确保 marker 在内容中存在
                    ThrowUtils.throwIf(!content.contains(marker),
                            ErrorCode.PARAMS_ERROR, "图片标记 " + marker + " 未在内容中找到");
                }
            }
        }

        // 创建帖子
        Post post = new Post();
        BeanUtils.copyProperties(postAddRequest, post);
        post.setUserId(loginUser.getId());
        post.setStatus(0); // 待审核

        // 保存帖子
        boolean success = this.save(post);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 同步到 ES
        try {
            EsPost esPost = new EsPost();
            BeanUtils.copyProperties(post, esPost);
            esPostDao.save(esPost);
        } catch (Exception e) {
            log.error("Failed to sync post to ES during creation, postId: {}", post.getId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }

        // 处理附件时，对于图片类型，使用缩略图URL
        if (CollUtil.isNotEmpty(attachments)) {
            List<PostAttachment> postAttachments = attachments.stream()
                    .map(attach -> {
                        PostAttachment attachment = new PostAttachment();
                        BeanUtils.copyProperties(attach, attachment);
                        attachment.setPostId(post.getId());
                        if (attach.getType() == 1) { // 图片类型
                            String marker = "{img-" + (attachments.indexOf(attach) + 1) + "}";
                            attachment.setMarker(marker);
                            attachment.setPosition(content.indexOf(marker));
                            // 将原始URL转换为缩略图URL
                            String thumbnailUrl = attach.getUrl().replace("/public/", "/thumbnail/");
                            attachment.setUrl(thumbnailUrl);
                        }
                        return attachment;
                    }).collect(Collectors.toList());
            postAttachmentService.saveBatch(postAttachments);
        }

        return post.getId();
    }

    /**
     * 检测爬虫或恶意请求
     */
    private void crawlerDetect(HttpServletRequest request) {
        crawlerManager.detectNormalRequest(request);
    }

    @Override
    public Post getPostDetail(Long id, User loginUser, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 检测爬虫
        crawlerDetect(request);

        // 获取帖子信息
        Post post = this.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);

        // 增加浏览量
        incrementViewCount(id, request);

        // 获取附件并按位置排序
        List<PostAttachment> attachments = postAttachmentService.list(new QueryWrapper<PostAttachment>()
                .eq("postId", id)
                .orderByAsc("position"));

        // 替换内容中的图片标记为缩略图URL
        String content = post.getContent();
        for (PostAttachment attachment : attachments) {
            if (attachment.getType() == 1 && StrUtil.isNotBlank(attachment.getMarker())) {
                content = content.replace(attachment.getMarker(),
                        String.format("![%s](%s)", attachment.getName(), attachment.getUrl()));
            }
        }
        post.setContent(content);
        post.setAttachments(attachments);

        // 填充用户信息
        User user = userService.getById(post.getUserId());
        post.setUser(userService.getUserVO(user));

        // 设置点赞和分享状态
        if (loginUser != null) {
            boolean isLiked = likeRecordService.isContentLiked(post.getId(), 2, loginUser.getId());
            post.setIsLiked(isLiked ? 1 : 0);
            boolean isShared = shareRecordService.isContentShared(post.getId(), 2, loginUser.getId());
            post.setIsShared(isShared ? 1 : 0);
        } else {
            post.setIsLiked(0);
            post.setIsShared(0);
        }

        // 获取最新的浏览量
        long realViewCount = getViewCount(id);
        post.setViewCount(realViewCount);

        return post;
    }

    @Override
    public Page<Post> listPosts(PostQueryRequest postQueryRequest, User loginUser) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();

        // 构建查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();

        // 搜索词
        String searchText = postQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("title", searchText).or().like("content", searchText);
        }

        // 分类
        String category = postQueryRequest.getCategory();
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }

        // 用户ID
        Long userId = postQueryRequest.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }

        // 处理查询范围
        boolean isPublic = postQueryRequest.getIsPublic();
        boolean isAdmin = loginUser != null && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

        if (isPublic || !isAdmin) {
            // 公共查询或非管理员，只显示已发布的帖子
            queryWrapper.eq("status", 1);
        } else {
            // 管理员查询所有状态的帖子
            Integer status = postQueryRequest.getStatus();
            if (status != null) {
                queryWrapper.eq("status", status);
            }
        }

        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");

        // 执行查询
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);

        // 填充帖子信息
        postPage.getRecords().forEach(this::fillPostInfo);

        return postPage;
    }

    /**
     * 批量填充帖子信息
     */
    private void fillPostsInfo(List<Post> posts, User loginUser) {
        if (CollUtil.isEmpty(posts)) {
            return;
        }

        // 获取所有帖子ID
        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());

        // 批量查询所有图片
        Map<Long, List<PostAttachment>> postAttachmentMap = getPostAttachments(postIds);

        // 批量查询用户信息
        Map<Long, User> userMap = getUserMap(posts);

        // 获取登录用户的点赞和分享信息
        Map<Long, Boolean> likeMap = new HashMap<>();
        Map<Long, Boolean> shareMap = new HashMap<>();
        if (loginUser != null) {
            likeMap = getPostIdIsLikedMap(loginUser, postIds);
            shareMap = getPostIdIsSharedMap(loginUser, postIds);
        }

        // 批量获取浏览量
        Map<Long, Long> viewCountMap = new HashMap<>();
        List<String> viewCountKeys = postIds.stream()
                .map(postId -> String.format("post:viewCount:%d", postId))
                .collect(Collectors.toList());
        if (!viewCountKeys.isEmpty()) {
            List<String> redisViewCounts = stringRedisTemplate.opsForValue().multiGet(viewCountKeys);
            int i = 0;
            for (Long postId : postIds) {
                String redisCount = redisViewCounts.get(i++);
                Post post = this.getById(postId);
                long baseCount = post != null && post.getViewCount() != null ? post.getViewCount() : 0L;
                long increment = redisCount != null ? Long.parseLong(redisCount) : 0L;
                viewCountMap.put(postId, baseCount + increment);
            }
        }

        // 填充信息
        for (Post post : posts) {
            // 清空内容，只在详情页显示
            post.setContent(null);
            // 设置第一张图片
            List<PostAttachment> attachments = postAttachmentMap.get(post.getId());
            post.setAttachments(attachments != null ? attachments : Collections.emptyList());
            // 设置用户信息
            User user = userMap.get(post.getUserId());
            if (user != null) {
                post.setUser(userService.getUserVO(user));
            }
            // 设置点赞和分享状态
            post.setIsLiked(likeMap.getOrDefault(post.getId(), false) ? 1 : 0);
            post.setIsShared(shareMap.getOrDefault(post.getId(), false) ? 1 : 0);
            // 设置实时浏览量
            post.setViewCount(viewCountMap.getOrDefault(post.getId(), 0L));
        }
    }

    /**
     * 填充用户点赞状态
     */
    private void fillUserLikeStatus(List<Post> posts, Long userId) {
        if (CollUtil.isEmpty(posts) || userId == null) {
            return;
        }
        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Map<Long, Boolean> likeMap = getPostIdIsLikedMap(userService.getById(userId), postIds);
        posts.forEach(post -> post.setIsLiked(likeMap.getOrDefault(post.getId(), false) ? 1 : 0));
    }

    /**
     * 获取帖子的点赞状态映射
     */
    private Map<Long, Boolean> getPostIdIsLikedMap(User currentUser, Set<Long> postIds) {
        // 使用通用点赞表查询
        QueryWrapper<LikeRecord> likeQueryWrapper = new QueryWrapper<>();
        likeQueryWrapper.in("targetId", postIds)
                .eq("userId", currentUser.getId())
                .eq("targetType", 2)  // 2表示帖子类型
                .eq("isLiked", true);

        List<LikeRecord> likeRecords = likeRecordService.list(likeQueryWrapper);

        return likeRecords.stream()
                .collect(Collectors.toMap(
                        LikeRecord::getTargetId,
                        like -> true,
                        (b1, b2) -> b1
                ));
    }

    /**
     * 获取帖子的分享状态映射
     */
    private Map<Long, Boolean> getPostIdIsSharedMap(User currentUser, Set<Long> postIds) {
        // 查询分享记录
        QueryWrapper<ShareRecord> shareQueryWrapper = new QueryWrapper<>();
        shareQueryWrapper.in("targetId", postIds)
                .eq("userId", currentUser.getId())
                .eq("targetType", 2)  // 2表示帖子类型
                .eq("isShared", true);

        List<ShareRecord> shareRecords = shareRecordService.list(shareQueryWrapper);

        return shareRecords.stream()
                .collect(Collectors.toMap(
                        ShareRecord::getTargetId,
                        share -> true,
                        (b1, b2) -> b1
                ));
    }

    /**
     * 获取帖子的附件信息
     */
    private Map<Long, List<PostAttachment>> getPostAttachments(Set<Long> postIds) {
        List<PostAttachment> allAttachments = postAttachmentService.list(
                new QueryWrapper<PostAttachment>()
                        .in("postId", postIds)
                        .eq("type", 1)  // 只查询图片类型
                        .orderByAsc("position")
        );

        Map<Long, List<PostAttachment>> postAttachmentMap = new HashMap<>();
        if (CollUtil.isNotEmpty(allAttachments)) {
            for (PostAttachment attachment : allAttachments) {
                postAttachmentMap.computeIfAbsent(attachment.getPostId(), k -> new ArrayList<>())
                        .add(attachment);
            }
            // 只保留每个帖子的第一张图片
            postAttachmentMap.forEach((postId, attachments) -> {
                if (attachments.size() > 1) {
                    attachments.subList(1, attachments.size()).clear();
                }
            });
        }
        return postAttachmentMap;
    }

    /**
     * 获取用户信息映射
     */
    private Map<Long, User> getUserMap(List<Post> posts) {
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPost(Long postId, Integer status, String message, User loginUser) {
        // 参数校验
        Post post = this.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 校验权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新审核状态
        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setStatus(status);
        updatePost.setReviewMessage(message);

        boolean success = this.updateById(updatePost);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 同步更新 ES 数据
        try {
            Optional<EsPost> esOptional = esPostDao.findById(postId);
            EsPost esPost;
            if (esOptional.isPresent()) {
                esPost = esOptional.get();
                esPost.setStatus(status);
                esPost.setReviewMessage(message);
            } else {
                post = this.getById(postId);
                esPost = new EsPost();
                BeanUtils.copyProperties(post, esPost);
            }
            esPostDao.save(esPost);
        } catch (Exception e) {
            log.error("Failed to sync post review status to ES, postId: {}", postId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }
    }

    @Override
    public Page<Post> listMyPosts(PostQueryRequest request) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();

        // 必须是当前用户的帖子
        ThrowUtils.throwIf(request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        queryWrapper.eq("userId", request.getUserId());

        // 构建查询条件
        if (StrUtil.isNotBlank(request.getCategory())) {
            queryWrapper.eq("category", request.getCategory());
        }

        // 处理审核状态查询
        if (request.getStatus() != null) {
            queryWrapper.eq("status", request.getStatus());
        }

        // 搜索标题和内容
        if (StrUtil.isNotBlank(request.getSearchText())) {
            queryWrapper.and(wrap -> wrap
                    .like("title", request.getSearchText())
                    .or()
                    .like("content", request.getSearchText())
            );
        }

        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<Post> postPage = this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);

        // 填充用户信息和首张图片
        List<Post> records = postPage.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            // 获取所有帖子ID
            Set<Long> postIds = records.stream().map(Post::getId).collect(Collectors.toSet());

            // 批量查询所有图片并按位置排序
            List<PostAttachment> allAttachments = postAttachmentService.list(
                    new QueryWrapper<PostAttachment>()
                            .in("postId", postIds)
                            .eq("type", 1)  // 只查询图片类型
                            .orderByAsc("position")
            );

            // 在内存中取每个帖子的第一张图片
            Map<Long, List<PostAttachment>> postAttachmentMap = new HashMap<>();
            if (CollUtil.isNotEmpty(allAttachments)) {
                for (PostAttachment attachment : allAttachments) {
                    postAttachmentMap.computeIfAbsent(attachment.getPostId(), k -> new ArrayList<>())
                            .add(attachment);
                }
                // 只保留每个帖子的第一张图片
                postAttachmentMap.forEach((postId, attachments) -> {
                    if (attachments.size() > 1) {
                        attachments.subList(1, attachments.size()).clear();
                    }
                });
            }

            // 获取用户信息
            User user = userService.getById(request.getUserId());

            // 填充信息
            records.forEach(post -> {
                // 清空内容，只在详情页显示
                post.setContent(null);
                // 只设置第一张图片
                List<PostAttachment> attachments = postAttachmentMap.get(post.getId());
                post.setAttachments(attachments != null ? attachments : Collections.emptyList());
                // 设置用户信息
                if (user != null) {
                    post.setUser(userService.getUserVO(user));
                }
                // 设置点赞状态为0（因为是自己的帖子，不需要显示点赞状态）
                post.setIsLiked(0);
            });
        }

        return postPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePost(Post post) {
        // 参数校验
        ThrowUtils.throwIf(post == null || post.getId() == null, ErrorCode.PARAMS_ERROR);

        // 获取原帖子信息
        Post oldPost = this.getById(post.getId());
        ThrowUtils.throwIf(oldPost == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

        // 保持不变的字段
        post.setUserId(oldPost.getUserId());
        post.setCreateTime(oldPost.getCreateTime());
        post.setLikeCount(oldPost.getLikeCount());
        post.setCommentCount(oldPost.getCommentCount());
        post.setViewCount(oldPost.getViewCount());

        // 更新基本信息
        post.setStatus(0);  // 更新后需要重新审核
        post.setUpdateTime(new Date());

        // 处理内容中的图片标记
        String content = post.getContent();
        List<PostAttachment> attachments = post.getAttachments();
        if (CollUtil.isNotEmpty(attachments)) {
            for (int i = 0; i < attachments.size(); i++) {
                PostAttachment attach = attachments.get(i);
                if (attach.getType() == 1) { // 图片类型
                    String marker = "{img-" + (i + 1) + "}";
                    // 确保 marker 在内容中存在
                    ThrowUtils.throwIf(!content.contains(marker),
                            ErrorCode.PARAMS_ERROR, "图片标记 " + marker + " 未在内容中找到");
                }
            }
        }

        // 开始更新操作
        boolean success = this.updateById(post);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "帖子更新失败");

        // 同步更新 ES 数据
        try {
            Optional<EsPost> esOptional = esPostDao.findById(post.getId());
            EsPost esPost;
            if (esOptional.isPresent()) {
                esPost = esOptional.get();
                // 使用新的变量名避免冲突
                Post updatedPost = this.getById(post.getId());
                BeanUtils.copyProperties(updatedPost, esPost);
            } else {
                esPost = new EsPost();
                BeanUtils.copyProperties(post, esPost);
            }
            esPostDao.save(esPost);
        } catch (Exception e) {
            log.error("Failed to sync post to ES during update, postId: {}", post.getId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }

        // 更新附件时，对于图片类型，使用缩略图URL
        if (attachments != null) {
            // 1. 删除原有附件
            postAttachmentService.remove(new QueryWrapper<PostAttachment>()
                    .eq("postId", post.getId()));

            // 2. 保存新附件
            if (!attachments.isEmpty()) {
                attachments.forEach(attach -> {
                    attach.setPostId(post.getId());
                    if (attach.getType() == 1) { // 图片类型
                        String marker = "{img-" + (attachments.indexOf(attach) + 1) + "}";
                        attach.setMarker(marker);
                        attach.setPosition(content.indexOf(marker));
                        // 将原始URL转换为缩略图URL
                        String thumbnailUrl = attach.getUrl().replace("/public/", "/thumbnail/");
                        attach.setUrl(thumbnailUrl);
                    }
                });
                postAttachmentService.saveBatch(attachments);
            }
        }

        return true;
    }

    @Override
    public Page<Post> getFollowPosts(HttpServletRequest request, PostQueryRequest postQueryRequest) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 获取当前用户关注的用户ID列表
        QueryWrapper<Userfollows> followsQueryWrapper = new QueryWrapper<>();
        followsQueryWrapper.eq("followerId", loginUser.getId())
                .eq("followStatus", 1);
        List<Userfollows> userFollowsList = userfollowsService.list(followsQueryWrapper);

        if (CollUtil.isEmpty(userFollowsList)) {
            return new Page<>();
        }

        // 提取关注用户的ID
        List<Long> followingIds = userFollowsList.stream()
                .map(Userfollows::getFollowingId)
                .collect(Collectors.toList());

        // 构建查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("userId", followingIds)
                .eq("status", 1)  // 只查询已发布的帖子
                .eq("isDelete", 0);

        // 添加搜索条件
        if (StrUtil.isNotBlank(postQueryRequest.getSearchText())) {
            queryWrapper.and(qw -> qw.like("title", postQueryRequest.getSearchText())
                    .or()
                    .like("content", postQueryRequest.getSearchText()));
        }

        // 添加分类条件
        if (StrUtil.isNotBlank(postQueryRequest.getCategory())) {
            queryWrapper.eq("category", postQueryRequest.getCategory());
        }

        queryWrapper.orderByDesc("createTime");

        // 执行分页查询
        Page<Post> postPage = this.page(
                new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize()),
                queryWrapper
        );

        // 填充帖子信息
        postPage.getRecords().forEach(this::fillPostInfo);

        return postPage;
    }

    /**
     * 获取帖子榜单
     */
    @Override
    public List<Post> getTop100Post(Long type) {
        return getTop100Post(type.longValue(), null);
    }

    /**
     * 获取帖子榜单（带请求检测）
     */
    private List<Post> getTop100Post(long type, HttpServletRequest request) {
        // 如果有请求对象，进行爬虫检测
        if (request != null) {
            crawlerDetect(request);
        }

        // 构建查询条件
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .eq("status", 1);  // 只查询已审核通过的帖子

        // 根据类型设置时间范围
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);

        switch ((int) type) {
            case 1: // 日榜
                calendar.add(Calendar.DATE, -1);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 2: // 周榜
                calendar.add(Calendar.DATE, -7);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 3: // 月榜
                calendar.add(Calendar.MONTH, -1);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 4: // 总榜
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 按照浏览量、点赞数、评论数排序
        queryWrapper.orderByDesc("viewCount", "likeCount", "commentCount");

        // 限制返回100条
        queryWrapper.last("LIMIT 100");

        List<Post> posts = list(queryWrapper);

        // 填充帖子信息
        posts.forEach(this::fillPostInfo);

        return posts;
    }

    @Override
    public void fillPostInfo(Post post) {
        // 填充用户信息
        User user = userService.getById(post.getUserId());
        if (user != null) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            post.setUser(userVO);
        }

        // 获取帖子附件（只保留第一张图片）
        QueryWrapper<PostAttachment> attachmentQueryWrapper = new QueryWrapper<>();
        attachmentQueryWrapper.eq("postId", post.getId())
                .eq("type", 1)  // 只获取图片类型的附件
                .orderByAsc("position")
                .last("LIMIT 1");
        List<PostAttachment> attachments = postAttachmentService.list(attachmentQueryWrapper);
        post.setAttachments(attachments);

        // 获取实时浏览量（合并 Redis 中的增量）
        long realViewCount = getViewCount(post.getId());
        post.setViewCount(realViewCount);

        // 清空内容，只在详情页显示
        post.setContent(null);
    }

    /**
     * 获取帖子浏览量（包括 Redis 增量）
     */
    @Override
    public long getViewCount(Long postId) {
        // 先从 Redis 获取增量
        String viewCountKey = String.format("post:viewCount:%d", postId);
        String incrementCount = stringRedisTemplate.opsForValue().get(viewCountKey);

        // 从数据库获取基础浏览量
        Post post = this.getById(postId);
        if (post == null) {
            return 0L;
        }

        // 合并数据库和 Redis 的浏览量
        long baseCount = post.getViewCount() != null ? post.getViewCount() : 0L;
        long increment = incrementCount != null ? Long.parseLong(incrementCount) : 0L;

        return baseCount + increment;
    }

    /**
     * 异步增加帖子浏览量
     */
    @Async("asyncExecutor")
    public void incrementViewCount(Long postId, HttpServletRequest request) {
        // 检查是否需要增加浏览量
        if (!crawlerManager.detectViewRequest(request, postId)) {
            return;
        }

        // 使用 Redis 进行计数
        String viewCountKey = String.format("post:viewCount:%d", postId);
        String lockKey = String.format("post:viewCount:lock:%d", postId);

        try {
            // 获取分布式锁
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 增加浏览量
                stringRedisTemplate.opsForValue().increment(viewCountKey);

                // 当浏览量达到一定阈值时，更新数据库
                String viewCountStr = stringRedisTemplate.opsForValue().get(viewCountKey);
                if (viewCountStr != null && Long.parseLong(viewCountStr) % 100 == 0) {  // 改为100，和图片保持一致
                    this.update()
                            .setSql("viewCount = viewCount + " + viewCountStr)
                            .eq("id", postId)
                            .update();
                    // 更新ES
                    updateEsPostViewCount(postId, Long.parseLong(viewCountStr));
                    // 更新后重置 Redis 计数
                    stringRedisTemplate.delete(viewCountKey);
                }
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 更新 ES 中帖子的浏览量
     */
    private void updateEsPostViewCount(Long postId, Long viewCount) {
        try {
            esPostDao.findById(postId).ifPresent(esPost -> {
                esPost.setViewCount(esPost.getViewCount() + viewCount);
                esPostDao.save(esPost);
            });
        } catch (Exception e) {
            log.error("Failed to update ES post view count, postId: {}", postId, e);
        }
    }
}
