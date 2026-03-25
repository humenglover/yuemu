package com.lumenglover.yuemupicturebackend.model.dto.systemnotify;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统通知查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SystemNotifyQueryRequest extends PageRequest implements Serializable {

    /**
     * 通知类型：ADMIN_ANNOUNCE(管理员公告)/POST_SELECTED(帖子精选)/POST_DELETED(帖子删除)/POST_UPDATED(帖子修改)/ACCOUNT_CHANGED(账号变更)/SYSTEM_ALERT(系统告警)
     */
    private String notifyType;

    /**
     * 接收者类型：ALL_USER(全体用户)/SPECIFIC_USER(指定用户)/ROLE(按角色)
     */
    private String receiverType;

    /**
     * 是否有效[0:无效（如误发通知）, 1:有效]
     */
    private Integer isEnabled;

    /**
     * 阅读状态[0:未读, 1:已读]
     */
    private Integer readStatus;

    private static final long serialVersionUID = 1L;
}