package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.BugReportMapper;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.BugReport;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.BugReportVO;
import com.lumenglover.yuemupicturebackend.service.BugReportService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * bug报告服务实现
 *
 * @author 鹿梦
 */
@Service
public class BugReportServiceImpl extends ServiceImpl<BugReportMapper, BugReport> implements BugReportService {

    @Resource
    private UserService userService;

    @Override
    public void validBugReport(BugReport bugReport, boolean add) {
        if (bugReport == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = bugReport.getTitle();
        String description = bugReport.getDescription();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, description), ErrorCode.PARAMS_ERROR);
        }

        if (!ObjectUtil.isEmpty(title) && title.length() > 255) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }

        if (!ObjectUtil.isEmpty(description) && description.length() > 5000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述过长");
        }
    }

    @Override
    public QueryWrapper<BugReport> getQueryWrapper(BugReportQueryRequest bugReportQueryRequest) {
        QueryWrapper<BugReport> queryWrapper = new QueryWrapper<>();
        if (bugReportQueryRequest == null) {
            return queryWrapper;
        }
        Long id = bugReportQueryRequest.getId();
        Long userId = bugReportQueryRequest.getUserId();
        String title = bugReportQueryRequest.getTitle();
        Integer bugType = bugReportQueryRequest.getBugType();
        Integer status = bugReportQueryRequest.getStatus();
        String websiteUrl = bugReportQueryRequest.getWebsiteUrl();

        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq(bugType != null, "bugType", bugType);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.like(StringUtils.isNotBlank(websiteUrl), "websiteUrl", websiteUrl);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }

    @Override
    public BugReportVO getBugReportVO(BugReport bugReport, HttpServletRequest request) {
        BugReportVO bugReportVO = BugReportVO.objToVo(bugReport);

        // 脱敏：根据实际情况设置用户信息
        Long userId = bugReport.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                bugReportVO.setUserName(user.getUserName());
            }
        }

        return bugReportVO;
    }

    @Override
    public Page<BugReportVO> getBugReportVOPage(Page<BugReport> bugReportPage, HttpServletRequest request) {
        List<BugReport> bugReportList = bugReportPage.getRecords();
        Page<BugReportVO> bugReportVOPage = new Page<>(bugReportPage.getCurrent(), bugReportPage.getSize(), bugReportPage.getTotal());
        if (CollUtil.isEmpty(bugReportList)) {
            return bugReportVOPage;
        }
        List<BugReportVO> bugReportVOList = bugReportList.stream().map(bugReport -> {
            return getBugReportVO(bugReport, request);
        }).collect(Collectors.toList());
        bugReportVOPage.setRecords(bugReportVOList);
        return bugReportVOPage;
    }

    @Override
    public Long addBugReport(BugReportAddRequest bugReportAddRequest, HttpServletRequest request) {
        BugReport bugReport = new BugReport();
        bugReport.setTitle(bugReportAddRequest.getTitle());
        bugReport.setDescription(bugReportAddRequest.getDescription());
        bugReport.setBugType(bugReportAddRequest.getBugType());
        bugReport.setScreenshotUrls(bugReportAddRequest.getScreenshotUrls());
        bugReport.setWebsiteUrl(bugReportAddRequest.getWebsiteUrl());
        bugReport.setStatus(0); // 默认为待处理状态

        // 设置当前登录用户
        User loginUser = userService.getLoginUser(request);
        bugReport.setUserId(loginUser.getId());

        this.validBugReport(bugReport, true);
        boolean result = this.save(bugReport);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return bugReport.getId();
    }

    @Override
    public boolean deleteBugReport(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BugReport oldBugReport = this.getById(id);
        if (oldBugReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        // 只有管理员或创建者才能删除
        if (!oldBugReport.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean b = this.removeById(id);
        return b;
    }

    @Override
    public boolean updateBugReport(BugReportUpdateRequest bugReportUpdateRequest, HttpServletRequest request) {
        if (bugReportUpdateRequest.getId() == null || bugReportUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        BugReport bugReport = new BugReport();
        bugReport.setId(bugReportUpdateRequest.getId());
        bugReport.setTitle(bugReportUpdateRequest.getTitle());
        bugReport.setDescription(bugReportUpdateRequest.getDescription());
        bugReport.setBugType(bugReportUpdateRequest.getBugType());
        bugReport.setStatus(bugReportUpdateRequest.getStatus());
        bugReport.setScreenshotUrls(bugReportUpdateRequest.getScreenshotUrls());
        bugReport.setWebsiteUrl(bugReportUpdateRequest.getWebsiteUrl());

        // 只有管理员才能更新
        User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 数据校验
        BugReport oldBugReport = this.getById(bugReportUpdateRequest.getId());
        if (oldBugReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        this.validBugReport(bugReport, false);
        boolean result = this.updateById(bugReport);
        return result;
    }

    @Override
    public BugReportVO getBugReportById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BugReport bugReport = this.getById(id);
        if (bugReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return BugReportVO.objToVo(bugReport);
    }

    @Override
    public boolean updateBugStatus(Long id, Integer status, String resolution) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        BugReport bugReport = this.getById(id);
        if (bugReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 更新状态和解决信息
        bugReport.setStatus(status);
        if (status >= 2) { // 如果是已解决或更高状态，则设置解决时间
            bugReport.setResolvedTime(new Date());
        }
        if (StringUtils.isNotBlank(resolution)) {
            bugReport.setResolution(resolution);
        }

        boolean result = this.updateById(bugReport);
        return result;
    }
}
