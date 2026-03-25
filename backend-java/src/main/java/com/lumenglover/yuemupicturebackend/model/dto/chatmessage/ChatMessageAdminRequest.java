package com.lumenglover.yuemupicturebackend.model.dto.chatmessage;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理员查询聊天消息请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessageAdminRequest extends PageRequest implements Serializable {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（1-私聊消息，2-图片评论，3-空间消息）
     */
    private Integer type;

    /**
     * 消息状态（0-未读，1-已读）
     */
    private Integer status;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 私聊ID
     */
    private Long privateChatId;

    private static final long serialVersionUID = 1L;
} 