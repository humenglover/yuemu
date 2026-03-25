package com.lumenglover.yuemupicturebackend.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
public class CategoryVO implements Serializable {
    /**
     * 分类id
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类类型：0-图片分类 1-帖子分类
     */
    private Integer type;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 分类编辑时间
     */
    private Date editTime;

    /**
     * 分类更新时间
     */
    private Date updateTime;

}
