package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.ViewRecord;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.service.ViewRecordService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.service.PostService;
import com.lumenglover.yuemupicturebackend.mapper.ViewRecordMapper;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.vo.ViewRecordVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ReportViewDurationRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 浏览记录服务实现类
 */
@Service
@Slf4j
public class ViewRecordServiceImpl extends ServiceImpl<ViewRecordMapper, ViewRecord> implements ViewRecordService {

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    @Lazy
    private PostService postService;

    @Resource
    @Lazy
    private UserService userService;

    /**
     * 添加浏览记录
     *
     * @param viewRecordAddRequest 请求参数
     * @param request              HTTP请求
     * @return 浏览记录ID
     */
    @Override
    public long addViewRecord(ViewRecordAddRequest viewRecordAddRequest, HttpServletRequest request) {
        if (viewRecordAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = viewRecordAddRequest.getUserId();
        Long targetId = viewRecordAddRequest.getTargetId();
        Integer targetType = viewRecordAddRequest.getTargetType();

        if (userId == null || targetId == null || targetType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查是否已存在相同的浏览记录（仅判断是否存在，减少数据传输）
        QueryWrapper<ViewRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("isDelete", 0); // 未删除的记录

        ViewRecord existingRecord = this.getOne(queryWrapper, false); // 不需要返回全部字段
        if (existingRecord != null) {
            // 仅当传入了新的浏览时长时才更新
            if (viewRecordAddRequest.getViewDuration() != null) {
                existingRecord.setViewDuration(viewRecordAddRequest.getViewDuration());
            }

            this.updateById(existingRecord);
            return existingRecord.getId();
        } else {
            // 创建新的浏览记录
            ViewRecord viewRecord = new ViewRecord();
            viewRecord.setUserId(userId);
            viewRecord.setTargetId(targetId);
            viewRecord.setTargetType(targetType);
            viewRecord.setViewDuration(viewRecordAddRequest.getViewDuration());
            this.save(viewRecord);
            return viewRecord.getId();
        }
    }

    @Override
    public boolean hasViewed(Long userId, Long targetId, Integer targetType) {
        if (userId == null || targetId == null || targetType == null) {
            return false;
        }

        QueryWrapper<ViewRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("isDelete", 0); // 未删除的记录

        long count = this.count(queryWrapper);
        return count > 0;
    }

    @Override
    public boolean reportViewDuration(ReportViewDurationRequest request) {
        if (request == null || request.getUserId() == null || request.getTargetId() == null
                || request.getTargetType() == null || request.getDuration() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 服务端验证：检查时长合理性（例如不超过12小时）
        long durationMs = request.getDuration();
        if (durationMs > 12 * 60 * 60 * 1000L) { // 12小时
            durationMs = 12 * 60 * 60 * 1000L; // 限制最大时长
        }

        // 将毫秒转换为秒存储
        int durationSec = (int) (durationMs / 1000);

        // 检查时间戳合理性（与服务端时间差异不能太大）
        if (request.getClientTimestamp() != null) {
            long currentTime = System.currentTimeMillis();
            long clientTime = request.getClientTimestamp();
            // 时间差异不能超过10分钟
            if (Math.abs(currentTime - clientTime) > 10 * 60 * 1000L) {
                // 如果时间戳差异过大，仍然处理请求但可能需要记录日志
                System.out.println("客户端时间戳与服务端时间差异较大: " + Math.abs(currentTime - clientTime) + "ms");
            }
        }

        // 检查是否已存在相同的浏览记录，如果有则累加时长
        QueryWrapper<ViewRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", request.getUserId())
                .eq("targetId", request.getTargetId())
                .eq("targetType", request.getTargetType())
                .eq("isDelete", 0); // 未删除的记录

        ViewRecord existingRecord = this.getOne(queryWrapper, false);
        int totalDuration = durationSec; // 默认为本次上报的时长

        if (existingRecord != null) {
            // 如果已存在记录，则累加时长
            int existingDuration = existingRecord.getViewDuration() != null ? existingRecord.getViewDuration() : 0;
            totalDuration = existingDuration + durationSec;

            // 更新现有记录的时长
            existingRecord.setViewDuration(totalDuration);
            this.updateById(existingRecord);
        } else {
            // 创建新的浏览记录
            ViewRecord viewRecord = new ViewRecord();
            viewRecord.setUserId(request.getUserId());
            viewRecord.setTargetId(request.getTargetId());
            viewRecord.setTargetType(request.getTargetType());
            viewRecord.setViewDuration(totalDuration);
            this.save(viewRecord);
        }

        return true;
    }

    @Override
    public boolean batchReportViewDuration(List<ReportViewDurationRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 对请求进行分组，合并相同目标的浏览时长
        Map<String, ReportViewDurationRequest> groupedRequests = new HashMap<>();

        for (ReportViewDurationRequest request : requestList) {
            // 验证参数
            if (request.getTargetId() == null || request.getDuration() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少必要参数");
            }

            // 尝试解析字符串形式的targetId为Long
            Long targetId;
            try {
                targetId = Long.valueOf(request.getTargetId().toString());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "targetId格式错误");
            }

            // 构造唯一键，格式为: userId_targetId_targetType
            String key = request.getUserId() + "_" + targetId + "_" + request.getTargetType();

            if (groupedRequests.containsKey(key)) {
                // 如果已存在相同的目标记录，累加时长
                ReportViewDurationRequest existingRequest = groupedRequests.get(key);
                existingRequest.setDuration(existingRequest.getDuration() + request.getDuration());
                // 更新时间戳为最新的
                if (request.getClientTimestamp() != null &&
                        existingRequest.getClientTimestamp() != null &&
                        request.getClientTimestamp() > existingRequest.getClientTimestamp()) {
                    existingRequest.setClientTimestamp(request.getClientTimestamp());
                }
            } else {
                // 设置目标ID并加入映射
                request.setTargetId(targetId);
                groupedRequests.put(key, request);
            }
        }

        boolean allSuccess = true;
        // 处理合并后的请求
        for (ReportViewDurationRequest request : groupedRequests.values()) {
            boolean result = reportViewDuration(request);
            if (!result) {
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    @Override
    public Page<ViewRecordVO> listMyViewRecordVOByPage(long current, long size, Integer targetType, User loginUser) {
        // 创建分页对象
        Page<ViewRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<ViewRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("isDelete", 0) // 只查询未删除的记录
                .orderByDesc("createTime"); // 按创建时间倒序排列

        // 如果指定了目标类型，则添加条件
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        // 执行查询
        Page<ViewRecord> viewRecordPage = this.page(page, queryWrapper);

        // 转换为VO对象
        Page<ViewRecordVO> viewRecordVOPage = new Page<>(current, size, viewRecordPage.getTotal());
        List<ViewRecordVO> viewRecordVOList = viewRecordPage.getRecords().stream().map(viewRecord -> {
            ViewRecordVO viewRecordVO = new ViewRecordVO();
            viewRecordVO.setId(viewRecord.getId());
            viewRecordVO.setUserId(viewRecord.getUserId());
            viewRecordVO.setTargetId(viewRecord.getTargetId());
            viewRecordVO.setTargetType(viewRecord.getTargetType());
            viewRecordVO.setViewDuration(viewRecord.getViewDuration());
            // 转换Date为LocalDateTime
            if (viewRecord.getCreateTime() != null) {
                viewRecordVO.setCreateTime(viewRecord.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            }

            // 设置目标相关信息（需要从对应的表中查询）
            if (viewRecord.getTargetType() == 1) { // 图片类型
                // 查询图片信息
                Picture picture = pictureService.getById(viewRecord.getTargetId());
                if (picture != null) {
                    viewRecordVO.setTargetTitle(picture.getName());
                    viewRecordVO.setTargetCover(picture.getThumbnailUrl());

                    // 查询图片作者信息
                    User author = userService.getById(picture.getUserId());
                    if (author != null) {
                        viewRecordVO.setTargetAuthorUsername(author.getUserName());
                        viewRecordVO.setTargetAuthorAvatar(author.getUserAvatar());
                    }
                }
            } else if (viewRecord.getTargetType() == 2) { // 帖子类型
                // 查询帖子信息
                Post post = postService.getById(viewRecord.getTargetId());
                if (post != null) {
                    viewRecordVO.setTargetTitle(post.getTitle());
                    viewRecordVO.setTargetCover(null); // 不返回帖子封面图，设为空

                    // 查询帖子作者信息
                    User author = userService.getById(post.getUserId());
                    if (author != null) {
                        viewRecordVO.setTargetAuthorUsername(author.getUserName());
                        viewRecordVO.setTargetAuthorAvatar(author.getUserAvatar());
                    }
                }
            }

            return viewRecordVO;
        }).collect(Collectors.toList());

        viewRecordVOPage.setRecords(viewRecordVOList);
        return viewRecordVOPage;
    }

    @Override
    public boolean deleteViewRecordBatch(List<Long> ids, User loginUser) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID列表不能为空");
        }

        log.info("开始删除浏览记录，待删除ID列表: {}，当前用户ID: {}", ids, loginUser.getId());

        // 构建查询条件，确保只能删除自己的记录
        QueryWrapper<ViewRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids)
                .eq("userId", loginUser.getId())
                .eq("isDelete", 0); // 只能删除未删除的记录

        List<ViewRecord> recordsToDelete = this.list(queryWrapper);

        List<Long> foundIds = recordsToDelete.stream().map(ViewRecord::getId).collect(Collectors.toList());
        log.info("查询到的待删除记录数量: {}, 记录ID: {}", recordsToDelete.size(), foundIds);

        // 如果找不到任何匹配的记录，说明无权限删除
        if (CollectionUtils.isEmpty(recordsToDelete)) {
            log.warn("未找到匹配的浏览记录，可能的原因：1)ID不存在 2)非本人记录 3)记录已被删除，待查ID: {}", ids);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除指定的浏览记录");
        }

        // 提取要删除的记录ID
        List<Long> validIds = recordsToDelete.stream()
                .map(ViewRecord::getId)
                .collect(Collectors.toList());

        log.info("准备执行软删除，有效ID列表: {}", validIds);

        // 执行批量软删除
        // 使用 update wrapper 直接设置要更新的字段，避免MyBatis-Plus只更新非空字段的问题
        UpdateWrapper<ViewRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("isDelete", 1);
        updateWrapper.set("updateTime", new Date());
        updateWrapper.in("id", validIds);

        log.info("即将执行更新操作，更新条件ID: {}", validIds);

        boolean result = this.update(updateWrapper);

        log.info("软删除执行结果: {}，影响记录数: {}", result, validIds.size());

        // 额外查询验证更新结果
        QueryWrapper<ViewRecord> verifyWrapper = new QueryWrapper<>();
        verifyWrapper.in("id", validIds);
        List<ViewRecord> verifyRecords = this.list(verifyWrapper);
        log.info("验证记录: {} 条，isDelete值: {}", verifyRecords.size(),
                verifyRecords.stream().map(ViewRecord::getIsDelete).collect(Collectors.toList()));

        return result;
    }
}
