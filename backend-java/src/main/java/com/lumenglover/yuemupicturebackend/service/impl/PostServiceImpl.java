package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.mapper.PostMapper;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.vo.PostTagCategory;
import com.lumenglover.yuemupicturebackend.model.vo.PostVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.utils.SystemNotifyUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {


    @Resource
    private UserService userService;

    @Resource
    private UserFollowsService userfollowsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    @Lazy
    private ShareRecordService shareRecordService;

    @Resource
    private CrawlerManager crawlerManager;

    @Resource
    private SystemNotifyUtil systemNotifyUtil;

    @Resource
    private SensitiveUtil sensitiveUtil;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private ViewRecordService viewRecordService;

    @Resource
    @Lazy
    private FavoriteRecordService favoriteRecordService;

    @Resource
    private TagService tagService;

    @Resource
    private CategoryService categoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPost(PostAddRequest postAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(postAddRequest == null, ErrorCode.PARAMS_ERROR);

        String title = postAddRequest.getTitle();
        String content = postAddRequest.getContent();

        // 如果请求中包含ID，则执行更新操作（用于发布草稿）
        if (postAddRequest.getId() != null) {
            // 获取原帖子信息
            Post oldPost = this.getById(postAddRequest.getId());
            ThrowUtils.throwIf(oldPost == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");

            // 校验权限 - 只有本人或管理员可以更新
            if (!oldPost.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 对帖子内容进行敏感词过滤
            String filteredTitle = SensitiveUtil.filter(title);
            String filteredContent = SensitiveUtil.filter(content);

            // 检查是否包含敏感词
            boolean hasSensitiveWords = !Objects.equals(title, filteredTitle) ||
                    !Objects.equals(content, filteredContent);

            // 更新帖子信息
            Post updatePost = new Post();
            updatePost.setId(postAddRequest.getId());
            updatePost.setTitle(postAddRequest.getTitle());
            updatePost.setContent(postAddRequest.getContent());
            updatePost.setCategory(postAddRequest.getCategory());

            // 设置封面图，直接使用前端传递的封面图URL
            updatePost.setCoverUrl(postAddRequest.getCoverUrl());

            // 设置标签，直接使用前端传递的标签
            if (postAddRequest.getTags() != null) {
                updatePost.setTags(cn.hutool.json.JSONUtil.toJsonStr(postAddRequest.getTags()));
            } else {
                updatePost.setTags(null); // 如果前端传空值，则清空标签
            }

            // 设置删除状态为未删除
            updatePost.setIsDelete(0); // 0表示未删除，1表示已删除

            // 发布草稿，将草稿状态设为非草稿
            updatePost.setIsDraft(0); // 0表示非草稿，发布后的帖子不再是草稿

            if (hasSensitiveWords) {
                // 包含敏感词，设置为待审核状态
                updatePost.setStatus(0); // 待审核
                log.warn("更新的帖子包含敏感词，需要人工审核，标题: {}", title);
            } else {
                // 不包含敏感词，直接通过审核
                updatePost.setStatus(1); // 审核通过
                log.info("更新的帖子通过敏感词过滤，标题: {}", title);
            }

            // 更新帖子
            boolean success = this.updateById(updatePost);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

            // 在帖子更新后，根据审核状态发送相应通知
            if (hasSensitiveWords) {
                // 包含敏感词，发送邮件给管理员审核
                try {
                    String adminEmail = cosClientConfig.getAdminEmail();
                    if (adminEmail != null && !adminEmail.isEmpty()) {
                        String emailContent = buildSensitiveWordNotificationContentForUpdate(updatePost, filteredTitle, filteredContent);
                        emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
                        log.info("更新帖子敏感词审核通知邮件已发送给管理员: {}", adminEmail);
                    } else {
                        log.warn("未配置管理员邮箱，无法发送更新帖子敏感词审核通知");
                    }
                } catch (Exception e) {
                    log.error("发送更新帖子敏感词审核通知邮件失败: {}", e.getMessage());
                }
            } else {
                // 不包含敏感词，直接审核通过，发送系统通知
                SystemNotifyUtil.sendPostApprovedNotify(loginUser.getId(), updatePost.getId(), updatePost.getTitle());
                log.info("更新帖子直接通过审核，已发送系统通知，帖子ID: {}", updatePost.getId());
            }



            return updatePost.getId();
        }

        // 标题校验
        ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(title.length() > 100, ErrorCode.PARAMS_ERROR, "标题最多100字");

        // 内容校验
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");

        // 对帖子内容进行敏感词过滤
        String filteredTitle = SensitiveUtil.filter(title);
        String filteredContent = SensitiveUtil.filter(content);

        // 检查是否包含敏感词
        boolean hasSensitiveWords = !Objects.equals(title, filteredTitle) ||
                !Objects.equals(content, filteredContent);

        // 创建帖子
        Post post = new Post();
        post.setTitle(postAddRequest.getTitle());
        post.setContent(postAddRequest.getContent());
        post.setCategory(postAddRequest.getCategory());
        post.setUserId(loginUser.getId());

        // 设置封面图，直接使用前端传递的封面图URL
        String coverUrl = postAddRequest.getCoverUrl();
        log.info("准备设置封面图，URL: {}", coverUrl);
        post.setCoverUrl(coverUrl);
        log.info("已设置封面图，当前值: {}", post.getCoverUrl());

        // 设置标签，直接使用前端传递的标签
        if (postAddRequest.getTags() != null) {
            post.setTags(cn.hutool.json.JSONUtil.toJsonStr(postAddRequest.getTags()));
        } else {
            post.setTags(null); // 如果前端传空值，则清空标签
        }

        // 设置删除状态为未删除
        post.setIsDelete(0); // 0表示未删除，1表示已删除

        // 设置草稿状态为非草稿（因为这是添加正式帖子）
        post.setIsDraft(0); // 0表示非草稿，1表示草稿

        if (hasSensitiveWords) {
            // 包含敏感词，设置为待审核状态
            post.setStatus(0); // 待审核
            log.warn("帖子包含敏感词，需要人工审核，标题: {}", title);
        } else {
            // 不包含敏感词，直接通过审核
            post.setStatus(1); // 审核通过
            log.info("帖子通过敏感词过滤，标题: {}", title);
        }

        // 记录即将保存的帖子信息
        log.info("即将保存的帖子信息 - ID: {}, 标题: {}, 封面图: {}, 标签: {}",
                post.getId(), post.getTitle(), post.getCoverUrl(), post.getTags());

        // 保存帖子
        boolean success = this.save(post);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 验证数据库中的实际值
        Post savedPost = this.getById(post.getId());
        if (savedPost != null) {
            log.info("帖子保存后从数据库读取 - ID: {}, 封面图: {}, 标题: {}",
                    savedPost.getId(), savedPost.getCoverUrl(), savedPost.getTitle());
        } else {
            log.warn("保存的帖子未能立即从数据库读取 - ID: {}", post.getId());
        }

        // 在帖子保存后，根据审核状态发送相应通知
        if (hasSensitiveWords) {
            // 包含敏感词，发送邮件给管理员审核
            try {
                String adminEmail = cosClientConfig.getAdminEmail();
                if (adminEmail != null && !adminEmail.isEmpty()) {
                    String emailContent = buildSensitiveWordNotificationContent(post, loginUser, filteredTitle, filteredContent);
                    emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
                    log.info("敏感词审核通知邮件已发送给管理员: {}", adminEmail);
                } else {
                    log.warn("未配置管理员邮箱，无法发送敏感词审核通知");
                }
            } catch (Exception e) {
                log.error("发送敏感词审核通知邮件失败: {}", e.getMessage());
            }
        } else {
            // 不包含敏感词，直接审核通过，发送系统通知
            SystemNotifyUtil.sendPostApprovedNotify(loginUser.getId(), post.getId(), post.getTitle());
            log.info("帖子直接通过审核，已发送系统通知，帖子ID: {}", post.getId());
        }



        // 重新查询以确保获取完整的帖子信息，包括标签
        log.info("帖子创建成功，ID: {}，标签: {}", post.getId(), post.getTags());

        return post.getId();
    }

    @Override
    public Map<String, Object> getPostStatusStats() {
        // 统计帖子状态信息
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0); // 未删除的帖子

        // 总帖子数
        long total = this.count(queryWrapper);

        // 已审核通过的帖子数
        long approved = this.count(queryWrapper.clone().eq("status", 1));

        // 待审核的帖子数
        long pending = this.count(queryWrapper.clone().eq("status", 0));

        // 审核不通过的帖子数
        long rejected = this.count(queryWrapper.clone().eq("status", 2));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("approved", approved);
        stats.put("pending", pending);
        stats.put("rejected", rejected);

        return stats;
    }

    /**
     * 构建敏感词通知邮件内容
     */
    private String buildSensitiveWordNotificationContent(Post post, User loginUser, String filteredTitle, String filteredContent) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>帖子敏感词审核通知</h2>");
        content.append("<p><strong>帖子ID:</strong> ").append(post.getId()).append("</p>");
        content.append("<p><strong>上传用户ID:</strong> ").append(loginUser.getId()).append("</p>");
        content.append("<p><strong>上传用户名:</strong> ").append(loginUser.getUserName()).append("</p>");
        content.append("<p><strong>上传用户邮箱:</strong> ").append(loginUser.getEmail()).append("</p>");
        content.append("<p><strong>帖子标题:</strong> ").append(post.getTitle() != null ? post.getTitle() : "无").append("</p>");
        content.append("<p><strong>帖子内容:</strong> ").append(post.getContent() != null ? post.getContent() : "无").append("</p>");
        content.append("<p><strong>过滤后标题:</strong> ").append(filteredTitle).append("</p>");
        content.append("<p><strong>过滤后内容:</strong> ").append(filteredContent).append("</p>");
        content.append("<p><strong>上传时间:</strong> ").append(post.getCreateTime()).append("</p>");
        content.append("<p>请登录系统进行人工审核。</p>");

        return content.toString();
    }

    /**
     * 构建更新帖子敏感词通知邮件内容
     */
    private String buildSensitiveWordNotificationContentForUpdate(Post post, String filteredTitle, String filteredContent) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>更新帖子敏感词审核通知</h2>");
        content.append("<p><strong>帖子ID:</strong> ").append(post.getId()).append("</p>");
        content.append("<p><strong>帖子标题:</strong> ").append(post.getTitle() != null ? post.getTitle() : "无").append("</p>");
        content.append("<p><strong>帖子内容:</strong> ").append(post.getContent() != null ? post.getContent() : "无").append("</p>");
        content.append("<p><strong>过滤后标题:</strong> ").append(filteredTitle).append("</p>");
        content.append("<p><strong>过滤后内容:</strong> ").append(filteredContent).append("</p>");
        content.append("<p><strong>更新时间:</strong> ").append(post.getUpdateTime()).append("</p>");
        content.append("<p>请登录系统进行人工审核。</p>");

        return content.toString();
    }

    /**
     * 检测爬虫或恶意请求
     */
    private void crawlerDetect(HttpServletRequest request) {
        crawlerManager.detectNormalRequest(request);
    }

    @Override
    public PostVO getPostDetail(Long id, User loginUser, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 检测爬虫
        crawlerDetect(request);

        // 【修正点1】先获取Post实体，再转VO
        Post postEntity = this.getById(id);
        ThrowUtils.throwIf(postEntity == null, ErrorCode.NOT_FOUND_ERROR);
        PostVO post = PostVO.objToVo(postEntity);

        // 增加浏览量
        incrementViewCount(id, request);

        String content = post.getContent();
        post.setContent(content);



        // 填充用户信息
        User user = userService.getById(post.getUserId());
        post.setUser(userService.getUserVO(user));

        // 设置点赞和分享状态
        if (loginUser != null) {
            boolean isLiked = likeRecordService.isContentLiked(post.getId(), 2, loginUser.getId());
            post.setIsLiked(isLiked ? 1 : 0);
            boolean isShared = shareRecordService.isContentShared(post.getId(), 2, loginUser.getId());
            post.setIsShared(isShared ? 1 : 0);

            // 获取收藏状态
            boolean isFavorited = favoriteRecordService.hasFavorited(loginUser.getId(), post.getId(), 2); // 2表示帖子类型
            post.setIsFavorited(isFavorited ? 1 : 0);

            // 自动添加浏览记录
            try {
                addPostViewRecord(post.getId(), loginUser.getId(), request);
            } catch (Exception e) {
                log.error("添加帖子浏览记录失败", e);
            }
        } else {
            post.setIsLiked(0);
            post.setIsShared(0);
            post.setIsFavorited(0);
        }

        // 获取最新的浏览量
        long realViewCount = getViewCount(id);
        post.setViewCount(realViewCount);

        return post;
    }

    @Override
    public Page<PostVO> listPosts(PostQueryRequest postQueryRequest, User loginUser) {
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
        queryWrapper.eq("isDraft", 0); // 只查询非草稿的帖子
        queryWrapper.orderByDesc("createTime");

        // 执行查询
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);

        // 转换为VO并填充信息
        Page<PostVO> postVOPage = new Page<>();
        BeanUtils.copyProperties(postPage, postVOPage);
        // 【修正点2】正确转换Post实体到PostVO
        List<PostVO> postVOList = postPage.getRecords().stream().map(PostVO::objToVo).collect(Collectors.toList());

        // 填充帖子信息
        fillPostsInfo(postVOList, loginUser);

        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    /**
     * 批量填充帖子信息
     */
    private void fillPostsInfo(List<PostVO> posts, User loginUser) {
        if (CollUtil.isEmpty(posts)) {
            return;
        }

        // 获取所有帖子ID
        Set<Long> postIds = posts.stream().map(PostVO::getId).collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = getUserMapFromVO(posts);

        // 获取登录用户的点赞、分享和收藏信息
        Map<Long, Boolean> likeMap = new HashMap<>();
        Map<Long, Boolean> shareMap = new HashMap<>();
        Map<Long, Boolean> favoriteMap = new HashMap<>();
        if (loginUser != null) {
            likeMap = getPostIdIsLikedMap(loginUser, postIds);
            shareMap = getPostIdIsSharedMap(loginUser, postIds);
            favoriteMap = getPostIdIsFavoritedMap(loginUser, postIds);
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
        for (PostVO post : posts) {
            // 清空内容，只在详情页显示
            post.setContent(null);
            // 设置用户信息
            User user = userMap.get(post.getUserId());
            if (user != null) {
                post.setUser(userService.getUserVO(user));
            }
            // 设置点赞、分享和收藏状态
            post.setIsLiked(likeMap.getOrDefault(post.getId(), false) ? 1 : 0);
            post.setIsShared(shareMap.getOrDefault(post.getId(), false) ? 1 : 0);
            post.setIsFavorited(favoriteMap.getOrDefault(post.getId(), false) ? 1 : 0);
            // 设置实时浏览量
            post.setViewCount(viewCountMap.getOrDefault(post.getId(), 0L));
        }
    }

    /**
     * 从VO列表获取用户信息映射
     */
    private Map<Long, User> getUserMapFromVO(List<PostVO> posts) {
        Set<Long> userIds = posts.stream().map(PostVO::getUserId).collect(Collectors.toSet());
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    /**
     * 填充用户点赞状态
     */
    private void fillUserLikeStatus(List<PostVO> posts, Long userId) {
        if (CollUtil.isEmpty(posts) || userId == null) {
            return;
        }
        Set<Long> postIds = posts.stream().map(PostVO::getId).collect(Collectors.toSet());
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
     * 获取帖子的收藏状态映射
     */
    private Map<Long, Boolean> getPostIdIsFavoritedMap(User currentUser, Set<Long> postIds) {
        // 使用通用收藏表查询
        QueryWrapper<FavoriteRecord> favoriteQueryWrapper = new QueryWrapper<>();
        favoriteQueryWrapper.in("targetId", postIds)
                .eq("userId", currentUser.getId())
                .eq("targetType", 2)  // 2表示帖子类型
                .eq("isFavorite", true);

        List<FavoriteRecord> favoriteRecords = favoriteRecordService.list(favoriteQueryWrapper);

        return favoriteRecords.stream()
                .collect(Collectors.toMap(
                        FavoriteRecord::getTargetId,
                        favorite -> true,
                        (b1, b2) -> b1
                ));
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

        // 校验帖子是否为草稿，草稿不能审核
        if (post.getIsDraft() != null && post.getIsDraft() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "草稿不能进行审核");
        }

        // 更新审核状态
        Post updatePost = new Post();
        updatePost.setId(postId);

        // 管理员审核具有最高优先级，直接采用管理员的审核决定
        updatePost.setStatus(status);
        updatePost.setReviewMessage(message);

        boolean success = this.updateById(updatePost);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 重新获取post对象，确保获取到最新的信息
        post = this.getById(postId);

        // 发送系统通知
        if (status == 1) {
            // 审核通过通知
            SystemNotifyUtil.sendPostApprovedNotify(post.getUserId(), postId, post.getTitle());
        } else if (status == 2) {
            // 审核不通过通知
            SystemNotifyUtil.sendPostRejectedNotify(post.getUserId(), postId, post.getTitle(), message);
        }
    }

    @Override
    public Page<PostVO> listMyPosts(PostQueryRequest request) {
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

        // 转换为VO并填充信息
        Page<PostVO> postVOPage = new Page<>();
        BeanUtils.copyProperties(postPage, postVOPage);
        // 【修正点3】正确转换Post实体到PostVO
        List<PostVO> postVOList = postPage.getRecords().stream().map(PostVO::objToVo).collect(Collectors.toList());

        // 填充用户信息
        if (CollUtil.isNotEmpty(postVOList)) {
            // 获取用户信息
            User user = userService.getById(request.getUserId());

            // 填充信息
            postVOList.forEach(post -> {
                // 清空内容，只在详情页显示
                post.setContent(null);
                // 设置用户信息
                if (user != null) {
                    post.setUser(userService.getUserVO(user));
                }
                // 设置点赞状态为0（因为是自己的帖子，不需要显示点赞状态）
                post.setIsLiked(0);
                // 设置收藏状态为0（因为是自己的帖子，不需要显示收藏状态）
                post.setIsFavorited(0);
            });
        }

        postVOPage.setRecords(postVOList);
        return postVOPage;
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
        post.setIsDelete(oldPost.getIsDelete()); // 保持删除状态不变
        // 注意：这里不保留旧的标签，因为标签可能已被更新

        // 对帖子内容进行敏感词过滤
        String filteredTitle = SensitiveUtil.filter(post.getTitle());
        String filteredContent = SensitiveUtil.filter(post.getContent());

        // 检查是否包含敏感词
        boolean hasSensitiveWords = !Objects.equals(post.getTitle(), filteredTitle) ||
                !Objects.equals(post.getContent(), filteredContent);

        if (hasSensitiveWords) {
            // 包含敏感词，设置为待审核状态
            post.setStatus(0); // 待审核
            log.warn("更新的帖子包含敏感词，需要人工审核，标题: {}", post.getTitle());
        } else {
            // 不包含敏感词，直接通过审核
            post.setStatus(1); // 审核通过
            log.info("更新的帖子通过敏感词过滤，标题: {}", post.getTitle());
        }
        post.setUpdateTime(new Date());
        log.info("更新帖子时的封面图: {}", post.getCoverUrl());

        // 在帖子保存后，如果包含敏感词，发送邮件通知管理员
        boolean shouldSendEmail = hasSensitiveWords; // 保存是否需要发送邮件的状态

        String content = post.getContent();

        // 开始更新操作
        boolean success = this.updateById(post);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "帖子更新失败");

        // 在帖子更新后，根据审核状态发送相应通知
        if (hasSensitiveWords) {
            // 包含敏感词，发送邮件给管理员审核
            try {
                String adminEmail = cosClientConfig.getAdminEmail();
                if (adminEmail != null && !adminEmail.isEmpty()) {
                    String emailContent = buildSensitiveWordNotificationContentForUpdate(post, filteredTitle, filteredContent);
                    emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
                    log.info("更新帖子敏感词审核通知邮件已发送给管理员: {}", adminEmail);
                } else {
                    log.warn("未配置管理员邮箱，无法发送更新帖子敏感词审核通知");
                }
            } catch (Exception e) {
                log.error("发送更新帖子敏感词审核通知邮件失败: {}", e.getMessage());
            }
        } else {
            // 不包含敏感词，直接审核通过，发送系统通知
            SystemNotifyUtil.sendPostApprovedNotify(post.getUserId(), post.getId(), post.getTitle());
            log.info("更新帖子直接通过审核，已发送系统通知，帖子ID: {}", post.getId());
        }



        return true;
    }

    @Override
    public Page<PostVO> getFollowPosts(HttpServletRequest request, PostQueryRequest postQueryRequest) {
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

        // 转换为VO并填充信息
        Page<PostVO> postVOPage = new Page<>();
        BeanUtils.copyProperties(postPage, postVOPage);
        // 【修正点4】正确转换Post实体到PostVO
        List<PostVO> postVOList = postPage.getRecords().stream().map(PostVO::objToVo).collect(Collectors.toList());

        fillPostsInfo(postVOList, loginUser);

        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    /**
     * 获取帖子榜单
     */
    @Override
    public List<PostVO> getTop100Post(Long type) {
        return getTop100Post(type.longValue(), null);
    }

    /**
     * 获取帖子榜单（带请求检测）
     */
    private List<PostVO> getTop100Post(long type, HttpServletRequest request) {
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

        // 转换为VO并填充信息
        // 【修正点5】正确转换Post实体到PostVO
        List<PostVO> postVOList = posts.stream().map(PostVO::objToVo).collect(Collectors.toList());
        fillPostsInfo(postVOList, null);

        return postVOList;
    }

    @Override
    public void fillPostInfo(PostVO postVO) {
        // 填充用户信息
        User user = userService.getById(postVO.getUserId());
        if (user != null) {
            postVO.setUser(userService.getUserVO(user));
        }

        // 获取实时浏览量（合并 Redis 中的增量）
        long realViewCount = getViewCount(postVO.getId());
        postVO.setViewCount(realViewCount);

        // 清空内容，只在详情页显示
        postVO.setContent(null);
    }

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

                    // 更新后重置 Redis 计数
                    stringRedisTemplate.delete(viewCountKey);
                }
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public void addPostViewRecord(long postId, long userId, HttpServletRequest request) {
        try {
            ViewRecordAddRequest viewRecordAddRequest = new ViewRecordAddRequest();
            viewRecordAddRequest.setUserId(userId);
            viewRecordAddRequest.setTargetId(postId);
            viewRecordAddRequest.setTargetType(2); // 2-帖子

            viewRecordService.addViewRecord(viewRecordAddRequest, request);
        } catch (Exception e) {
            log.error("添加帖子浏览记录失败", e);
        }
    }

    @Override
    public PostTagCategory listPostTagCategory(User loginUser) {
        String cacheKey = "post:tag_category:list";

        // 如果用户已登录，使用用户私有缓存
        if (loginUser != null) {
            cacheKey = "post:tag_category:list:" + loginUser.getId();
        }

        // 尝试从缓存获取
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                // 反序列化缓存数据
                PostTagCategory cachedData = cn.hutool.json.JSONUtil.toBean(cachedJson, PostTagCategory.class);
                return cachedData;
            } catch (Exception e) {
                log.warn("获取帖子标签分类缓存失败: {}", e.getMessage());
            }
        }

        // 缓存未命中，查询数据库默认全局分类和标签
        List<String> tagList = tagService.listTag();
        // 获取帖子分类（类型为1的分类）
        List<String> categoryList = categoryService.listCategoryByType(1);

        // 如果用户已登录，进行个性化排序
        if (loginUser != null) {
            try {
                Map<String, Integer> tagWeightMap = new HashMap<>();
                Map<String, Integer> categoryWeightMap = new HashMap<>();
                Long userId = loginUser.getId();
                log.info("[帖子个性化推荐] 开始计算用户 {} 的分类权重", userId);

                // 获取近期交互记录，包含目标类型 2（帖子）

                // 1. 获取近期收藏
                List<FavoriteRecord> favorites = favoriteRecordService.list(new QueryWrapper<FavoriteRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 2)
                        .orderByDesc("createTime")
                        .last("limit 20"));
                log.info("[帖子个性化推荐] 用户 {} 近期收藏帖子 {} 条", userId, favorites.size());

                // 2. 获取近期点赞
                List<LikeRecord> likes = likeRecordService.list(new QueryWrapper<LikeRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 2)
                        .orderByDesc("lastLikeTime")
                        .last("limit 20"));
                log.info("[帖子个性化推荐] 用户 {} 近期点赞帖子 {} 条", userId, likes.size());

                // 3. 获取近期浏览
                List<ViewRecord> views = viewRecordService.list(new QueryWrapper<ViewRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 2)
                        .orderByDesc("createTime")
                        .last("limit 50"));
                log.info("[帖子个性化推荐] 用户 {} 近期浏览帖子 {} 条", userId, views.size());

                // 统一收集帖子ID
                Set<Long> postIds = new HashSet<>();
                favorites.forEach(f -> postIds.add(f.getTargetId()));
                likes.forEach(l -> postIds.add(l.getTargetId()));
                views.forEach(v -> postIds.add(v.getTargetId()));

                // 批量查询帖子信息
                if (!postIds.isEmpty()) {
                    List<Post> postList = this.listByIds(postIds);
                    Map<Long, Post> postMap = postList.stream().collect(Collectors.toMap(Post::getId, p -> p));

                    // 计算权重 (浏览=1, 点赞=2, 收藏=3)
                    views.forEach(v -> addWeight(postMap.get(v.getTargetId()), tagWeightMap, categoryWeightMap, 1));
                    likes.forEach(l -> addWeight(postMap.get(l.getTargetId()), tagWeightMap, categoryWeightMap, 2));
                    favorites.forEach(f -> addWeight(postMap.get(f.getTargetId()), tagWeightMap, categoryWeightMap, 3));
                    log.info("[帖子个性化推荐] 用户 {} categoryWeightMap={}", userId, categoryWeightMap);

                    // 对默认列表进行降序排序
                    tagList.sort((a, b) -> tagWeightMap.getOrDefault(b, 0) - tagWeightMap.getOrDefault(a, 0));
                    categoryList.sort((a, b) -> categoryWeightMap.getOrDefault(b, 0) - categoryWeightMap.getOrDefault(a, 0));
                    log.info("[帖子个性化推荐] 用户 {} 排序后 categoryList={}", userId, categoryList);
                }
            } catch (Exception e) {
                log.error("计算用户个性化帖子分类推荐失败", e);
            }
        }

        PostTagCategory postTagCategory = new PostTagCategory();
        postTagCategory.setTagList(tagList);
        postTagCategory.setCategoryList(categoryList);

        // 将结果存入缓存 (公共缓存1小时，私有缓存5分钟以提升更新率)
        try {
            long expireTime = loginUser != null ? 300 : 3600;
            stringRedisTemplate.opsForValue().set(cacheKey, cn.hutool.json.JSONUtil.toJsonStr(postTagCategory),
                    expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("设置帖子标签分类缓存失败: {}", e.getMessage());
        }

        return postTagCategory;
    }

    /**
     * 为标签和分类添加交互权重分数
     */
    private void addWeight(Post p, Map<String, Integer> tagWeightMap, Map<String, Integer> categoryWeightMap, int weight) {
        if (p == null) return;

        // 累加分类
        if (StrUtil.isNotBlank(p.getCategory())) {
            categoryWeightMap.put(p.getCategory(), categoryWeightMap.getOrDefault(p.getCategory(), 0) + weight);
        }

        // 累加标签
        if (StrUtil.isNotBlank(p.getTags())) {
            try {
                List<String> tags = cn.hutool.json.JSONUtil.toList(p.getTags(), String.class);
                for (String tag : tags) {
                    tagWeightMap.put(tag, tagWeightMap.getOrDefault(tag, 0) + weight);
                }
            } catch (Exception ignored) {
                // 如果解析失败则尝试使用逗号分隔
                String[] tags = p.getTags().split(",");
                for (String tag : tags) {
                    if (StrUtil.isNotBlank(tag)) {
                        tagWeightMap.put(tag.trim(), tagWeightMap.getOrDefault(tag.trim(), 0) + weight);
                    }
                }
            }
        }
    }

    @Override
    public Long saveDraft(PostAddRequest postAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(postAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 创建帖子草稿
        Post post = new Post();
        post.setTitle(postAddRequest.getTitle());
        post.setContent(postAddRequest.getContent());
        post.setCategory(postAddRequest.getCategory());
        post.setUserId(loginUser.getId());

        // 设置封面图，直接使用前端传递的封面图URL
        String coverUrl = postAddRequest.getCoverUrl();
        log.info("保存草稿时设置封面图，URL: {}", coverUrl);
        post.setCoverUrl(coverUrl);

        // 设置标签，直接使用前端传递的标签
        if (postAddRequest.getTags() != null) {
            post.setTags(cn.hutool.json.JSONUtil.toJsonStr(postAddRequest.getTags()));
        } else {
            post.setTags(null); // 如果前端传空值，则清空标签
        }

        // 设置为草稿状态
        post.setIsDraft(1); // 1表示草稿
        post.setStatus(0); // 草稿状态下状态为0

        log.info("准备保存草稿 - ID: {}, 标题: {}, 封面图: {}", post.getId(), post.getTitle(), post.getCoverUrl());

        // 保存草稿
        boolean success = this.save(post);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 验证草稿保存结果
        Post savedDraft = this.getById(post.getId());
        log.info("草稿保存后从数据库读取 - ID: {}, 封面图: {}, 标题: {}",
                savedDraft.getId(), savedDraft.getCoverUrl(), savedDraft.getTitle());

        return post.getId();
    }

    @Override
    public Long saveOrUpdateDraft(PostAddRequest postAddRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(postAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查是否提供了ID，如果有ID则尝试更新现有草稿
        if (postAddRequest.getId() != null) {
            // 获取原草稿信息
            Post oldDraft = this.getById(postAddRequest.getId());
            ThrowUtils.throwIf(oldDraft == null, ErrorCode.NOT_FOUND_ERROR, "草稿不存在");

            // 校验权限 - 只有本人可以更新自己的草稿
            if (!oldDraft.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 校验是否确实是草稿
            if (oldDraft.getIsDraft() == null || oldDraft.getIsDraft() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该帖子不是草稿，无法更新");
            }

            // 更新草稿信息
            Post updateDraft = new Post();
            updateDraft.setId(postAddRequest.getId());
            updateDraft.setTitle(postAddRequest.getTitle());
            updateDraft.setContent(postAddRequest.getContent());
            updateDraft.setCategory(postAddRequest.getCategory());

            // 设置封面图，直接使用前端传递的封面图URL
            updateDraft.setCoverUrl(postAddRequest.getCoverUrl());

            // 设置标签，直接使用前端传递的标签
            if (postAddRequest.getTags() != null) {
                updateDraft.setTags(cn.hutool.json.JSONUtil.toJsonStr(postAddRequest.getTags()));
            } else {
                updateDraft.setTags(null); // 如果前端传空值，则清空标签
            }

            // 确保仍然是草稿状态
            updateDraft.setIsDraft(1); // 1表示草稿
            updateDraft.setStatus(0); // 草稿状态下状态为0

            // 更新草稿
            boolean success = this.updateById(updateDraft);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

            return updateDraft.getId();
        } else {
            // 没有ID，创建新草稿
            return saveDraft(postAddRequest, loginUser);
        }
    }

    @Override
    public List<PostVO> listDrafts(User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 查询用户的草稿列表
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("isDraft", 1) // 只查询草稿
                .eq("isDelete", 0) // 未删除的
                .orderByDesc("updateTime"); // 按更新时间倒序

        List<Post> posts = this.list(queryWrapper);

        // 转换为 VO
        return posts.stream().map(PostVO::objToVo).collect(Collectors.toList());
    }

    @Override
    public Boolean deleteDraft(Long draftId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(draftId == null, ErrorCode.PARAMS_ERROR);

        // 查询草稿是否存在
        Post draft = this.getById(draftId);
        ThrowUtils.throwIf(draft == null, ErrorCode.PARAMS_ERROR, "草稿不存在");

        // 校验是否为当前用户创建的草稿
        ThrowUtils.throwIf(!draft.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权删除此草稿");

        // 校验是否为草稿状态
        ThrowUtils.throwIf(draft.getIsDraft() == null || draft.getIsDraft() != 1,
                ErrorCode.PARAMS_ERROR, "该帖子不是草稿，不能删除");

        // 逻辑删除草稿
        Post updatePost = new Post();
        updatePost.setId(draftId);
        updatePost.setIsDelete(1); // 逻辑删除

        return this.updateById(updatePost);
    }

    @Override
    public boolean setPostPermission(Long postId, Long userId, Integer allowCollect, Integer allowLike, Integer allowComment, Integer allowShare) {
        // 1. 校验参数
        if (postId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID和用户ID不能为空");
        }

        // 2. 检查帖子是否存在
        Post post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        // 3. 权限校验：只有帖子作者或管理员可以设置权限
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        if (!userService.isAdmin(user) && !post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限设置帖子权限");
        }

        // 4. 校验参数值的有效性
        if (allowCollect != null && !asList(0, 1).contains(allowCollect)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许收藏参数无效");
        }

        if (allowLike != null && !asList(0, 1).contains(allowLike)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许点赞参数无效");
        }

        if (allowComment != null && !asList(0, 1).contains(allowComment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许评论参数无效");
        }

        if (allowShare != null && !asList(0, 1).contains(allowShare)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许分享参数无效");
        }

        // 5. 构建更新对象
        Post updatePost = new Post();
        updatePost.setId(postId);

        if (allowCollect != null) {
            updatePost.setAllowCollect(allowCollect);
        }
        if (allowLike != null) {
            updatePost.setAllowLike(allowLike);
        }
        if (allowComment != null) {
            updatePost.setAllowComment(allowComment);
        }
        if (allowShare != null) {
            updatePost.setAllowShare(allowShare);
        }

        // 6. 更新数据库
        boolean result = this.updateById(updatePost);

        // 7. 清除相关缓存
        if (result) {
            // 清除该帖子的缓存
            String postCacheKey = String.format("post:detail:%d", postId);
            stringRedisTemplate.delete(postCacheKey);

            // 清除可能影响的列表缓存
            String listCacheKeyPattern = "post:list:*";
            Set<String> keys = stringRedisTemplate.keys(listCacheKeyPattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }

        return result;
    }

    @Override
    public boolean checkPostPermission(Post post, String operation) {
        if (post == null) {
            return false;
        }

        // 根据操作类型检查权限
        switch (operation) {
            case "collect":
                return post.getAllowCollect() != null && post.getAllowCollect() == 1;
            case "like":
                return post.getAllowLike() != null && post.getAllowLike() == 1;
            case "comment":
                return post.getAllowComment() != null && post.getAllowComment() == 1;
            case "share":
                return post.getAllowShare() != null && post.getAllowShare() == 1;
            default:
                return false;
        }
    }

    // 辅助方法，用于创建包含指定元素的列表
    private static <T> List<T> asList(T... elements) {
        return Arrays.asList(elements);
    }
}
