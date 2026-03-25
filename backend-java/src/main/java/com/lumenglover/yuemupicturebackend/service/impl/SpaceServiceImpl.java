package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.space.SpaceQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Activity;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.lumenglover.yuemupicturebackend.model.entity.SpaceUser;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceLevelEnum;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceRoleEnum;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceTypeEnum;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.SpaceService;
import com.lumenglover.yuemupicturebackend.mapper.SpaceMapper;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 鹿梦
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private com.lumenglover.yuemupicturebackend.service.ActivityService activityService;

    private static final Logger log = LoggerFactory.getLogger(SpaceServiceImpl.class);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        // 转换实体类和 DTO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        this.validSpace(space, true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 4. 控制用户创建空间的数量
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断是否已有私有空间
                if (SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType()) {
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue())
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有空间");
                }
                // 判断公共空间数量是否超限
                else if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    long count = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
                            .count();
                    ThrowUtils.throwIf(count >= 10, ErrorCode.OPERATION_ERROR, "每个用户最多只能创建10个团队空间");
                }

                // 创建空间
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");

                // 创建成功后，如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    spaceUser.setStatus(1);  // 设置为已通过状态
                    result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }

                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
        }
        // 修改数据时，空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 校验空间简介长度
        String spaceDesc = space.getSpaceDesc();
        if (StrUtil.isNotBlank(spaceDesc) && spaceDesc.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间简介过长");
        }
        // 校验空间封面图URL格式
        String spaceCover = space.getSpaceCover();
        if (StrUtil.isNotBlank(spaceCover) && !spaceCover.matches("^(http|https)://.*")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间封面图URL格式不正确");
        }
        // 修改数据时，空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 修改数据时，空间类别进行校验
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        // 从MySQL删除
        return super.removeById(id);
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        // 从MySQL批量删除
        return super.removeByIds(idList);
    }

    @Override
    public boolean updateById(Space entity) {
        // 更新MySQL
        return super.updateById(entity);
    }

    @Override
    public boolean save(Space entity) {
        // 保存到MySQL
        return super.save(entity);
    }

    @Override
    public List<Activity> listSpaceActivities(List<Long> spaceIds) {
        if (CollUtil.isEmpty(spaceIds)) {
            return new ArrayList<>();
        }

        // 限制最多查询10个空间的活动
        if (spaceIds.size() > 10) {
            spaceIds = spaceIds.subList(0, 10);
        }

        // 构建查询条件：查询指定空间ID列表中未过期的活动
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("spaceId", spaceIds)
                .eq("isExpired", 0)  // 未过期
                .eq("status", 1)      // 已审核通过
                .eq("isDelete", 0)    // 未删除
                .orderByDesc("createTime");  // 按创建时间倒序

        // 查询活动，限制最多返回10个
        List<Activity> activities = this.activityService.list(queryWrapper.last("LIMIT 10"));

        return activities;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPageWithActivityAndRecommendedUsers(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(space -> {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);

                    // 设置用户信息
                    Long userId = space.getUserId();
                    if (userId != null && userId > 0) {
                        User user = userService.getById(userId);
                        UserVO userVO = userService.getUserVO(user);
                        spaceVO.setUser(userVO);
                    }

                    return spaceVO;
                })
                .collect(Collectors.toList());

        // 获取空间ID列表
        List<Long> spaceIds = spaceVOList.stream()
                .map(SpaceVO::getId)
                .collect(Collectors.toList());

        // 获取这些空间的活动
        List<Activity> activities = this.listSpaceActivities(spaceIds);

        // 将活动按空间ID分组
        Map<Long, List<Activity>> spaceActivityMap = activities.stream()
                .collect(Collectors.groupingBy(Activity::getSpaceId));

        // 获取这些空间的推荐用户
        Map<Long, List<SpaceUserVO>> spaceRecommendedUsersMap = new HashMap<>();
        for (Long spaceId : spaceIds) {
            try {
                List<SpaceUserVO> recommendedUsers = spaceUserService.getRecommendedMembersVO(spaceId);
                spaceRecommendedUsersMap.put(spaceId, recommendedUsers);
            } catch (Exception e) {
                // 如果获取推荐用户失败，返回空列表
                spaceRecommendedUsersMap.put(spaceId, new ArrayList<>());
                log.error("获取空间推荐用户失败，spaceId: " + spaceId, e);
            }
        }

        // 为每个空间VO设置对应的活动和推荐用户
        spaceVOList.forEach(spaceVO -> {
            List<Activity> spaceActivities = spaceActivityMap.get(spaceVO.getId());
            if (spaceActivities != null) {
                // 替换活动中的URL为自定义域名
                spaceActivities.forEach(Activity::replaceUrlWithCustomDomain);
                spaceVO.setActivities(spaceActivities);
            }

            List<SpaceUserVO> recommendedUsers = spaceRecommendedUsersMap.get(spaceVO.getId());
            if (recommendedUsers != null) {
                spaceVO.setRecommendedUsers(recommendedUsers);
            }
        });

        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public List<SpaceVO> listMyTeamSpaceWithActivityAndRecommendedUsers(Long userId) {
        // 查询用户加入的团队空间
        List<SpaceUser> spaceUserList = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getStatus, 1) // 只查询已通过的记录
                .list();

        if (CollUtil.isEmpty(spaceUserList)) {
            return new ArrayList<>();
        }

        // 获取空间ID列表
        List<Long> spaceIds = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toList());

        // 查询空间信息
        List<Space> spaceList = this.lambdaQuery()
                .in(Space::getId, spaceIds)
                .list();

        if (CollUtil.isEmpty(spaceList)) {
            return new ArrayList<>();
        }

        // 转换为VO并填充信息
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(space -> {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);

                    // 设置用户信息
                    Long spaceUserId = space.getUserId();
                    if (spaceUserId != null && spaceUserId > 0) {
                        User user = userService.getById(spaceUserId);
                        UserVO userVO = userService.getUserVO(user);
                        spaceVO.setUser(userVO);
                    }

                    return spaceVO;
                })
                .collect(Collectors.toList());

        // 获取这些空间的活动
        List<Activity> activities = this.listSpaceActivities(spaceIds);

        // 将活动按空间ID分组
        Map<Long, List<Activity>> spaceActivityMap = activities.stream()
                .collect(Collectors.groupingBy(Activity::getSpaceId));

        // 获取这些空间的推荐用户
        Map<Long, List<SpaceUserVO>> spaceRecommendedUsersMap = new HashMap<>();
        for (Long spaceId : spaceIds) {
            try {
                List<SpaceUserVO> recommendedUsers = spaceUserService.getRecommendedMembersVO(spaceId);
                spaceRecommendedUsersMap.put(spaceId, recommendedUsers);
            } catch (Exception e) {
                // 如果获取推荐用户失败，返回空列表
                spaceRecommendedUsersMap.put(spaceId, new ArrayList<>());
                log.error("获取空间推荐用户失败，spaceId: " + spaceId, e);
            }
        }

        // 为每个空间VO设置对应的活动和推荐用户
        spaceVOList.forEach(spaceVO -> {
            List<Activity> spaceActivities = spaceActivityMap.get(spaceVO.getId());
            if (spaceActivities != null) {
                // 替换活动中的URL为自定义域名
                spaceActivities.forEach(Activity::replaceUrlWithCustomDomain);
                spaceVO.setActivities(spaceActivities);
            }

            List<SpaceUserVO> recommendedUsers = spaceRecommendedUsersMap.get(spaceVO.getId());
            if (recommendedUsers != null) {
                spaceVO.setRecommendedUsers(recommendedUsers);
            }
        });

        return spaceVOList;
    }

    @Override
    public boolean setRecommendStatus(Long spaceId, Integer recommendStatus, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceId == null || recommendStatus == null, ErrorCode.PARAMS_ERROR);
        // 校验推荐状态值
        if (!recommendStatus.equals(0) && !recommendStatus.equals(1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "推荐状态值错误");
        }

        // 校验空间是否存在
        Space oldSpace = this.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 校验权限 - 只有管理员才能设置推荐状态
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员有权限设置推荐状态");
        }

        // 校验空间类型 - 只有团队空间（spaceType=1）可以被推荐
        if (oldSpace.getSpaceType() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只有团队空间可以被推荐");
        }

        // 更新推荐状态
        Space updateSpace = new Space();
        updateSpace.setId(spaceId);
        updateSpace.setIsRecommended(recommendStatus);
        return this.updateById(updateSpace);
    }

    @Override
    public List<SpaceVO> listRecommendedSpaces() {
        // 查询所有推荐状态为1（推荐）的空间
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isRecommended", 1);
        queryWrapper.eq("isDelete", 0); // 只查询未删除的空间
        List<Space> spaceList = this.list(queryWrapper);

        if (CollUtil.isEmpty(spaceList)) {
            return new ArrayList<>();
        }

        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(space -> {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);

                    // 设置用户信息
                    Long userId = space.getUserId();
                    if (userId != null && userId > 0) {
                        User user = userService.getById(userId);
                        UserVO userVO = userService.getUserVO(user);
                        spaceVO.setUser(userVO);
                    }

                    return spaceVO;
                })
                .collect(Collectors.toList());

        // 获取空间ID列表
        List<Long> spaceIds = spaceVOList.stream()
                .map(SpaceVO::getId)
                .collect(Collectors.toList());

        // 获取这些空间的活动
        List<Activity> activities = this.listSpaceActivities(spaceIds);

        // 将活动按空间ID分组
        Map<Long, List<Activity>> spaceActivityMap = activities.stream()
                .collect(Collectors.groupingBy(Activity::getSpaceId));

        // 获取这些空间的推荐用户
        Map<Long, List<SpaceUserVO>> spaceRecommendedUsersMap = new HashMap<>();
        for (Long spaceId : spaceIds) {
            try {
                List<SpaceUserVO> recommendedUsers = spaceUserService.getRecommendedMembersVO(spaceId);
                spaceRecommendedUsersMap.put(spaceId, recommendedUsers);
            } catch (Exception e) {
                // 如果获取推荐用户失败，返回空列表
                spaceRecommendedUsersMap.put(spaceId, new ArrayList<>());
                log.error("获取空间推荐用户失败，spaceId: " + spaceId, e);
            }
        }

        // 为每个空间VO设置对应的活动和推荐用户
        spaceVOList.forEach(spaceVO -> {
            List<Activity> spaceActivities = spaceActivityMap.get(spaceVO.getId());
            if (spaceActivities != null) {
                // 替换活动中的URL为自定义域名
                spaceActivities.forEach(Activity::replaceUrlWithCustomDomain);
                spaceVO.setActivities(spaceActivities);
            }

            List<SpaceUserVO> recommendedUsers = spaceRecommendedUsersMap.get(spaceVO.getId());
            if (recommendedUsers != null) {
                spaceVO.setRecommendedUsers(recommendedUsers);
            }
        });

        return spaceVOList;
    }
}
