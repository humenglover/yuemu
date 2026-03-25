package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.report.ReportQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Report;
import com.lumenglover.yuemupicturebackend.model.vo.ReportVO;

/**
 * 举报服务
 */
public interface ReportService extends IService<Report> {

    /**
     * 校验数据
     *
     * @param report 举报
     * @param add 对创建的数据进行校验
     */
    void validReport(Report report, boolean add);

    /**
     * 获取查询条件
     *
     * @param reportQueryRequest 查询条件
     * @return QueryWrapper
     */
    QueryWrapper<Report> getQueryWrapper(ReportQueryRequest reportQueryRequest);

    /**
     * 分页获取举报封装
     *
     * @param reportPage 分页对象
     * @param request 请求对象
     * @return ReportVO分页对象
     */
    Page<ReportVO> getReportVOPage(Page<Report> reportPage, ReportQueryRequest request);

    /**
     * 获取举报封装
     *
     * @param report 举报对象
     * @param request 请求对象
     * @return ReportVO
     */
    ReportVO getReportVO(Report report, ReportQueryRequest request);
}
