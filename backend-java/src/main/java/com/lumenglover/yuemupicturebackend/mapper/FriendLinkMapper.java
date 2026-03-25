package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.FriendLink;
import org.apache.ibatis.annotations.Param;

/**
 * 友情链接 Mapper
 */
public interface FriendLinkMapper extends BaseMapper<FriendLink> {

    /**
     * 增加浏览量
     * @param id 友链id
     * @return 影响行数
     */
    int increaseViewCount(@Param("id") Long id);

    /**
     * 增加点击量
     * @param id 友链id
     * @return 影响行数
     */
    int increaseClickCount(@Param("id") Long id);
}
