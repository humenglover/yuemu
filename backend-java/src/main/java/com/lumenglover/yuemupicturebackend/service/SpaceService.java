package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lumeng
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2024-12-18 19:53:34
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space
     * @param add   是否为创建时检验
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 获取指定空间的未过期活动列表（最多前10个）
     *
     * @param spaceIds 空间ID列表
     * @return
     */
    List<Activity> listSpaceActivities(List<Long> spaceIds);

    /**
     * 设置空间推荐状态
     *
     * @param spaceId 空间ID
     * @param recommendStatus 推荐状态：0-取消推荐 1-推荐
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean setRecommendStatus(Long spaceId, Integer recommendStatus, User loginUser);

    /**
     * 获取推荐空间列表
     *
     * @return 推荐空间列表
     */
    List<SpaceVO> listRecommendedSpaces();

    /**
     * 获取带有活动和推荐用户的空间VO分页结果
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPageWithActivityAndRecommendedUsers(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取指定用户加入的团队空间列表（包含推荐成员和活动）
     *
     * @param userId 用户ID
     * @return
     */
    List<SpaceVO> listMyTeamSpaceWithActivityAndRecommendedUsers(Long userId);
}
