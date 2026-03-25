package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.report.ReportAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.report.ReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.report.ReportUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Report;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.ReportVO;
import com.lumenglover.yuemupicturebackend.service.ReportService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Date;

/**
 * 举报接口
 */
@RestController
@RequestMapping("/report")
@Slf4j
public class ReportController {

    @Resource
    private ReportService reportService;

    @Resource
    private UserService userService;

    @Resource
    private SystemNotifyService systemNotifyService;

    /**
     * 创建举报
     *
     * @param reportAddRequest 举报创建请求
     * @param request HTTP请求
     * @return 举报ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addReport(@RequestBody ReportAddRequest reportAddRequest, HttpServletRequest request) {
        if (reportAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验参数
        ThrowUtils.throwIf(reportAddRequest.getTargetId() == null || reportAddRequest.getTargetId() <= 0,
                ErrorCode.PARAMS_ERROR, "被举报内容ID不能为空");
        ThrowUtils.throwIf(reportAddRequest.getTargetType() == null || reportAddRequest.getTargetType() < 1 || reportAddRequest.getTargetType() > 5,
                ErrorCode.PARAMS_ERROR, "举报目标类型非法");
        ThrowUtils.throwIf(reportAddRequest.getReportType() == null || reportAddRequest.getReportType() < 1 || reportAddRequest.getReportType() > 7,
                ErrorCode.PARAMS_ERROR, "举报类型非法");
        ThrowUtils.throwIf(reportAddRequest.getReason() == null || reportAddRequest.getReason().trim().length() == 0 || reportAddRequest.getReason().length() > 500,
                ErrorCode.PARAMS_ERROR, "举报原因不能为空且长度不能超过500字符");

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 创建举报对象
        Report report = new Report();
        BeanUtils.copyProperties(reportAddRequest, report);
        report.setUserId(loginUser.getId());
        report.setStatus(0); // 待处理状态

        // 如果有截图URL列表，转换为JSON字符串存储
        if (reportAddRequest.getScreenshotUrls() != null && !reportAddRequest.getScreenshotUrls().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String screenshotUrlsJson = objectMapper.writeValueAsString(reportAddRequest.getScreenshotUrls());
                report.setScreenshotUrls(screenshotUrlsJson);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "截图URL序列化失败");
            }
        }

        // 校验数据
        reportService.validReport(report, true);

        // 插入数据
        boolean result = reportService.save(report);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "举报提交失败");

        return ResultUtils.success(report.getId());
    }

    /**
     * 删除举报
     *
     * @param deleteRequest 删除请求
     * @param request HTTP请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteReport(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 管理员可删除
        long id = deleteRequest.getId();
        boolean result = reportService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新举报（主要用于管理员处理）
     *
     * @param reportUpdateRequest 举报更新请求
     * @param request HTTP请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateReport(@RequestBody ReportUpdateRequest reportUpdateRequest, HttpServletRequest request) {
        if (reportUpdateRequest == null || reportUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户（管理员）
        User loginUser = userService.getLoginUser(request);

        // 获取原举报信息
        Report oldReport = reportService.getById(reportUpdateRequest.getId());
        if (oldReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        }

        // 构造更新对象
        Report report = new Report();
        BeanUtils.copyProperties(reportUpdateRequest, report);
        report.setHandlerId(loginUser.getId()); // 记录处理人ID
        report.setHandleTime(new Date()); // 记录处理时间

        // 校验数据
        reportService.validReport(report, false);

        // 更新数据
        boolean result = reportService.updateById(report);

        // 如果更新成功且状态已改变，发送系统通知给举报人
        if (result && oldReport.getStatus() != report.getStatus()) {
            sendNotificationToReporter(report.getId());
        }

        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取举报
     *
     * @param id 举报ID
     * @return 举报信息
     */
    @GetMapping("/get")
    public BaseResponse<ReportVO> getReportById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Report report = reportService.getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 构造查询请求对象
        ReportQueryRequest reportQueryRequest = new ReportQueryRequest();
        reportQueryRequest.setId(id);

        // 封装成VO返回
        ReportVO reportVO = reportService.getReportVO(report, reportQueryRequest);
        return ResultUtils.success(reportVO);
    }

    /**
     * 分页获取举报列表
     *
     * @param reportQueryRequest 举报查询请求
     * @param request HTTP请求
     * @return 分页举报列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ReportVO>> listReportByPage(@RequestBody ReportQueryRequest reportQueryRequest, HttpServletRequest request) {
        if (reportQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        long current = reportQueryRequest.getCurrent();
        long size = reportQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页条数不能大于20");

        // 查询数据库
        Page<Report> reportPage = reportService.page(new Page<>(current, size),
                reportService.getQueryWrapper(reportQueryRequest));

        // 封装成VO返回
        Page<ReportVO> reportVOPage = reportService.getReportVOPage(reportPage, reportQueryRequest);
        return ResultUtils.success(reportVOPage);
    }

    /**
     * 获取我的举报列表
     *
     * @param reportQueryRequest 举报查询请求
     * @param request HTTP请求
     * @return 分页举报列表
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<ReportVO>> listMyReportByPage(@RequestBody ReportQueryRequest reportQueryRequest, HttpServletRequest request) {
        if (reportQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        reportQueryRequest.setUserId(loginUser.getId());

        long current = reportQueryRequest.getCurrent();
        long size = reportQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页条数不能大于20");

        // 查询数据库
        Page<Report> reportPage = reportService.page(new Page<>(current, size),
                reportService.getQueryWrapper(reportQueryRequest));

        // 封装成VO返回
        Page<ReportVO> reportVOPage = reportService.getReportVOPage(reportPage, reportQueryRequest);
        return ResultUtils.success(reportVOPage);
    }

    /**
     * 发送通知给举报人
     *
     * @param reportId 举报ID
     */
    private void sendNotificationToReporter(Long reportId) {
        try {
            // 通过举报ID直接查询数据库获取举报信息
            Report report = reportService.getById(reportId);
            if (report == null) {
                log.warn("举报记录不存在，举报ID: {}", reportId);
                return;
            }

            // 获取举报人ID
            Long reporterId = report.getUserId();
            if (reporterId == null) {
                log.warn("无法获取举报人ID，举报ID: {}", reportId);
                return;
            }

            // 构建通知内容
            String title = "您的举报处理结果";
            String content = "您举报的内容";

            // 根据举报目标类型添加描述
            switch (report.getTargetType()) {
                case 1: // 图片
                    content += "图片";
                    break;
                case 2: // 帖子
                    content += "帖子";
                    break;
                case 3: // 评论
                    content += "评论";
                    break;
                case 4: // 用户
                    content += "用户";
                    break;
                case 5: // 其他
                    content += "内容";
                    break;
                default:
                    content += "内容";
                    break;
            }

            // 添加处理结果
            if (report.getStatus() == 1) { // 已处理，表示举报成立
                content += "经审核存在违规行为，举报成立。";
            } else if (report.getStatus() == 2) { // 驳回，表示举报不成立
                content += "经审核未发现违规行为，举报不成立。";
            } else {
                content += "相关举报正在处理中。";
            }

            // 如果有处理结果说明，也添加进去
            if (report.getHandleResult() != null && !report.getHandleResult().trim().isEmpty()) {
                content += " 处理说明: " + report.getHandleResult();
            }

            // 创建系统通知对象
            SystemNotify systemNotify = new SystemNotify();
            systemNotify.setTitle(title);
            systemNotify.setContent(content);
            systemNotify.setNotifyType("REPORT_RESULT");
            systemNotify.setSenderType("SYSTEM");
            systemNotify.setSenderId("0"); // 系统发送
            systemNotify.setReceiverType("SPECIFIC_USER");
            systemNotify.setReceiverId(String.valueOf(reporterId));
            systemNotify.setIsGlobal(0); // 非全局通知
            systemNotify.setIsEnabled(1); // 启用通知
            systemNotify.setReadStatus(0); // 未读状态
            systemNotify.setRelatedBizType("REPORT");
            systemNotify.setRelatedBizId(String.valueOf(report.getId())); // 关联举报ID

            // 设置操作人信息
            systemNotify.setOperatorId(String.valueOf(report.getHandlerId()));
            systemNotify.setOperatorType("ADMIN");

            // 添加通知图标（可选）
            systemNotify.setNotifyIcon("/icons/report.png");

            // 保存通知
            systemNotifyService.addSystemNotify(systemNotify);

            log.info("已发送举报处理结果通知给举报人: {}, 举报ID: {}", reporterId, report.getId());

        } catch (Exception e) {
            log.error("发送举报处理结果通知失败", e);
        }
    }
}
