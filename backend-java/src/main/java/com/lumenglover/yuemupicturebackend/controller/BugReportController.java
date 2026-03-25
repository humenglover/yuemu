package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter; // 添加限流器注解
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.BugReport;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.BugReportVO;
import com.lumenglover.yuemupicturebackend.service.BugReportService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * bug报告接口
 *
 * @author 鹿梦
 */
@RestController
@RequestMapping("/bugReport")
@Slf4j
public class BugReportController {

    @Resource
    private BugReportService bugReportService;

    @Resource
    private UserService userService;

    /**
     * 创建bug报告
     *
     * @param bugReportAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @RateLimiter(key = "bug_report_add", time = 60, count = 30, message = "创建bug报告过于频繁，请稍后再试")
    public BaseResponse<Long> addBugReport(@RequestBody BugReportAddRequest bugReportAddRequest, HttpServletRequest request) {
        if (bugReportAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = bugReportService.addBugReport(bugReportAddRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 删除bug报告
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @RateLimiter(key = "bug_report_delete", time = 60, count = 30, message = "删除bug报告过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteBugReport(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = bugReportService.deleteBugReport(deleteRequest.getId(), request);
        return ResultUtils.success(b);
    }

    /**
     * 更新bug报告（仅管理员）
     *
     * @param bugReportUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "bug_report_update", time = 60, count = 30, message = "更新bug报告过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateBugReport(@RequestBody BugReportUpdateRequest bugReportUpdateRequest, HttpServletRequest request) {
        if (bugReportUpdateRequest == null || bugReportUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = bugReportService.updateBugReport(bugReportUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取bug报告
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @RateLimiter(key = "bug_report_get", time = 60, count = 60, message = "获取bug报告过于频繁，请稍后再试")
    public BaseResponse<BugReportVO> getBugReportById(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BugReportVO bugReportVO = bugReportService.getBugReportById(id);
        return ResultUtils.success(bugReportVO);
    }

    /**
     * 分页获取bug报告列表
     *
     * @param bugReportQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @RateLimiter(key = "bug_report_list_page", time = 60, count = 60, message = "获取bug报告列表过于频繁，请稍后再试")
    public BaseResponse<Page<BugReportVO>> listBugReportByPage(@RequestBody BugReportQueryRequest bugReportQueryRequest,
                                                               HttpServletRequest request) {
        long current = bugReportQueryRequest.getCurrent();
        long size = bugReportQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<BugReport> bugReportPage = bugReportService.page(new Page<>(current, size),
                bugReportService.getQueryWrapper(bugReportQueryRequest));
        return ResultUtils.success(bugReportService.getBugReportVOPage(bugReportPage, request));
    }

    /**
     * 分页获取我的bug报告列表（普通用户）
     *
     * @param bugReportQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    @RateLimiter(key = "bug_report_my_list_page", time = 60, count = 60, message = "获取我的bug报告列表过于频繁，请稍后再试")
    public BaseResponse<Page<BugReportVO>> listMyBugReportByPage(@RequestBody BugReportQueryRequest bugReportQueryRequest,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long current = bugReportQueryRequest.getCurrent();
        long size = bugReportQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询条件加上用户ID
        BugReportQueryRequest queryRequest = new BugReportQueryRequest();
        BeanUtils.copyProperties(bugReportQueryRequest, queryRequest);
        queryRequest.setUserId(loginUser.getId());
        Page<BugReport> bugReportPage = bugReportService.page(new Page<>(current, size),
                bugReportService.getQueryWrapper(queryRequest));
        return ResultUtils.success(bugReportService.getBugReportVOPage(bugReportPage, request));
    }

    /**
     * 管理员解决bug报告
     *
     * @param id
     * @param status
     * @param resolution
     * @return
     */
    @PostMapping("/admin/solve")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RateLimiter(key = "bug_report_solve", time = 60, count = 30, message = "解决bug报告过于频繁，请稍后再试")
    public BaseResponse<Boolean> solveBugReport(Long id, Integer status, String resolution) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = bugReportService.updateBugStatus(id, status, resolution);
        return ResultUtils.success(result);
    }
}
