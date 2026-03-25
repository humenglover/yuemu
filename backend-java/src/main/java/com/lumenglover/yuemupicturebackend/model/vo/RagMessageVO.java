package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.RagSessionMessage;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * RAG消息视图对象
 */
@Data
public class RagMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 消息类型 1-用户提问 2-AI回答
     */
    private Integer messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除 0-未删 1-已删
     */
    private Integer isDelete;

    /**
     * 获取消息VO
     *
     * @param ragSessionMessage 消息实体
     * @return 消息VO
     */
    public static RagMessageVO objToVo(RagSessionMessage ragSessionMessage) {
        if (ragSessionMessage == null) {
            return null;
        }
        RagMessageVO ragMessageVO = new RagMessageVO();
        BeanUtils.copyProperties(ragSessionMessage, ragMessageVO);
        return ragMessageVO;
    }

    /**
     * 获取消息实体
     *
     * @param ragMessageVO 消息VO
     * @return 消息实体
     */
    public static RagSessionMessage voToObj(RagMessageVO ragMessageVO) {
        if (ragMessageVO == null) {
            return null;
        }
        RagSessionMessage ragSessionMessage = new RagSessionMessage();
        BeanUtils.copyProperties(ragMessageVO, ragSessionMessage);
        return ragSessionMessage;
    }
}
