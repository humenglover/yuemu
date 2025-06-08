package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.SnakeGameRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SnakeGameMapper extends BaseMapper<SnakeGameRecord> {

    Integer getUserHighestScore(@Param("userId") Long userId, @Param("gameMode") Integer gameMode);

    List<SnakeGameRecord> getRankingList(@Param("gameMode") Integer gameMode, @Param("limit") Integer limit);

    int saveGameRecord(SnakeGameRecord record);
}
