package com.lumenglover.yuemupicturebackend.model.dto.privatechat;

import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 私聊查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PrivateChatQueryRequest extends PageRequest implements Serializable {

    /**
     * 目标用户id
     */
    private Long targetUserId;

    /**
     * 聊天类型：0-私信 1-好友
     */
    private Integer chatType;

    /**
     * 搜索词（搜索最后一条消息内容）
     */
    private String searchText;

    private static final long serialVersionUID = 1L;
}
