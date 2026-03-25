package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.friendlink.FriendLinkQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.FriendLink;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.FriendLinkService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 友情链接接口
 */
@RestController
@RequestMapping("/friend-link")
public class FriendLinkController {

    @Resource
    private FriendLinkService friendLinkService;

    @Resource
    private UserService userService;

    /**
     * 创建友链
     *
     * @param friendLink 友链信息
     * @param request   HTTP请求
     * @return 新创建的友链id
     */
    @PostMapping("/add")
    @RateLimiter(key = "friend_link_add", time = 60, count = 5, message = "友链添加过于频繁，请稍后再试")
    public BaseResponse<Long> addFriendLink(@RequestBody FriendLink friendLink, HttpServletRequest request) {
        if (friendLink == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long result = friendLinkService.addFriendLink(friendLink, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 删除友链
     *
     * @param id      友链id
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/delete/{id}")
    @RateLimiter(key = "friend_link_delete", time = 60, count = 10, message = "友链删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteFriendLink(@PathVariable("id") long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = friendLinkService.deleteFriendLink(id, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新友链
     *
     * @param friendLink 友链信息
     * @param request   HTTP请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @RateLimiter(key = "friend_link_update", time = 60, count = 10, message = "友链更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateFriendLink(@RequestBody FriendLink friendLink, HttpServletRequest request) {
        if (friendLink == null || friendLink.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = friendLinkService.updateFriendLink(friendLink, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取友链
     *
     * @param id 友链id
     * @return 友链信息
     */
    @GetMapping("/get/{id}")
    @RateLimiter(key = "friend_link_get", time = 60, count = 30, message = "友链详情查询过于频繁，请稍后再试")
    public BaseResponse<FriendLink> getFriendLinkById(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        FriendLink friendLink = friendLinkService.getFriendLinkById(id);
        return ResultUtils.success(friendLink);
    }

    /**
     * 分页获取友链列表
     * 支持按状态、类型、网站名称搜索，支持排序
     * 非管理员只能看到已审核通过的友链
     *
     * @param friendLinkQueryRequest 查询条件
     * @param request HTTP请求
     * @return 友链分页数据
     */
    @GetMapping("/list/page")
    @RateLimiter(key = "friend_link_list", time = 60, count = 25, message = "友链列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<FriendLink>> listFriendLinksByPage(
            @ModelAttribute FriendLinkQueryRequest friendLinkQueryRequest,
            HttpServletRequest request) {
        if (friendLinkQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<FriendLink> page = friendLinkService.listFriendLinksByPage(friendLinkQueryRequest, request);
        return ResultUtils.success(page);
    }

    /**
     * 审核友链（管理员使用）
     *
     * @param id            友链id
     * @param status        审核状态
     * @param reviewMessage 审核信息
     * @param request       HTTP请求
     * @return 是否成功
     */
    @PostMapping("/review/{id}")
    @RateLimiter(key = "friend_link_review", time = 60, count = 10, message = "友链审核过于频繁，请稍后再试")
    public BaseResponse<Boolean> reviewFriendLink(
            @PathVariable("id") long id,
            @RequestParam("status") int status,
            @RequestParam(value = "reviewMessage", required = false) String reviewMessage,
            HttpServletRequest request) {
        if (id <= 0 || (status != 1 && status != 2)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = friendLinkService.reviewFriendLink(id, status, reviewMessage, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 增加点击量
     *
     * @param id 友链id
     * @return 是否成功
     */
    @PostMapping("/click/{id}")
    @RateLimiter(key = "friend_link_click", time = 60, count = 20, message = "友链点击过于频繁，请稍后再试")
    public BaseResponse<Boolean> increaseClickCount(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = friendLinkService.increaseClickCount(id);
        return ResultUtils.success(result);
    }

    /**
     * 获取所有网站类型
     *
     * @return 类型列表
     */
    @GetMapping("/site-types")
    @RateLimiter(key = "friend_link_site_types", time = 60, count = 20, message = "网站类型查询过于频繁，请稍后再试")
    public BaseResponse<List<Map<String, String>>> listSiteTypes() {
        List<Map<String, String>> types = friendLinkService.listSiteTypes();
        return ResultUtils.success(types);
    }

    /**
     * 获取推荐友链列表
     *
     * @param limit 限制数量
     * @return 推荐友链列表
     */
    @GetMapping("/recommend")
    @RateLimiter(key = "friend_link_recommend", time = 60, count = 25, message = "推荐友链查询过于频繁，请稍后再试")
    public BaseResponse<List<FriendLink>> listRecommendFriendLinks(
            @RequestParam(defaultValue = "10") int limit) {
        List<FriendLink> list = friendLinkService.listRecommendFriendLinks(limit);
        return ResultUtils.success(list);
    }

    /**
     * 更新友链权重（管理员使用）
     *
     * @param id      友链id
     * @param weight  权重值
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/weight/{id}")
    @RateLimiter(key = "friend_link_weight", time = 60, count = 10, message = "友链权重更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateWeight(
            @PathVariable("id") long id,
            @RequestParam("weight") int weight,
            HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = friendLinkService.updateWeight(id, weight, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取某类型的友链数量
     *
     * @param siteType 网站类型
     * @return 友链数量
     */
    @GetMapping("/count/type")
    @RateLimiter(key = "friend_link_count_type", time = 60, count = 20, message = "友链数量统计过于频繁，请稍后再试")
    public BaseResponse<Long> countByType(@RequestParam String siteType) {
        long count = friendLinkService.countByType(siteType);
        return ResultUtils.success(count);
    }

    /**
     * 手动刷新缓存（管理员使用）
     *
     * @param request HTTP请求
     * @return 是否成功
     */
    @PostMapping("/refresh-cache")
    @RateLimiter(key = "friend_link_refresh_cache", time = 60, count = 5, message = "友链缓存刷新过于频繁，请稍后再试")
    public BaseResponse<Boolean> refreshCache(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (!loginUser.getUserRole().equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        friendLinkService.refreshCache();
        return ResultUtils.success(true);
    }
}
