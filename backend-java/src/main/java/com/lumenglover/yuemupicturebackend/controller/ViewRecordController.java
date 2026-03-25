package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.ViewRecord;
import com.lumenglover.yuemupicturebackend.model.vo.ViewRecordVO;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.ViewRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ReportViewDurationRequest;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;

/**
 * 浏览记录控制器
 */
@RestController
@RequestMapping("/view-record")
@Slf4j
public class ViewRecordController {

    @Resource
    private ViewRecordService viewRecordService;

    @Resource
    private UserService userService;

    /**
     * 添加浏览记录
     *
     * @param viewRecordAddRequest 请求参数
     * @param request              HTTP请求
     * @return 结果
     */
    @PostMapping("/add")
    public BaseResponse<Long> addViewRecord(@RequestBody ViewRecordAddRequest viewRecordAddRequest, HttpServletRequest request) {
        if (viewRecordAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = viewRecordService.addViewRecord(viewRecordAddRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 检查是否已浏览
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @param request    HTTP请求
     * @return 结果
     */
    @GetMapping("/check")
    public BaseResponse<Boolean> checkHasViewed(@RequestParam Long userId, @RequestParam Long targetId,
                                                @RequestParam Integer targetType, HttpServletRequest request) {
        ThrowUtils.throwIf(userId == null || targetId == null || targetType == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        if (!loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查询自己的浏览记录");
        }

        boolean result = viewRecordService.hasViewed(userId, targetId, targetType);
        return ResultUtils.success(result);
    }

    /**
     * 批量上报浏览时长
     */
    @PostMapping("/report-duration")
    @RateLimiter(key = "view_record_report_duration", time = 60, count = 120, message = "浏览时长上报过于频繁，请稍后再试")
    public BaseResponse<Boolean> reportViewDuration(@RequestBody List<ReportViewDurationRequest> requestList,
                                                    HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId = loginUser != null ? loginUser.getId() : null;

        if (requestList == null || requestList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 为每个请求设置用户ID
        for (ReportViewDurationRequest request : requestList) {
            if (userId != null) {
                request.setUserId(userId);
            }
        }

        boolean result = viewRecordService.batchReportViewDuration(requestList);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户的浏览记录
     * 用户传入类型分页返回对应浏览的图片或者帖子数据
     */
    @PostMapping("/my/list/page")
    @RateLimiter(key = "view_record_my_list", time = 60, count = 20, message = "浏览记录查询过于频繁，请稍后再试")
    public BaseResponse<Page<ViewRecordVO>> listMyViewRecordByPage(@RequestBody ViewRecordQueryRequest viewRecordQueryRequest,
                                                                   HttpServletRequest request) {
        if (viewRecordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        long current = viewRecordQueryRequest.getCurrent();
        long size = viewRecordQueryRequest.getPageSize();
        Integer targetType = viewRecordQueryRequest.getTargetType();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页最多显示20条");

        Page<ViewRecordVO> viewRecordVOPage = viewRecordService.listMyViewRecordVOByPage(current, size, targetType, loginUser);
        return ResultUtils.success(viewRecordVOPage);
    }

    /**
     * 删除浏览记录（支持单个删除和批量删除）
     * 单个删除：传入包含单个ID的列表
     * 批量删除：传入包含多个ID的列表
     */
    @PostMapping("/delete")
    @RateLimiter(key = "view_record_delete", time = 60, count = 10, message = "浏览记录删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteViewRecord(@RequestBody List<String> ids, HttpServletRequest request) {
        log.info("接收到删除浏览记录请求，ID列表: {}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID列表不能为空");
        }

        // 限制批量删除数量
        ThrowUtils.throwIf(ids.size() > 50, ErrorCode.PARAMS_ERROR, "一次最多删除50条记录");

        // 将字符串ID转换为Long
        List<Long> longIds = ids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        log.info("转换后的Long ID列表: {}", longIds);

        User loginUser = userService.getLoginUser(request);
        boolean result = viewRecordService.deleteViewRecordBatch(longIds, loginUser);

        log.info("删除操作完成，结果: {}", result);
        return ResultUtils.success(result);
    }
}
