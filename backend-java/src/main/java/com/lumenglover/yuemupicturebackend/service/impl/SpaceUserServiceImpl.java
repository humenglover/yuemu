package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserAuditRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserJoinRequest;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.lumenglover.yuemupicturebackend.model.entity.SpaceUser;
import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceRoleEnum;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SpaceVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.SpaceService;
import com.lumenglover.yuemupicturebackend.service.SpaceUserService;
import com.lumenglover.yuemupicturebackend.mapper.SpaceUserMapper;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 鹿梦
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-01-02 20:07:15
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private SystemNotifyService systemNotifyService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        // 设置初始状态为通过，因为只有管理员可以使用添加方法
        spaceUser.setStatus(1);
        validSpaceUser(spaceUser, true);

        // 校验空间成员数量是否已达到上限
        long memberCount = this.count(new QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceUserAddRequest.getSpaceId())
                .eq("status", 1));  // 只统计已通过的成员
        ThrowUtils.throwIf(memberCount >= 50, ErrorCode.OPERATION_ERROR, "该空间成员数量已达到上限");

        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        // 设置状态
        spaceUserVO.setStatus(spaceUser.getStatus());
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        Integer status = spaceUserQueryRequest.getStatus();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        return queryWrapper;
    }

    @Override
    public boolean isSpaceMember(long userId, long spaceId) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("spaceId", spaceId)
                .eq("status", 1);  // 只有审核通过的才算是成员
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<User> getSpaceMembers(long spaceId) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId)
                .eq("status", 1);  // 只获取审核通过的成员
        List<SpaceUser> spaceUsers = this.list(queryWrapper);

        if (CollUtil.isEmpty(spaceUsers)) {
            return Collections.emptyList();
        }

        // 2. 获取所有用户ID
        Set<Long> userIds = spaceUsers.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());

        // 3. 批量查询用户信息并脱敏
        List<User> users = userService.listByIds(userIds);

        // 4. 对用户信息进行脱敏处理，保留更多字段
        return users.stream()
                .map(user -> {
                    User safetyUser = new User();
                    safetyUser.setId(user.getId());
                    safetyUser.setUserAccount(user.getUserAccount());
                    safetyUser.setUserName(user.getUserName());
                    safetyUser.setUserAvatar(user.getUserAvatar());
                    safetyUser.setUserProfile(user.getUserProfile());
                    safetyUser.setUserRole(user.getUserRole());
                    safetyUser.setCreateTime(user.getCreateTime());
                    return safetyUser;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean auditSpaceUser(SpaceUserAuditRequest spaceUserAuditRequest, User loginUser) {
        if (spaceUserAuditRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验参数
        Long spaceId = spaceUserAuditRequest.getSpaceId();
        Long userId = spaceUserAuditRequest.getUserId();
        Integer status = spaceUserAuditRequest.getStatus();

        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId, status),
                ErrorCode.PARAMS_ERROR);

        // 校验状态值
        if (status != 1 && status != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不合法");
        }

        // 校验当前用户是否是该空间的管理员
        QueryWrapper<SpaceUser> adminQuery = new QueryWrapper<>();
        adminQuery.eq("spaceId", spaceId)
                .eq("userId", loginUser.getId())
                .eq("spaceRole", "admin");
        SpaceUser adminUser = this.getOne(adminQuery);
        ThrowUtils.throwIf(adminUser == null, ErrorCode.NO_AUTH_ERROR, "您不是该空间的管理员");

        // 校验被审核用户是否存在申请记录
        QueryWrapper<SpaceUser> userQuery = new QueryWrapper<>();
        userQuery.eq("spaceId", spaceId)
                .eq("userId", userId);
        SpaceUser targetUser = this.getOne(userQuery);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到该用户的申请记录");

        // 校验被审核用户不是管理员
        ThrowUtils.throwIf("admin".equals(targetUser.getSpaceRole()),
                ErrorCode.OPERATION_ERROR, "不能审核管理员");

        // 如果是通过申请，需要检查成员数量
        if (status == 1) {
            long memberCount = this.count(new QueryWrapper<SpaceUser>()
                    .eq("spaceId", spaceId)
                    .eq("status", 1));  // 只统计已通过的成员
            ThrowUtils.throwIf(memberCount >= 50, ErrorCode.OPERATION_ERROR, "该空间成员数量已达到上限");
        }

        // 更新审核状态,保持原有角色不变
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setId(targetUser.getId());
        spaceUser.setStatus(status);

        boolean result = this.updateById(spaceUser);

        // 如果审核通过，发送通知给申请人
        if (result && status == 1) {
            // 获取审核人信息
            User auditor = userService.getById(loginUser.getId());
            // 获取申请人信息
            User applicantUser = userService.getById(userId);
            // 获取空间信息
            Space targetSpace = spaceService.getById(spaceId);

            // 发送通知给申请人
            String title = "空间加入申请已通过";
            String content = String.format("您申请加入空间 '%s' 的请求已通过，审核人为 %s（ID: %s）。欢迎加入该空间！",
                    targetSpace.getSpaceName(), auditor.getUserName(), auditor.getId());
            String notifyType = "SPACE_JOIN_APPROVED";
            sendNotificationToUser(userId, title, content, notifyType);
        }

        return result;
    }

    @Override
    public boolean joinSpace(SpaceUserJoinRequest spaceUserJoinRequest, User loginUser) {
        if (spaceUserJoinRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long spaceId = spaceUserJoinRequest.getSpaceId();
        Long userId = loginUser.getId();

        // 校验空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 校验空间成员数量是否已达到上限
        long memberCount = this.count(new QueryWrapper<SpaceUser>()
                .eq("spaceId", spaceId)
                .eq("status", 1));  // 只统计已通过的成员
        ThrowUtils.throwIf(memberCount >= 50, ErrorCode.OPERATION_ERROR, "该空间成员数量已达到上限");

        // 校验用户是否已经有记录
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId)
                .eq("userId", userId);
        SpaceUser existSpaceUser = this.getOne(queryWrapper);

        if (existSpaceUser != null) {
            // 如果是已通过状态
            if (existSpaceUser.getStatus() == 1) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已是该空间成员");
            }
            // 如果是待审核状态
            if (existSpaceUser.getStatus() == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的申请正在审核中");
            }
            // 如果是已退出或被拒绝状态（status == 2），则重置为待审核状态
            existSpaceUser.setStatus(0);
            existSpaceUser.setSpaceRole("viewer");  // 重置为查看者角色
            return this.updateById(existSpaceUser);
        }

        // 创建新的申请记录
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(userId);
        spaceUser.setStatus(0);  // 设置为待审核状态
        spaceUser.setSpaceRole("viewer");  // 默认设置为查看者角色

        boolean result = this.save(spaceUser);

        // 如果申请成功，发送通知给空间所有管理员
        if (result) {
            // 获取申请人信息
            User applicant = userService.getById(userId);

            // 发送通知给空间所有管理员
            String title = "空间加入申请";
            String content = String.format("用户 %s（ID: %s）申请加入您的空间 '%s'，请尽快审核。",
                    applicant.getUserName(), applicant.getId(), space.getSpaceName());
            String notifyType = "SPACE_JOIN_REQUEST";
            sendNotificationToSpaceAdmins(spaceId, title, content, notifyType);
        }

        return result;
    }

    @Override
    public boolean quitSpace(long spaceId, User loginUser) {
        // 1. 检查空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 2. 检查用户是否在该空间中
        SpaceUser spaceUser = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, loginUser.getId())
                .one();
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.OPERATION_ERROR, "您不是该空间的成员");

        // 3. 检查是否为空间创建者（创建者不能退出）
        if (space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间创建者不能退出空间");
        }

        // 4. 执行退出操作（将状态设置为2-已拒绝，表示已退出）
        spaceUser.setStatus(2);
        return this.updateById(spaceUser);
    }

    @Override
    public boolean checkSpaceAdmin(Long spaceId, Long userId) {
        // 参数校验
        if (spaceId == null || userId == null) {
            return false;
        }

        // 查询用户在该空间的角色
        SpaceUser spaceUser = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                .one();

        // 如果用户不是空间成员，返回false
        if (spaceUser == null) {
            return false;
        }

        // 检查用户是否是管理员
        return "admin".equals(spaceUser.getSpaceRole());
    }

    @Override
    public boolean setRecommendedMember(Long spaceId, Long userId, Integer isRecommended, User loginUser) {
        // 参数校验
        if (spaceId == null || userId == null || isRecommended == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验当前用户是否是该空间的管理员
        if (!checkSpaceAdmin(spaceId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该空间的管理员");
        }

        // 查询目标用户是否是该空间的成员
        SpaceUser targetUser = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                .one();

        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该用户不是空间成员");
        }

        // 如果是设置推荐（isRecommended=1），检查是否已达到推荐成员数量上限
        if (isRecommended == 1) {
            // 查询当前推荐成员数量
            long currentRecommendedCount = this.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getIsRecommended, 1)
                    .eq(SpaceUser::getStatus, 1)
                    .count();

            // 如果当前推荐成员数量已达到10个，且目标用户还不是推荐成员，则拒绝设置
            if (currentRecommendedCount >= 10 && (targetUser.getIsRecommended() == null || targetUser.getIsRecommended() != 1)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "推荐成员数量已达上限（10个），无法添加更多推荐成员");
            }
        }

        // 更新推荐状态
        targetUser.setIsRecommended(isRecommended);
        return this.updateById(targetUser);
    }

    @Override
    public List<SpaceUser> getRecommendedMembers(Long spaceId) {
        // 参数校验
        if (spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询推荐成员
        List<SpaceUser> recommendedMembers = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getIsRecommended, 1)
                .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                .list();

        // 如果没有推荐成员，返回空间创建者
        if (recommendedMembers.isEmpty()) {
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            if (space != null && space.getUserId() != null) {
                // 查询空间创建者的信息
                SpaceUser spaceCreator = this.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, space.getUserId())
                        .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                        .one();

                if (spaceCreator != null) {
                    // 将创建者作为推荐成员返回
                    spaceCreator.setIsRecommended(1);
                    return Collections.singletonList(spaceCreator);
                }
            }
        }

        return recommendedMembers;
    }

    /**
     * 获取空间推荐成员列表（封装类）
     *
     * @param spaceId 空间ID
     * @return 推荐成员列表（封装类）
     */
    public List<SpaceUserVO> getRecommendedMembersVO(Long spaceId) {
        // 参数校验
        if (spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询推荐成员
        List<SpaceUser> recommendedMembers = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getIsRecommended, 1)
                .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                .list();

        // 如果没有推荐成员，返回空间创建者
        if (recommendedMembers.isEmpty()) {
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            if (space != null && space.getUserId() != null) {
                // 查询空间创建者的信息
                SpaceUser spaceCreator = this.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, space.getUserId())
                        .eq(SpaceUser::getStatus, 1) // 只查询已通过审核的成员
                        .one();

                if (spaceCreator != null) {
                    // 将创建者作为推荐成员返回
                    spaceCreator.setIsRecommended(1);
                    recommendedMembers = Collections.singletonList(spaceCreator);
                }
            }
        }

        // 转换为VO对象并加载用户信息
        return getSpaceUserVOList(recommendedMembers);
    }

    @Override
    public SpaceUser checkPendingApplication(Long userId, Long spaceId) {
        if (userId == null || spaceId == null) {
            return null;
        }

        // 查询用户在该空间的申请记录（包括待审核和已通过的）
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId)
                .eq("userId", userId)
                .and(wrapper -> wrapper.eq("status", 0).or().eq("status", 1)); // 待审核或已通过

        return this.getOne(queryWrapper);
    }

    /**
     * 发送通知给空间的所有管理员
     * @param spaceId 空间ID
     * @param title 通知标题
     * @param content 通知内容
     * @param notifyType 通知类型
     */
    private void sendNotificationToSpaceAdmins(Long spaceId, String title, String content, String notifyType) {
        // 查询空间的所有管理员
        QueryWrapper<SpaceUser> adminQuery = new QueryWrapper<>();
        adminQuery.eq("spaceId", spaceId)
                .eq("spaceRole", "admin")
                .eq("status", 1); // 只查询已通过审核的管理员
        List<SpaceUser> spaceAdmins = this.list(adminQuery);

        // 为每个管理员发送通知
        for (SpaceUser admin : spaceAdmins) {
            SystemNotify systemNotify = new SystemNotify();
            systemNotify.setTitle(title);
            systemNotify.setContent(content);
            systemNotify.setNotifyType(notifyType);
            systemNotify.setReceiverType("SPECIFIC_USER");
            systemNotify.setReceiverId(String.valueOf(admin.getUserId()));
            systemNotify.setIsEnabled(1);
            systemNotify.setIsGlobal(0);
            systemNotify.setReadStatus(0);
            systemNotify.setSenderId("system");
            systemNotify.setSenderType("SYSTEM");

            systemNotifyService.addSystemNotify(systemNotify);
        }
    }

    /**
     * 发送通知给指定用户
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     * @param notifyType 通知类型
     */
    private void sendNotificationToUser(Long userId, String title, String content, String notifyType) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setTitle(title);
        systemNotify.setContent(content);
        systemNotify.setNotifyType(notifyType);
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setIsEnabled(1);
        systemNotify.setIsGlobal(0);
        systemNotify.setReadStatus(0);
        systemNotify.setSenderId("system");
        systemNotify.setSenderType("SYSTEM");

        systemNotifyService.addSystemNotify(systemNotify);
    }
}

