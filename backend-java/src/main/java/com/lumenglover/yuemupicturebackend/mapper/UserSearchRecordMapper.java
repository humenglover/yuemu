package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.UserSearchRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserSearchRecordMapper extends BaseMapper<UserSearchRecord> {
    /**
     * 获取用户最近的搜索记录
     */
    List<UserSearchRecord> listRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取用户指定类型的最近搜索记录
     */
    List<UserSearchRecord> listRecentByUserAndType(@Param("userId") Long userId, @Param("type") String type, @Param("limit") int limit);

    /**
     * 插入搜索记录
     */
    void insertRecord(@Param("record") UserSearchRecord record);

    /**
     * 批量插入搜索记录
     */
    void batchInsertRecords(@Param("records") List<UserSearchRecord> records);

    /**
     * 删除用户指定类型的搜索历史记录
     */
    void deleteUserSearchHistoryByType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 删除用户所有搜索历史记录
     */
    void deleteUserSearchHistory(@Param("userId") Long userId);
}
