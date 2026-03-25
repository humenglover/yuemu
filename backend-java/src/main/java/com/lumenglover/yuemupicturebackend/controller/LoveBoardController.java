package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardBatchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoard;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.LoveBoardService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 恋爱画板接口
 */
@RestController
@RequestMapping("/love-board")
@Slf4j
public class LoveBoardController {

    @Resource
    private LoveBoardService loveBoardService;

    @Resource
    private UserService userService;

    // region 管理员接口

    /**
     * 分页获取恋爱画板列表（仅管理员可用）
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "love_board_list_admin", time = 60, count = 60, message = "管理员恋爱画板列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<LoveBoard>> listLoveBoardsByPage(@RequestBody LoveBoardAdminRequest loveBoardAdminRequest) {
        long current = loveBoardAdminRequest.getCurrent();
        long size = loveBoardAdminRequest.getPageSize();
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<LoveBoard> loveBoardPage = loveBoardService.page(new Page<>(current, size),
                loveBoardService.getAdminQueryWrapper(loveBoardAdminRequest));
        return ResultUtils.success(loveBoardPage);
    }

    /**
     * 根据 id 获取恋爱画板（仅管理员可用）
     */
    @GetMapping("/get/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "love_board_get_admin", time = 60, count = 60, message = "管理员恋爱画板详情查询过于频繁，请稍后再试")
    public BaseResponse<LoveBoard> getLoveBoardByIdAdmin(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoveBoard loveBoard = loveBoardService.getById(id);
        if (loveBoard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(loveBoard);
    }

    /**
     * 更新恋爱画板（仅管理员可用）
     */
    @PostMapping("/update/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "love_board_update_admin", time = 60, count = 60, message = "管理员恋爱画板更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateLoveBoardAdmin(@RequestBody LoveBoard loveBoard) {
        if (loveBoard == null || loveBoard.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = loveBoardService.updateById(loveBoard);
        return ResultUtils.success(result);
    }

    /**
     * 批量操作恋爱画板（仅管理员可用）
     */
    @PostMapping("/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "love_board_batch", time = 60, count = 60, message = "恋爱画板批量操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> batchOperationLoveBoards(@RequestBody LoveBoardBatchRequest loveBoardBatchRequest,
                                                          HttpServletRequest request) {
        if (loveBoardBatchRequest == null
                || loveBoardBatchRequest.getIds() == null
                || loveBoardBatchRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = loveBoardService.batchOperationLoveBoards(loveBoardBatchRequest);
        return ResultUtils.success(result);
    }

    // endregion

    // region 用户接口

    /**
     * 创建恋爱画板
     *
     * @param loveBoard
     * @param request
     * @return
     */
    @PostMapping("/add")
    @RateLimiter(key = "love_board_add", time = 60, count = 60, message = "恋爱画板创建过于频繁，请稍后再试")
    public BaseResponse<Long> addLoveBoard(@RequestBody LoveBoard loveBoard, HttpServletRequest request) {
        if (loveBoard == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long loveBoardId = loveBoardService.createLoveBoard(loveBoard, loginUser.getId());
        return ResultUtils.success(loveBoardId);
    }

    /**
     * 删除恋爱画板
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @RateLimiter(key = "love_board_delete", time = 60, count = 60, message = "恋爱画板删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteLoveBoard(@RequestParam("id") long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = loveBoardService.deleteLoveBoard(id, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新恋爱画板
     *
     * @param loveBoard
     * @param request
     * @return
     */
    @PostMapping("/update")
    @RateLimiter(key = "love_board_update", time = 60, count = 60, message = "恋爱画板更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateLoveBoard(@RequestBody LoveBoard loveBoard, HttpServletRequest request) {
        if (loveBoard == null || loveBoard.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = loveBoardService.updateLoveBoard(loveBoard, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取恋爱画板
     */
    @GetMapping("/get")
    @RateLimiter(key = "love_board_get", time = 60, count = 30, message = "恋爱画板详情查询过于频繁，请稍后再试")
    public BaseResponse<LoveBoard> getLoveBoardById(@RequestParam("id") long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户（如果有）
        User loginUser = userService.isLogin(request);
        Long loginUserId = loginUser != null ? loginUser.getId() : null;
        LoveBoard loveBoard = loveBoardService.getLoveBoardById(id, loginUserId);
        return ResultUtils.success(loveBoard);
    }

    /**
     * 获取当前用户的恋爱画板
     */
    @GetMapping("/my")
    @RateLimiter(key = "love_board_my", time = 60, count = 60, message = "我的恋爱画板查询过于频繁，请稍后再试")
    public BaseResponse<LoveBoard> getMyLoveBoard(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<LoveBoard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.eq("isDelete", 0);
        LoveBoard loveBoard = loveBoardService.getOne(queryWrapper);

        if (loveBoard != null) {
            // 增加浏览量
            Long realTimeViewCount = loveBoardService.incrementViewCount(loveBoard.getId());
            loveBoard.setViewCount(realTimeViewCount);

            // 替换URL为自定义域名
            loveBoard.replaceUrlWithCustomDomain();
        }

        return ResultUtils.success(loveBoard);
    }

    /**
     * 分页获取公共恋爱画板列表（无需登录）
     */
    @GetMapping("/list/public")
    @RateLimiter(key = "love_board_list_public", time = 60, count = 120, message = "公共恋爱画板列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<LoveBoard>> listPublicLoveBoards(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "15") long size,
            @RequestParam(value = "manName", required = false) String manName,
            @RequestParam(value = "womanName", required = false) String womanName,
            @RequestParam(value = "sortField", defaultValue = "viewCount") String sortField,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<LoveBoard> page = loveBoardService.listPublicLoveBoards(current, size, manName, womanName, sortField, sortOrder);
        return ResultUtils.success(page);
    }

    // endregion
}
