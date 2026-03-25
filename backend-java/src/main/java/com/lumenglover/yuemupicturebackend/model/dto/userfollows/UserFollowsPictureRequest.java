package com.lumenglover.yuemupicturebackend.model.dto.userfollows;


import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserFollowsPictureRequest extends PageRequest implements Serializable {
    /**
     * 是否只查询 spaceId 为 null 的数据
     */
    private boolean nullSpaceId;
    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
