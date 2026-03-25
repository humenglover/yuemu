package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lumenglover.yuemupicturebackend.utils.VoUrlReplaceUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long senderId;

    private Long receiverId;

    private Long pictureId;

    private String content;

    private Integer type;

    private Integer status;

    /**
     * 回复的消息id
     */
    private Long replyId;

    /**
     * 会话根消息id
     */
    private Long rootId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    /**
     * 回复的消息内容（非数据库字段）
     */
    @TableField(exist = false)
    private ChatMessage replyMessage;

    /**
     * 发送者信息（非数据库字段）
     */
    @TableField(exist = false)
    private User sender;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 私聊ID
     */
    private Long privateChatId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息资源地址
     */
    private String messageUrl;

    /**
     * 消息资源大小
     */
    private Long messageSize;

    /**
     * 消息发送位置
     */
    private String messageLocation;

    private static final long serialVersionUID = 1L;

    /**
     * 替换URL为自定义域名
     */
    public void replaceUrlWithCustomDomain() {
        this.messageUrl = VoUrlReplaceUtil.replaceUrl(this.messageUrl);
    }
}
