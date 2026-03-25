package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.model.dto.rag.SessionDeleteRequest;
import com.lumenglover.yuemupicturebackend.model.dto.rag.SessionQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.rag.SessionSwitchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.RagUserSession;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.RagSessionVO;
import com.lumenglover.yuemupicturebackend.service.RagUserSessionService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

/**
 * 会话控制层
 */
@Slf4j
@RestController
@RequestMapping("/rag/session")
public class SessionController {

    @Resource
    private RagUserSessionService ragUserSessionService;

    @Resource
    private UserService userService;

    /**
     * 新建会话
     *
     * @param sessionQueryRequest 会话创建请求
     * @param request HTTP请求
     * @return 新建的会话信息
     */
    @PostMapping("/create")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "session_create", time = 60, count = 5, message = "会话创建过于频繁，请稍后再试")
    public BaseResponse<RagSessionVO> createSession(@RequestBody(required = false) SessionQueryRequest sessionQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        // 创建新会话（已在服务层处理活跃会话逻辑）
        Long sessionId = ragUserSessionService.createSession(userId, sessionQueryRequest != null ? sessionQueryRequest.getSessionName() : null);
        RagUserSession session = ragUserSessionService.getById(sessionId);
        RagSessionVO sessionVO = RagSessionVO.objToVo(session);

        return ResultUtils.success(sessionVO);
    }

    /**
     * 会话列表查询
     *
     * @param sessionQueryRequest 查询条件
     * @param request HTTP请求
     * @return 会话列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "session_list", time = 60, count = 30, message = "会话列表查询过于频繁，请稍后再试")
    public BaseResponse<IPage<RagSessionVO>> listSessions(SessionQueryRequest sessionQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        // 确保查询请求对象不为null，然后设置用户ID查询条件
        if (sessionQueryRequest == null) {
            sessionQueryRequest = new SessionQueryRequest();
        }
        sessionQueryRequest.setUserId(userId);

        IPage<RagSessionVO> sessionPage = new Page<>();
        IPage<RagUserSession> page =
                ragUserSessionService.getSessionPage(sessionQueryRequest);

        // 转换为VO分页对象
        sessionPage.setCurrent(page.getCurrent());
        sessionPage.setSize(page.getSize());
        sessionPage.setTotal(page.getTotal());
        sessionPage.setRecords(page.getRecords().stream()
                .map(RagSessionVO::objToVo)
                .collect(Collectors.toList()));

        return ResultUtils.success(sessionPage);
    }

    /**
     * 会话切换
     *
     * @param sessionSwitchRequest 切换请求
     * @param request HTTP请求
     * @return 切换结果
     */
    @PostMapping("/switch")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "session_switch", time = 60, count = 10, message = "会话切换过于频繁，请稍后再试")
    public BaseResponse<RagSessionVO> switchSession(@RequestBody SessionSwitchRequest sessionSwitchRequest, HttpServletRequest request) {
        if (sessionSwitchRequest == null || sessionSwitchRequest.getSessionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Long sessionId = sessionSwitchRequest.getSessionId();

        // 验证会话是否存在且属于当前用户
        RagUserSession session = ragUserSessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(userId) || session.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
        }

        // 切换会话
        ragUserSessionService.switchSession(userId, sessionId);

        // 返回切换后的会话信息
        RagSessionVO sessionVO = RagSessionVO.objToVo(ragUserSessionService.getById(sessionId));
        return ResultUtils.success(sessionVO);
    }

    /**
     * 会话删除
     *
     * @param sessionDeleteRequest 删除请求
     * @param request HTTP请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "session_delete", time = 60, count = 10, message = "会话删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteSession(@RequestBody SessionDeleteRequest sessionDeleteRequest, HttpServletRequest request) {
        if (sessionDeleteRequest == null || sessionDeleteRequest.getSessionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Long sessionId = sessionDeleteRequest.getSessionId();

        // 验证会话是否存在且属于当前用户
        RagUserSession session = ragUserSessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
        }

        // 删除会话
        ragUserSessionService.deleteSession(userId, sessionId);

        return ResultUtils.success(true);
    }

    /**
     * 更新会话名称
     *
     * @param sessionQueryRequest 会话更新请求
     * @param request HTTP请求
     * @return 更新结果
     */
    @PostMapping("/updateName")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @RateLimiter(key = "session_update_name", time = 60, count = 10, message = "会话名称更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateSessionName(@RequestBody SessionQueryRequest sessionQueryRequest, HttpServletRequest request) {
        if (sessionQueryRequest == null || sessionQueryRequest.getId() == null || sessionQueryRequest.getSessionName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Long sessionId = sessionQueryRequest.getId();
        String newSessionName = sessionQueryRequest.getSessionName();

        // 验证会话是否存在且属于当前用户
        RagUserSession session = ragUserSessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(userId) || session.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或无权限访问");
        }

        // 更新会话名称
        boolean result = ragUserSessionService.updateSessionName(sessionId, newSessionName, userId);

        return ResultUtils.success(result);
    }

    /**
     * 获取用户活跃会话
     *
     * @param userId 用户ID
     * @return 活跃会话
     */
    private RagSessionVO getActiveSession(Long userId) {
        RagUserSession session = ragUserSessionService.getActiveSession(userId);
        return RagSessionVO.objToVo(session);
    }

    /**
     * 管理员分页获取所有会话列表
     *
     * @param sessionQueryRequest 查询条件
     * @param request HTTP请求
     * @return 会话列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<RagSessionVO>> listSessionsByPage(@RequestBody(required = false) SessionQueryRequest sessionQueryRequest, HttpServletRequest request) {
        if (sessionQueryRequest == null) {
            sessionQueryRequest = new SessionQueryRequest();
        }

        IPage<RagSessionVO> sessionPage = new Page<>();
        IPage<RagUserSession> page = ragUserSessionService.getSessionPage(sessionQueryRequest);

        // 转换为VO分页对象
        sessionPage.setCurrent(page.getCurrent());
        sessionPage.setSize(page.getSize());
        sessionPage.setTotal(page.getTotal());
        sessionPage.setRecords(page.getRecords().stream()
                .map(RagSessionVO::objToVo)
                .collect(Collectors.toList()));

        return ResultUtils.success(sessionPage);
    }

    /**
     * 管理员根据ID获取会话
     *
     * @param id 会话ID
     * @return 会话信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<RagSessionVO> getSessionById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        RagUserSession session = ragUserSessionService.getById(id);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(RagSessionVO.objToVo(session));
    }

    /**
     * 管理员删除会话
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSessionById(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = ragUserSessionService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员批量删除会话
     *
     * @param deleteRequestList 删除请求列表
     * @return 删除结果
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchSessions(@RequestBody List<DeleteRequest> deleteRequestList) {
        if (CollectionUtils.isEmpty(deleteRequestList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取要删除的会话ID列表
        List<Long> ids = deleteRequestList.stream()
                .map(DeleteRequest::getId)
                .collect(Collectors.toList());

        // 批量删除
        boolean result = ragUserSessionService.removeByIds(ids);
        return ResultUtils.success(result);
    }
}
