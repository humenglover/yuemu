package com.lumenglover.yuemupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.StringTypeHandler;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("aichat")
public class AiChat implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField(value = "content", typeHandler = StringTypeHandler.class)
    private String content;

    @TableField("role")
    private String role = "user";  // 默认为用户角色

    @TableField("createTime")
    private Date createTime;

    @TableField("isDeleted")
    @TableLogic
    private Integer isDeleted = 0;

    @TableField("sessionId")
    private Long sessionId = 0L;

    private static final long serialVersionUID = 1L;

    public AiChat() {
        // 默认构造函数
    }

    public AiChat(Long userId, String content, String role, Date createTime) {
        this.userId = userId;
        this.content = content;
        this.role = role;
        this.createTime = createTime;
        this.isDeleted = 0;
        this.sessionId = 0L;
    }
}
