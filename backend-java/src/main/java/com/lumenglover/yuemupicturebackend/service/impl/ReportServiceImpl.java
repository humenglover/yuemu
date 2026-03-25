package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.common.PageRequest;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.ReportMapper;
import com.lumenglover.yuemupicturebackend.model.dto.report.ReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Report;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.ReportStatusEnum;
import com.lumenglover.yuemupicturebackend.model.enums.ReportTargetTypeEnum;
import com.lumenglover.yuemupicturebackend.model.enums.ReportTypeEnum;
import com.lumenglover.yuemupicturebackend.model.vo.ReportVO;
import com.lumenglover.yuemupicturebackend.service.ReportService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.ReportUtil;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import javax.annotation.Resource;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报服务实现
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Resource
    private UserService userService;

    @Resource
    private ReportUtil reportUtil;

    @Override
    public void validReport(Report report, boolean add) {
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer targetType = report.getTargetType();
        Integer reportType = report.getReportType();
        String reason = report.getReason();

        if (add) {
            if (targetType == null || !ReportTargetTypeEnum.getValues().contains(targetType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报目标类型非法");
            }
            if (targetType == null || !ReportTypeEnum.getValues().contains(reportType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报类型非法");
            }
            if (StringUtils.isBlank(reason) || reason.length() > 500) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报原因不能为空且长度不能超过500字符");
            }
        }
        if (report.getStatus() != null && !ReportStatusEnum.getValues().contains(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报状态非法");
        }
    }

    @Override
    public QueryWrapper<Report> getQueryWrapper(ReportQueryRequest reportQueryRequest) {
        QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
        if (reportQueryRequest == null) {
            return queryWrapper;
        }

        Long id = reportQueryRequest.getId();
        Long userId = reportQueryRequest.getUserId();
        Long targetId = reportQueryRequest.getTargetId();
        Integer targetType = reportQueryRequest.getTargetType();
        Integer reportType = reportQueryRequest.getReportType();
        Integer status = reportQueryRequest.getStatus();

        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(targetId != null, "targetId", targetId);
        queryWrapper.eq(targetType != null, "targetType", targetType);
        queryWrapper.eq(reportType != null, "reportType", reportType);
        queryWrapper.eq(status != null, "status", status);

        queryWrapper.orderByDesc("createTime");

        return queryWrapper;
    }

    @Override
    public Page<ReportVO> getReportVOPage(Page<Report> reportPage, ReportQueryRequest request) {
        List<Report> reportList = reportPage.getRecords();
        Page<ReportVO> reportVOPage = new Page<>();
        reportVOPage.setCurrent(reportPage.getCurrent());
        reportVOPage.setSize(reportPage.getSize());
        reportVOPage.setTotal(reportPage.getTotal());

        if (reportList == null || reportList.isEmpty()) {
            reportVOPage.setRecords(java.util.Collections.emptyList());
            return reportVOPage;
        }

        List<ReportVO> reportVOList = reportList.stream()
                .map(report -> getReportVO(report, request))
                .collect(Collectors.toList());
        reportVOPage.setRecords(reportVOList);
        return reportVOPage;
    }

    @Override
    public ReportVO getReportVO(Report report, ReportQueryRequest request) {
        if (report == null) {
            return null;
        }

        ReportVO reportVO = new ReportVO();
        // 设置基本信息
        reportVO.setId(report.getId());
        reportVO.setUserId(report.getUserId());
        reportVO.setTargetId(report.getTargetId());
        reportVO.setTargetType(report.getTargetType());
        reportVO.setReportType(report.getReportType());
        reportVO.setReason(report.getReason());
        reportVO.setStatus(report.getStatus());
        reportVO.setHandlerId(report.getHandlerId());
        reportVO.setHandleResult(report.getHandleResult());
        reportVO.setHandleTime(report.getHandleTime());
        reportVO.setCreateTime(report.getCreateTime());
        reportVO.setUpdateTime(report.getUpdateTime());

        // 设置类型文本
        if (report.getTargetType() != null) {
            ReportTargetTypeEnum targetTypeEnum = ReportTargetTypeEnum.values()[report.getTargetType() - 1];
            reportVO.setTargetTypeText(targetTypeEnum.getText());
        }
        if (report.getReportType() != null) {
            ReportTypeEnum reportTypeEnum = ReportTypeEnum.values()[report.getReportType() - 1];
            reportVO.setReportTypeText(reportTypeEnum.getText());
        }
        if (report.getStatus() != null) {
            ReportStatusEnum statusEnum = ReportStatusEnum.values()[report.getStatus()];
            reportVO.setStatusText(statusEnum.getText());
        }

        // 设置用户信息
        User user = userService.getById(report.getUserId());
        if (user != null) {
            reportVO.setUserName(user.getUserName());
            reportVO.setUserAvatar(user.getUserAvatar());
        }

        // 设置处理人信息
        if (report.getHandlerId() != null) {
            User handler = userService.getById(report.getHandlerId());
            if (handler != null) {
                reportVO.setHandlerName(handler.getUserName());
            }
        }

        // 设置截图URL列表
        if (StringUtils.isNotBlank(report.getScreenshotUrls())) {
            // 解析JSON字符串为列表
            try {
                // 使用Jackson解析JSON数组
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> screenshotUrls = objectMapper.readValue(report.getScreenshotUrls(), List.class);
                reportVO.setScreenshotUrls(screenshotUrls);
            } catch (Exception e) {
                // 解析失败则设置为空列表
                reportVO.setScreenshotUrls(java.util.Collections.emptyList());
            }
        } else {
            reportVO.setScreenshotUrls(java.util.Collections.emptyList());
        }

        return reportVO;
    }
}
