package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.bugreport.BugReportUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.BugReport;
import com.lumenglover.yuemupicturebackend.model.vo.BugReportVO;

import javax.servlet.http.HttpServletRequest;

/**
 * bug报告服务
 *
 * @author 鹿梦
 */
public interface BugReportService extends IService<BugReport> {

    /**
     * 校验数据
     *
     * @param bugReport
     * @param add 对创建的数据进行校验
     */
    void validBugReport(BugReport bugReport, boolean add);

    /**
     * 获取查询条件
     *
     * @param bugReportQueryRequest
     * @return
     */
    QueryWrapper<BugReport> getQueryWrapper(BugReportQueryRequest bugReportQueryRequest);

    /**
     * 获取bug报告封装
     *
     * @param bugReport
     * @param request
     * @return
     */
    BugReportVO getBugReportVO(BugReport bugReport, HttpServletRequest request);

    /**
     * 分页获取bug报告封装
     *
     * @param bugReportPage
     * @param request
     * @return
     */
    Page<BugReportVO> getBugReportVOPage(Page<BugReport> bugReportPage, HttpServletRequest request);

    /**
     * 创建bug报告
     *
     * @param bugReportAddRequest
     * @param request
     * @return
     */
    Long addBugReport(BugReportAddRequest bugReportAddRequest, HttpServletRequest request);

    /**
     * 删除bug报告
     *
     * @param id
     * @param request
     * @return
     */
    boolean deleteBugReport(Long id, HttpServletRequest request);

    /**
     * 更新bug报告（仅管理员可用）
     *
     * @param bugReportUpdateRequest
     * @param request
     * @return
     */
    boolean updateBugReport(BugReportUpdateRequest bugReportUpdateRequest, HttpServletRequest request);

    /**
     * 根据 id 获取 bug报告信息
     *
     * @param id
     * @return
     */
    BugReportVO getBugReportById(Long id);

    /**
     * 更新bug报告状态（管理员解决bug）
     *
     * @param id
     * @param status
     * @param resolution
     * @return
     */
    boolean updateBugStatus(Long id, Integer status, String resolution);
}
