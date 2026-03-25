package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.HotSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Date;

public interface HotSearchMapper extends BaseMapper<HotSearch> {

    /**
     * 批量插入或更新热门搜索
     */
    void batchInsertOrUpdate(@Param("list") List<HotSearch> hotSearchList);

    /**
     * 获取指定时间之后的热门搜索
     */
    List<HotSearch> getHotSearchAfter(@Param("type") String type,
                                     @Param("startTime") Date startTime,
                                     @Param("limit") Integer limit);
}
