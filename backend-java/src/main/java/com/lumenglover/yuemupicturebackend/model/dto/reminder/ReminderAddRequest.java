package com.lumenglover.yuemupicturebackend.model.dto.reminder;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 提醒事项创建请求
 */
@Data
public class ReminderAddRequest implements Serializable {

    /**
     * 提醒内容
     */
    @NotBlank(message = "提醒内容不能为空")
    private String content;

    private static final long serialVersionUID = 1L;
}
