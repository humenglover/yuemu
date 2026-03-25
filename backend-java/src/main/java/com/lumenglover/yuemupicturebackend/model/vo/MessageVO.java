package com.lumenglover.yuemupicturebackend.model.vo;

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
public class MessageVO implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 留言内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
