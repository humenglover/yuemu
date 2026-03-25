package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAuditRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.lumenglover.yuemupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserJoinRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 鹿梦
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-01-02 20:07:15
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add       是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    boolean isSpaceMember(long userId, long spaceId);

    List<User> getSpaceMembers(long spaceId);

    /**
     * 审核空间成员申请
     * @param spaceUserAuditRequest 审核请求
     * @param loginUser 当前登录用户
     * @return 是否审核成功
     */
    boolean auditSpaceUser(SpaceUserAuditRequest spaceUserAuditRequest, User loginUser);

    /**
     * 申请加入空间
     * @param spaceUserJoinRequest 申请请求
     * @param loginUser 当前登录用户
     * @return 是否申请成功
     */
    boolean joinSpace(SpaceUserJoinRequest spaceUserJoinRequest, User loginUser);

    /**
     * 退出空间
     *
     * @param spaceId
     * @param loginUser
     * @return
     */
    boolean quitSpace(long spaceId, User loginUser);

    /**
     * 检查用户是否是空间的管理员
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @return 是否是空间管理员
     */
    boolean checkSpaceAdmin(Long spaceId, Long userId);

    /**
     * 设置空间推荐成员
     *
     * @param spaceId       空间ID
     * @param userId        用户ID
     * @param isRecommended 是否推荐
     * @param loginUser     当前登录用户
     * @return 是否设置成功
     */
    boolean setRecommendedMember(Long spaceId, Long userId, Integer isRecommended, User loginUser);

    /**
     * 获取空间推荐成员列表
     *
     * @param spaceId 空间ID
     * @return 推荐成员列表
     */
    List<SpaceUser> getRecommendedMembers(Long spaceId);

    /**
     * 获取空间推荐成员列表（封装类）
     *
     * @param spaceId 空间ID
     * @return 推荐成员列表（封装类）
     */
    List<SpaceUserVO> getRecommendedMembersVO(Long spaceId);

    /**
     * 检查用户是否已有未审核的申请
     *
     * @param userId  用户ID
     * @param spaceId 空间ID
     * @return SpaceUser对象，如果不存在未审核申请则返回null
     */
    SpaceUser checkPendingApplication(Long userId, Long spaceId);
}
