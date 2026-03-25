package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统通知
 * @TableName t_system_notify
 */
@TableName(value = "t_system_notify")
@Data
public class SystemNotify implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 操作人ID：管理员操作填管理员ID/系统自动操作填"system"/NULL=无
     */
    private String operatorId;

    /**
     * 操作人类型：ADMIN(管理员)/SYSTEM(系统)/NULL=无
     */
    private String operatorType;

    /**
     * 通知类型：ADMIN_ANNOUNCE(管理员公告)/POST_SELECTED(帖子精选)/POST_DELETED(帖子删除)/POST_UPDATED(帖子修改)/ACCOUNT_CHANGED(账号变更)/SYSTEM_ALERT(系统告警)
     */
    private String notifyType;

    /**
     * 发送者类型：ADMIN(管理员)/SYSTEM(系统)
     */
    private String senderType;

    /**
     * 发送者ID：ADMIN=管理员用户ID/SYSTEM=固定值"system"
     */
    private String senderId;

    /**
     * 接收者类型：ALL_USER(全体用户)/SPECIFIC_USER(指定用户)/ROLE(按角色)
     */
    private String receiverType;

    /**
     * 接收者ID：ALL_USER=NULL/SPECIFIC_USER=用户ID/ROLE=角色编码（如USER/VIP/ADMIN）
     */
    private String receiverId;

    /**
     * 通知标题（如：系统公告、您的帖子已精选）
     */
    private String title;

    /**
     * 通知详情（支持富文本）
     */
    private String content;

    /**
     * 通知图标标识（用于前端差异化展示，如：announce/selected/alert）
     */
    private String notifyIcon;

    /**
     * 关联业务类型：POST(帖子)/ACCOUNT(账号)/COMMENT(评论)/NULL(无关联)
     */
    private String relatedBizType;

    /**
     * 关联业务ID：帖子ID/账号ID/评论ID（用于前端跳转至对应页面）
     */
    private String relatedBizId;

    /**
     * 阅读状态[0:未读, 1:已读]
     */
    private Integer readStatus;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 是否全局通知[0:否, 1:是（全员可见，如系统公告）]
     */
    private Integer isGlobal;

    /**
     * 通知过期时间（NULL=永久有效）
     */
    private Date expireTime;

    /**
     * 是否有效[0:无效（如误发通知）, 1:有效]
     */
    private Integer isEnabled;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
