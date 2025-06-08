package com.lumenglover.yuemupicturebackend.model.dto.reminder;


import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 提醒事项查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReminderQueryRequest extends PageRequest implements Serializable {

    /**
     * 查询日期
     */
    private LocalDate date;

    /**
     * 是否只看已完成
     */
    private Boolean completed;

    /**
     * 是否只看收藏
     */
    private Boolean starred;

    /**
     * 是否只看重要
     */
    private Boolean important;

    private static final long serialVersionUID = 1L;
}
