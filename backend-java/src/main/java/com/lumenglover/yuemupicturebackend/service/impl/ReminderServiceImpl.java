package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.ReminderMapper;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Reminder;
import com.lumenglover.yuemupicturebackend.model.vo.ReminderVO;
import com.lumenglover.yuemupicturebackend.service.ReminderService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder>
        implements ReminderService {

    @Override
    public long addReminder(ReminderAddRequest reminderAddRequest, Long userId) {
        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminder.setContent(reminderAddRequest.getContent());
        reminder.setRemindTime(LocalTime.now());
        reminder.setCompleted(0);
        reminder.setIsStarred(0);
        reminder.setIsImportant(0);

        boolean success = this.save(reminder);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加失败");
        }
        return reminder.getId();
    }

    @Override
    public Page<ReminderVO> getReminderVOPage(ReminderQueryRequest reminderQueryRequest, Long userId) {
        long current = reminderQueryRequest.getCurrent();
        long pageSize = reminderQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

        Page<Reminder> reminderPage = new Page<>(current, pageSize);
        QueryWrapper<Reminder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);

        // 根据日期筛选
        if (reminderQueryRequest.getDate() != null) {
            queryWrapper.apply("DATE(createTime) = {0}", reminderQueryRequest.getDate());
        }

        // 根据完成状态筛选
        if (reminderQueryRequest.getCompleted() != null) {
            queryWrapper.eq("completed", reminderQueryRequest.getCompleted() ? 1 : 0);
        }

        // 根据收藏状态筛选
        if (reminderQueryRequest.getStarred() != null) {
            queryWrapper.eq("isStarred", reminderQueryRequest.getStarred() ? 1 : 0);
        }

        // 根据重要状态筛选
        if (reminderQueryRequest.getImportant() != null) {
            queryWrapper.eq("isImportant", reminderQueryRequest.getImportant() ? 1 : 0);
        }

        // 处理排序
        String sortField = reminderQueryRequest.getSortField();
        String sortOrder = reminderQueryRequest.getSortOrder();

        if (sortField != null) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(true, isAsc, sortField);
        } else {
            // 默认排序：重要的在前，然后按提醒时间升序
            queryWrapper.orderByDesc("isImportant")
                    .orderByAsc("remindTime");
        }

        // 执行查询
        this.page(reminderPage, queryWrapper);

        // 转换为VO
        Page<ReminderVO> reminderVOPage = new Page<>(current, pageSize, reminderPage.getTotal());
        List<ReminderVO> reminderVOList = reminderPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        reminderVOPage.setRecords(reminderVOList);

        return reminderVOPage;
    }

    @Override
    public boolean toggleReminder(Long id, Long userId) {
        Reminder reminder = this.getById(id);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        reminder.setCompleted(reminder.getCompleted() == 0 ? 1 : 0);
        return this.updateById(reminder);
    }

    @Override
    public boolean toggleStarred(Long id, Long userId) {
        Reminder reminder = this.getById(id);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        reminder.setIsStarred(reminder.getIsStarred() == 0 ? 1 : 0);
        return this.updateById(reminder);
    }

    @Override
    public boolean toggleImportant(Long id, Long userId) {
        Reminder reminder = this.getById(id);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        reminder.setIsImportant(reminder.getIsImportant() == 0 ? 1 : 0);
        return this.updateById(reminder);
    }

    @Override
    public boolean deleteReminder(Long id, Long userId) {
        QueryWrapper<Reminder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("userId", userId);
        return this.remove(queryWrapper);
    }

    private ReminderVO convertToVO(Reminder reminder) {
        ReminderVO vo = new ReminderVO();
        vo.setId(reminder.getId());
        vo.setContent(reminder.getContent());
        vo.setTime(reminder.getRemindTime().toString());
        vo.setCompleted(reminder.getCompleted() == 1);
        vo.setStarred(reminder.getIsStarred() == 1);
        vo.setImportant(reminder.getIsImportant() == 1);
        return vo;
    }
}
