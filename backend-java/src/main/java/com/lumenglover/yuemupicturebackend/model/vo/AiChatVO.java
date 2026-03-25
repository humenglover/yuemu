package com.lumenglover.yuemupicturebackend.model.vo;

import lombok.Data;
import java.util.Date;

@Data
public class AiChatVO {
    private String content;
    private String role;
    private Date createTime;
}
