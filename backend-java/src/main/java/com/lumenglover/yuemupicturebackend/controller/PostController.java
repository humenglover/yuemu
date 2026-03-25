package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.post.PostPermissionRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.vo.PostTagCategory;
import com.lumenglover.yuemupicturebackend.model.vo.PostVO;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.PostService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.service.LikeRecordService;
import com.lumenglover.yuemupicturebackend.model.dto.like.LikeRequest;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;

@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {

    @Resource
    private UserService userService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CrawlerManager crawlerManager;

    @Resource
    private PostService postService;

    /**
     * 创建帖子
     */
    @PostMapping("/add")
    @RateLimiter(key = "post_add", time = 60, count = 60, message = "帖子发布过于频繁，请稍后再试")
    public BaseResponse<Long> addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        log.info("接收到发布帖子请求，标题: {}, 封面图: {}, 标签: {}", postAddRequest.getTitle(), postAddRequest.getCoverUrl(), postAddRequest.getTags());
        Long postId = postService.addPost(postAddRequest, loginUser);
        log.info("帖子发布完成，ID: {}", postId);
        return ResultUtils.success(postId);
    }

    /**
     * 删除帖子
     */
    @PostMapping("/delete/{id}")
    @RateLimiter(key = "post_delete", time = 60, count = 10, message = "帖子删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deletePost(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        Post post = postService.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!post.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = postService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新帖子
     */
    @PostMapping("/update")
    @RateLimiter(key = "post_update", time = 60, count = 10, message = "帖子更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updatePost(@RequestBody Post post, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        log.info("接收到更新帖子请求，ID: {}, 标题: {}, 封面图: {}", post.getId(), post.getTitle(), post.getCoverUrl());

        // 判断是否存在
        Post oldPost = postService.getById(post.getId());
        ThrowUtils.throwIf(oldPost == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可修改
        if (!oldPost.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = postService.updatePost(post);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取帖子
     */
    @GetMapping("/get/{id}")
    @RateLimiter(key = "post_get", time = 60, count = 30, message = "帖子详情查询过于频繁，请稍后再试")
    public BaseResponse<PostVO> getPostById(@PathVariable Long id, HttpServletRequest request) {
        // 检测爬虫
        crawlerManager.detectNormalRequest(request);

        User loginUser = userService.getLoginUser(request);
        PostVO post = postService.getPostDetail(id, loginUser, request);
        return ResultUtils.success(post);
    }

    /**
     * 分页获取帖子列表
     */
    @PostMapping("/list/page")
    @RateLimiter(key = "post_list", time = 60, count = 30, message = "帖子列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<PostVO>> listPostByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                     HttpServletRequest request) {
        // 用户权限校验
        User loginUser = userService.isLogin(request);
        if (loginUser != null) {
            String userRole = loginUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 限制爬虫
        long size = postQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        Page<PostVO> postPage = postService.listPosts(postQueryRequest, loginUser);
        return ResultUtils.success(postPage);
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like/{id}")
    @RateLimiter(key = "post_like", time = 60, count = 20, message = "帖子点赞过于频繁，请稍后再试")
    public BaseResponse<Boolean> likePost(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检查帖子点赞权限
        Post post = postService.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        boolean hasLikePermission = postService.checkPostPermission(post, "like");
        if (!hasLikePermission) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该帖子不允许点赞");
        }

        // 使用通用点赞服务
        LikeRequest likeRequest = new LikeRequest();
        likeRequest.setTargetId(id);
        likeRequest.setTargetType(2);  // 2表示帖子类型
        likeRequest.setIsLiked(true);  // 自动判断是点赞还是取消

        likeRecordService.doLike(likeRequest, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 审核帖子（仅管理员）
     */
    @PostMapping("/review/{id}")
    @RateLimiter(key = "post_review", time = 60, count = 10, message = "帖子审核过于频繁，请稍后再试")
    public BaseResponse<Boolean> reviewPost(@PathVariable Long id, @RequestParam Integer status,
                                            @RequestParam(required = false) String message, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        postService.reviewPost(id, status, message, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户的所有帖子
     */
    @PostMapping("/my/list")
    @RateLimiter(key = "post_my_list", time = 60, count = 25, message = "我的帖子列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<PostVO>> listMyPosts(@RequestBody PostQueryRequest postQueryRequest,
                                                  HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 设置只查询当前用户的帖子
        postQueryRequest.setUserId(loginUser.getId());
        Page<PostVO> postPage = postService.listMyPosts(postQueryRequest);
        return ResultUtils.success(postPage);
    }

    /**
     * 获取关注用户的帖子列表
     */
    @PostMapping("/follow")
    @RateLimiter(key = "post_follow", time = 60, count = 20, message = "关注帖子列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<PostVO>> getFollowPosts(@RequestBody PostQueryRequest postQueryRequest,
                                                     HttpServletRequest request) {
        // 检测爬虫
        crawlerManager.detectNormalRequest(request);
        return ResultUtils.success(postService.getFollowPosts(request, postQueryRequest));
    }

    /**
     * 获取帖子榜单
     */
    @GetMapping("/top100/{id}")
    @RateLimiter(key = "post_top100", time = 60, count = 15, message = "Top100帖子查询过于频繁，请稍后再试")
    public BaseResponse<List<PostVO>> getTop100Post(@PathVariable Long id, HttpServletRequest request) {
        // 检测爬虫
        crawlerManager.detectNormalRequest(request);

        // 构建 Redis 缓存的 key
        String cacheKey = RedisConstant.TOP_100_POST_REDIS_KEY_PREFIX + id;

        // 先从 Redis 缓存中获取数据
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            List<PostVO> postList = JSONUtil.toList(cachedValue, PostVO.class);
            return ResultUtils.success(postList);
        }

        // 缓存未命中，调用服务层方法获取数据
        List<PostVO> postList = postService.getTop100Post(id);

        // 设置缓存，添加随机过期时间防止缓存雪崩
        int cacheExpireTime = (int) (RedisConstant.TOP_100_POST_REDIS_KEY_EXPIRE_TIME
                + RandomUtil.randomInt(0, 6000));
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(postList),
                cacheExpireTime, TimeUnit.SECONDS);

        return ResultUtils.success(postList);
    }

    @GetMapping("/tag_category")
    @RateLimiter(key = "post_tag_category", time = 60, count = 120, message = "标签分类查询过于频繁，请稍后再试")
    public BaseResponse<PostTagCategory> listPostTagCategory(HttpServletRequest request) {
        User loginUser = userService.isLogin(request);
        PostTagCategory postTagCategory = postService.listPostTagCategory(loginUser);
        return ResultUtils.success(postTagCategory);
    }

    /**
     * 保存帖子草稿
     */
    @PostMapping("/draft/save")
    @RateLimiter(key = "post_draft_save", time = 60, count = 5, message = "草稿保存过于频繁，请稍后再试")
    public BaseResponse<Long> savePostDraft(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long draftId = postService.saveOrUpdateDraft(postAddRequest, loginUser);
        return ResultUtils.success(draftId);
    }

    /**
     * 获取用户的草稿列表
     */
    @GetMapping("/draft/list")
    @RateLimiter(key = "post_draft_list", time = 60, count = 20, message = "草稿列表查询过于频繁，请稍后再试")
    public BaseResponse<List<PostVO>> listPostDrafts(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PostVO> drafts = postService.listDrafts(loginUser);
        return ResultUtils.success(drafts);
    }

    /**
     * 获取用户的最新一条草稿
     */
    @GetMapping("/draft/latest")
    @RateLimiter(key = "post_draft_latest", time = 60, count = 20, message = "最新草稿查询过于频繁，请稍后再试")
    public BaseResponse<PostVO> getPostLatestDraft(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PostVO> drafts = postService.listDrafts(loginUser);
        if (drafts.isEmpty()) {
            return ResultUtils.success(null);
        }
        // 返回第一个（最新的一条，因为我们按更新时间倒序排列）
        return ResultUtils.success(drafts.get(0));
    }

    /**
     * 删除草稿
     */
    @DeleteMapping("/draft/{draftId}")
    @RateLimiter(key = "post_draft_delete", time = 60, count = 10, message = "草稿删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deletePostDraft(@PathVariable Long draftId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = postService.deleteDraft(draftId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 设置帖子权限
     *
     * @param permissionRequest 权限设置请求参数
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/permission/set")
    public BaseResponse<Boolean> setPostPermission(@RequestBody PostPermissionRequest permissionRequest, HttpServletRequest request) {
        // 校验参数
        if (permissionRequest == null || permissionRequest.getPostId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 调用服务层设置权限
        boolean result = postService.setPostPermission(
                permissionRequest.getPostId(),
                loginUser.getId(),
                permissionRequest.getAllowCollect(),
                permissionRequest.getAllowLike(),
                permissionRequest.getAllowComment(),
                permissionRequest.getAllowShare()
        );

        return ResultUtils.success(result);
    }

    /**
     * 检查帖子权限
     *
     * @param postId 帖子ID
     * @param operation 操作类型（collect, like, comment, share）
     * @param request HTTP请求
     * @return 权限检查结果
     */
    @GetMapping("/permission/check")
    public BaseResponse<Boolean> checkPostPermission(@RequestParam Long postId, @RequestParam String operation, HttpServletRequest request) {
        // 获取帖子信息
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);

        // 调用服务层检查权限
        boolean hasPermission = postService.checkPostPermission(post, operation);

        return ResultUtils.success(hasPermission);
    }
}
