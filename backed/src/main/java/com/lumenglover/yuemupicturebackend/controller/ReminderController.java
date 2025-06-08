package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.reminder.ReminderQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.ReminderVO;
import com.lumenglover.yuemupicturebackend.service.ReminderService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * 提醒事项控制器
 * 提供添加、查询、切换状态和删除提醒事项的功能
 *
 * @author lumenglover
 */
@RestController
@RequestMapping("/reminder")
@Slf4j
@Validated
public class ReminderController {

    @Resource
    private ReminderService reminderService;

    @Resource
    private UserService userService;

    /**
     * 添加提醒事项
     * 为当前登录用户创建一个新的提醒事项
     *
     * @param reminderAddRequest 提醒事项创建请求，包含内容和提醒时间
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 新创建的提醒事项ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addReminder(@RequestBody @Validated ReminderAddRequest reminderAddRequest,
                                          HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        long reminderId = reminderService.addReminder(reminderAddRequest, userId);
        return ResultUtils.success(reminderId);
    }

    /**
     * 分页获取提醒事项
     * 支持按日期、完成状态、收藏状态、重要状态筛选
     * 支持自定义排序，默认按重要程度和提醒时间排序
     *
     * @param reminderQueryRequest 查询参数
     * @param request HTTP请求对象
     * @return 分页的提醒事项列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ReminderVO>> listReminderByPage(@RequestBody ReminderQueryRequest reminderQueryRequest,
                                                             HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        Page<ReminderVO> reminderVOPage = reminderService.getReminderVOPage(reminderQueryRequest, userId);
        return ResultUtils.success(reminderVOPage);
    }

    /**
     * 获取今日提醒事项
     * 支持分页和自定义排序，默认按重要程度和提醒时间排序
     *
     * @param reminderQueryRequest 查询参数
     * @param request HTTP请求对象
     * @return 今日提醒事项列表
     */
    @PostMapping("/today")
    public BaseResponse<Page<ReminderVO>> getTodayReminders(@RequestBody ReminderQueryRequest reminderQueryRequest,
                                                            HttpServletRequest request) {
        reminderQueryRequest.setDate(LocalDate.now());
        Long userId = userService.getLoginUser(request).getId();
        Page<ReminderVO> reminderVOPage = reminderService.getReminderVOPage(reminderQueryRequest, userId);
        return ResultUtils.success(reminderVOPage);
    }

    /**
     * 切换提醒事项的完成状态
     *
     * @param id 提醒事项ID
     * @param request HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/toggle/{id}")
    public BaseResponse<Boolean> toggleReminder(@PathVariable("id") Long id,
                                                HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        boolean result = reminderService.toggleReminder(id, userId);
        return ResultUtils.success(result);
    }

    /**
     * 切换提醒事项的收藏状态
     *
     * @param id 提醒事项ID
     * @param request HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/star/{id}")
    public BaseResponse<Boolean> toggleStarred(@PathVariable("id") Long id,
                                               HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        boolean result = reminderService.toggleStarred(id, userId);
        return ResultUtils.success(result);
    }

    /**
     * 切换提醒事项的重要状态
     *
     * @param id 提醒事项ID
     * @param request HTTP请求对象
     * @return 操作是否成功
     */
    @PostMapping("/important/{id}")
    public BaseResponse<Boolean> toggleImportant(@PathVariable("id") Long id,
                                                 HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        boolean result = reminderService.toggleImportant(id, userId);
        return ResultUtils.success(result);
    }

    /**
     * 删除提醒事项
     *
     * @param id 提醒事项ID
     * @param request HTTP请求对象
     * @return 删除操作是否成功
     */
    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteReminder(@PathVariable("id") Long id,
                                                HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        boolean result = reminderService.deleteReminder(id, userId);
        return ResultUtils.success(result);
    }
}
