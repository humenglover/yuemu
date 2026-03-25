package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.search.SearchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.UserSearchRecord;
import com.lumenglover.yuemupicturebackend.model.vo.HotSearchVO;
import com.lumenglover.yuemupicturebackend.service.SearchService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {

    @Resource
    private SearchService searchService;

    @Resource
    private UserService userService;

    @PostMapping("/all")
    @RateLimiter(key = "search_all", time = 60, count = 20, message = "搜索操作过于频繁，请稍后再试")
    public BaseResponse<Page<?>> searchAll(@RequestBody SearchRequest searchRequest) {
        return ResultUtils.success(searchService.doSearch(searchRequest));
    }

    /**
     * 获取热门搜索关键词
     * @param type 搜索类型 (picture/user/post/space)
     * @param size 返回数量，默认9个
     * @return 热门搜索关键词列表
     */
    @GetMapping("/hot")
    @RateLimiter(key = "search_hot", time = 60, count = 30, message = "热门搜索查询过于频繁，请稍后再试")
    public BaseResponse<List<HotSearchVO>> getHotSearchKeywords(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "9") Integer size) {
        // 参数校验
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-100之间");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        return ResultUtils.success(searchService.getHotSearchWithDetails(type, size));
    }


    /**
     * 获取用户搜索历史
     * @param userId 用户ID
     * @param type 搜索类型 (picture/user/post/space)
     * @param size 返回数量，默认10个
     * @return 搜索历史列表
     */
    @GetMapping("/history")
    @RateLimiter(key = "search_history", time = 60, count = 20, message = "搜索历史查询过于频繁，请稍后再试")
    public BaseResponse<List<UserSearchRecord>> getUserSearchHistory(
            @RequestParam(required = true) Long userId,
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-100之间");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        return ResultUtils.success(searchService.getUserSearchHistory(userId, type, size));
    }

    /**
     * 删除用户指定类型的搜索历史记录
     * @param type 搜索类型 (picture/user/post/space)
     * @param request HTTP请求
     * @return 是否删除成功
     */
    @DeleteMapping("/history")
    public BaseResponse<Boolean> deleteUserSearchHistory(
            @RequestParam(required = true) String type,
            HttpServletRequest request) {
        // 参数校验
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 删除用户指定类型的搜索历史记录
        boolean result = searchService.deleteUserSearchHistoryByType(loginUser.getId(), type);
        return ResultUtils.success(result);
    }

    /**
     * 获取猜你想搜的数据
     * @param type 搜索类型 (picture/user/post/space)
     * @param size 返回数量，默认10个
     * @param request HTTP请求
     * @return 推荐搜索词列表
     */
    @GetMapping("/guess")
    @RateLimiter(key = "search_guess", time = 60, count = 20, message = "猜你想搜查询过于频繁，请稍后再试")
    public BaseResponse<List<HotSearchVO>> getGuessYouWantToSearch(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            HttpServletRequest request) {
        // 参数校验
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-100之间");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        // 获取当前登录用户（可能为空）
        User loginUser = userService.isLogin(request);
        Long userId = loginUser != null ? loginUser.getId() : null;

        return ResultUtils.success(searchService.getGuessYouWantToSearch(userId, type, size));
    }
}
