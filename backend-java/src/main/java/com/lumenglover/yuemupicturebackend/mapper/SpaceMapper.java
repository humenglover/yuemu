package com.lumenglover.yuemupicturebackend.mapper;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author 鹿梦
 * @description 针对表【space(空间)】的数据库操作Mapper
 * @createDate 2024-12-18 19:53:34
 * @Entity com.lumenglover.yuemupicturebackend.model.entity.Space
 */
import java.util.List;
import java.util.Date;

public interface SpaceMapper extends BaseMapper<Space> {

    /**
     * 查询已删除的空间（绕过逻辑删除）
     * @param updateTime 更新时间
     * @return 已删除的空间列表
     */
    List<Space> selectDeletedSpacesByUpdateTime(Date updateTime);

    /**
     * 查询所有已删除的空间（绕过逻辑删除）
     * @return 已删除的空间列表
     */
    List<Space> selectAllDeletedSpaces();
}




