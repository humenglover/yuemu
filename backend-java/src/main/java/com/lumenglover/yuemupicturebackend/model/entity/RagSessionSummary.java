package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能客服会话摘要表
 */
@TableName(value ="rag_session_summary")
@Data
public class RagSessionSummary implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 摘要内容
     */
    private String content;

    /**
     * 最后一次总结的消息ID (水位线)
     */
    private Long lastMessageId;

    /**
     * 摘要层级：0-基础摘要(10条)，1-超级摘要(100条)
     */
    private Integer summaryLevel;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
