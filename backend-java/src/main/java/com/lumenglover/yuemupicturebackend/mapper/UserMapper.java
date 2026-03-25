package com.lumenglover.yuemupicturebackend.mapper;

import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * @author 鹿梦
 * @description 针对表【user(用户)】的数据库操作Mapper
 * @createDate 2024-12-10 10:39:52
 * @Entity generator.domain.User
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("select count(*) from user where userAccount = #{userAccount}")
    long selectByAccount(String userAccount);

    /**
     * 查询已删除的用户（绕过逻辑删除）
     * @param updateTime 更新时间
     * @return 已删除的用户列表
     */
    List<User> selectDeletedUsersByUpdateTime(Date updateTime);

    /**
     * 查询所有已删除的用户（绕过逻辑删除）
     * @return 已删除的用户列表
     */
    List<User> selectAllDeletedUsers();

    /**
     * 更新用户多设备登录设置
     * @param userId 用户ID
     * @param allowMultiDeviceLogin 是否允许多设备登录：1-允许、0-禁止
     * @return 更新记录数
     */
    int updateAllowMultiDeviceLogin(Long userId, Integer allowMultiDeviceLogin);

    /**
     * 获取用户多设备登录设置
     * @param userId 用户ID
     * @return allowMultiDeviceLogin值
     */
    Integer getAllowMultiDeviceLogin(Long userId);
}




