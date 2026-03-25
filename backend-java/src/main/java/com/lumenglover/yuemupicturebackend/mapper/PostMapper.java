package com.lumenglover.yuemupicturebackend.mapper;

import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;

/**
 * @author 鹿梦
 * @description 针对表【post(论坛帖子表)】的数据库操作Mapper
 * @createDate 2025-02-05 11:08:49
 * @Entity generator.domain.Post
 */
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 查询已删除的帖子（绕过逻辑删除）
     * @param updateTime 更新时间
     * @return 已删除的帖子列表
     */
    List<Post> selectDeletedPostsByUpdateTime(Date updateTime);

    /**
     * 查询所有已删除的帖子（绕过逻辑删除）
     * @return 已删除的帖子列表
     */
    List<Post> selectAllDeletedPosts();

    /**
     * 根据ID列表查询有效的帖子（status=1, isDelete=0）
     * @param postIds 帖子ID列表
     * @return 有效帖子列表
     */
    List<Post> selectValidPostsByIds(@Param("postIds") List<Long> postIds);

    /**
     * 查询热门帖子
     * @param limit 限制数量
     * @return 热门帖子ID列表
     */
    List<Long> selectHotPosts(@Param("limit") int limit);

    /**
     * 查询用户的草稿列表
     * @param userId 用户ID
     * @return 草稿帖子列表
     */
    List<Post> selectDraftPostsByUserId(@Param("userId") Long userId);
}




