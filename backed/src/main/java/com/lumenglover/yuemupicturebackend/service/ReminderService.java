package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Reminder;
import com.lumenglover.yuemupicturebackend.model.vo.ReminderVO;


public interface ReminderService extends IService<Reminder> {

    /**
     * 添加提醒事项
     */
    long addReminder(ReminderAddRequest reminderAddRequest, Long userId);

    /**
     * 分页获取提醒事项
     */
    Page<ReminderVO> getReminderVOPage(ReminderQueryRequest reminderQueryRequest, Long userId);

    /**
     * 切换提醒事项完成状态
     */
    boolean toggleReminder(Long id, Long userId);

    /**
     * 切换收藏状态
     */
    boolean toggleStarred(Long id, Long userId);

    /**
     * 切换重要状态
     */
    boolean toggleImportant(Long id, Long userId);

    /**
     * 删除提醒事项
     */
    boolean deleteReminder(Long id, Long userId);
}
