package com.lumenglover.yuemupicturebackend.manager.websocket.disruptor;

import com.lumenglover.yuemupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
