package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 聊天列表事件
 */
@Data
public class ChatListEvent {
    /**
     * 事件类型
     * UPDATE - 更新聊天
     * DELETE - 删除聊天
     * NEW - 新建聊天
     */
    private String type;

    /**
     * 聊天信息
     */
    private PrivateChat privateChat;

    /**
     * 当前用户的session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 清空事件数据
     */
    public void clear() {
        this.type = null;
        this.privateChat = null;
        this.session = null;
        this.user = null;
        this.targetUserId = null;
    }
}
