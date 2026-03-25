package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.ViewRecord;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ReportViewDurationRequest;
import com.lumenglover.yuemupicturebackend.model.vo.ViewRecordVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 浏览记录服务
 */
public interface ViewRecordService extends IService<ViewRecord> {

    /**
     * 添加浏览记录
     *
     * @param viewRecordAddRequest 请求参数
     * @param request              HTTP请求
     * @return 浏览记录ID
     */
    long addViewRecord(ViewRecordAddRequest viewRecordAddRequest, HttpServletRequest request);

    /**
     * 检查是否已经浏览过
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 是否已浏览
     */
    boolean hasViewed(Long userId, Long targetId, Integer targetType);

    /**
     * 上报浏览时长
     *
     * @param request 请求参数
     * @return 是否成功
     */
    boolean reportViewDuration(ReportViewDurationRequest request);

    /**
     * 批量上报浏览时长
     *
     * @param requestList 请求参数列表
     * @return 是否全部成功
     */
    boolean batchReportViewDuration(java.util.List<ReportViewDurationRequest> requestList);

    /**
     * 分页获取用户的浏览记录
     *
     * @param current     页码
     * @param size        每页大小
     * @param targetType  目标类型
     * @param loginUser   登录用户
     * @return 浏览记录分页
     */
    Page<ViewRecordVO> listMyViewRecordVOByPage(long current, long size, Integer targetType, User loginUser);

    /**
     * 批量删除浏览记录
     *
     * @param ids         浏览记录ID列表
     * @param loginUser   登录用户
     * @return 是否成功
     */
    boolean deleteViewRecordBatch(List<Long> ids, User loginUser);
}
