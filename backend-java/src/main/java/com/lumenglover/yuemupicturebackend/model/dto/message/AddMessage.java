package com.lumenglover.yuemupicturebackend.model.dto.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 留言板表
 * @TableName message
 */
@Data
public class AddMessage implements Serializable {

    /**
     * 留言内容
     */
    private String content;

    /**
     * IP地址
     */
    private String ip;

    private static final long serialVersionUID = 1L;
}
