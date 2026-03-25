package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.CommonValue;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.manager.WxLoginManager;
import com.lumenglover.yuemupicturebackend.model.dto.user.*;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserUpdatePermissionsRequest;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserMultiDeviceLoginUpdateRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.LoginUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserPublicVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;
import cn.hutool.core.util.StrUtil;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private WxLoginManager wxLoginManager;

    @Resource
    private SensitiveUtil sensitiveUtil;


    /**
     * 获取防刷验证码
     */

    @GetMapping("/getcode")
    @RateLimiter(key = "user_getcode", time = 60, count = 10, message = "验证码获取过于频繁，请稍后再试")
    public BaseResponse<Map<String, String>> getCode() {
        Map<String, String> captchaData = userService.getCaptcha();
        return ResultUtils.success(captchaData);
    }

    /**
     * 获取邮箱验证码
     */
    @PostMapping("/get_emailcode")
    @RateLimiter(key = "user_emailcode", time = 3600, count = 10, message = "邮箱验证码获取过于频繁，请稍后再试")
    public BaseResponse<String> getEmailCode(@RequestBody EmailCodeRequest emailCodeRequest, HttpServletRequest request) {
        if (emailCodeRequest == null || StrUtil.hasBlank(emailCodeRequest.getEmail(), emailCodeRequest.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        userService.sendEmailCode(emailCodeRequest.getEmail(), emailCodeRequest.getType(), request);
        return ResultUtils.success("验证码发送成功");
    }

    /**
     * 修改绑定邮箱
     */
    @PostMapping("/change/email")
    @RateLimiter(key = "user_change_email", time = 3600, count = 5, message = "邮箱更改过于频繁，请稍后再试")
    public BaseResponse<Boolean> changeEmail(@RequestBody UserChangeEmailRequest userChangeEmailRequest, HttpServletRequest request) {
        if (userChangeEmailRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String newEmail = userChangeEmailRequest.getNewEmail();
        String code = userChangeEmailRequest.getCode();
        if (StrUtil.hasBlank(newEmail, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.changeEmail(newEmail, code, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @RateLimiter(key = "user_register", time = 3600, count = 3, message = "注册过于频繁，请稍后再试")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = userRegisterRequest.getEmail();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String code = userRegisterRequest.getCode();
        if (StrUtil.hasBlank(email, userPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(email, userPassword, checkPassword, code);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    @RateLimiter(key = "user_login", time = 60, count = 10, message = "登录过于频繁，请稍后再试")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String accountOrEmail = userLoginRequest.getAccountOrEmail();
        String userPassword = userLoginRequest.getUserPassword();
        String verifyCode = userLoginRequest.getVerifyCode();
        String serververifycode = userLoginRequest.getSerververifycode();
        if (StrUtil.hasBlank(accountOrEmail, userPassword, verifyCode, serververifycode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验验证码
        userService.validateCaptcha(verifyCode, serververifycode);
        LoginUserVO loginUserVO = userService.userLogin(accountOrEmail, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 微信验证码登录
     *
     * @param userWxLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login/wx_code")
    @RateLimiter(key = "user_wx_login", time = 60, count = 10, message = "登录过于频繁，请稍后再试")
    public BaseResponse<LoginUserVO> userLoginByWxCode(@RequestBody UserWxLoginRequest userWxLoginRequest, HttpServletRequest request) {
        if (userWxLoginRequest == null || StrUtil.isBlank(userWxLoginRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLoginByWxCode(userWxLoginRequest.getCode(), request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * [重构] 微信扫码/获取临时验证码登录：生成验证码和场景ID
     */
    @GetMapping("/login/wx/req_code")
    @RateLimiter(key = "user_wx_req_code", time = 60, count = 10, message = "验证码获取过于频繁，请稍后再试")
    public BaseResponse<Map<String, String>> reqWxLoginCode() {
        Map<String, String> data = wxLoginManager.generateFrontendReqCode("LOGIN");
        return ResultUtils.success(data);
    }

    /**
     * [重构] 微信扫码绑定：生成验证码和场景ID
     */
    @GetMapping("/bind/wx/req_code")
    @RateLimiter(key = "user_wx_bind_req", time = 60, count = 10, message = "验证码获取过于频繁，请稍后再试")
    public BaseResponse<Map<String, String>> reqWxBindCode() {
        Map<String, String> data = wxLoginManager.generateFrontendReqCode("BIND");
        return ResultUtils.success(data);
    }

    /**
     * [重构] 微信扫码解绑：生成验证码和场景ID
     */
    @GetMapping("/unbind/wx/req_code")
    @RateLimiter(key = "user_wx_unbind_req", time = 60, count = 10, message = "验证码获取过于频繁，请稍后再试")
    public BaseResponse<Map<String, String>> reqWxUnbindCode() {
        Map<String, String> data = wxLoginManager.generateFrontendReqCode("UNBIND");
        return ResultUtils.success(data);
    }

    /**
     * [重构] 微信扫码/获取临时验证码登录：轮询状态验证
     */
    @GetMapping("/login/wx/check_status")
    public BaseResponse<LoginUserVO> checkWxLoginStatus(@RequestParam("sceneId") String sceneId, HttpServletRequest request) {
        if (StrUtil.isBlank(sceneId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场景ID为空");
        }
        String status = wxLoginManager.getSceneStatus(sceneId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "验证码已过期，请刷新重试");
        }
        if ("WAITING".equals(status)) {
            return ResultUtils.success(null); // 前端可判断 null 继续轮询
        } else {
            // status 为已经提取出的 openId
            LoginUserVO loginUserVO = userService.userLoginByWxOpenId(status, request);
            wxLoginManager.removeReqCode(null, sceneId); // 清除已成功的场景记录
            return ResultUtils.success(loginUserVO);
        }
    }

    /**
     * [重构] 微信扫码绑定：轮询状态验证
     */
    @GetMapping("/bind/wx/check_status")
    public BaseResponse<Boolean> checkWxBindStatus(@RequestParam("sceneId") String sceneId, HttpServletRequest request) {
        if (StrUtil.isBlank(sceneId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场景ID为空");
        }
        String status = wxLoginManager.getSceneStatus(sceneId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "验证码已过期，请刷新重试");
        }
        if ("WAITING".equals(status)) {
            return ResultUtils.success(null); // 前端判断 null 则继续轮询
        } else {
            // status 为 openId
            boolean result = userService.userBindWxByOpenId(status, request);
            wxLoginManager.removeReqCode(null, sceneId);
            return ResultUtils.success(result);
        }
    }

    /**
     * 微信绑定
     *
     * @param userWxBindRequest
     * @param request
     * @return
     */
    @PostMapping("/bind/wx")
    @RateLimiter(key = "user_wx_bind", time = 60, count = 5, message = "绑定操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> userBindWx(@RequestBody UserWxBindRequest userWxBindRequest, HttpServletRequest request) {
        if (userWxBindRequest == null || StrUtil.isBlank(userWxBindRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userBindWx(userWxBindRequest.getCode(), request);
        return ResultUtils.success(result);
    }

    /**
     * 微信解绑
     *
     * @param request
     * @return
     */
    @PostMapping("/unbind/wx")
    @RateLimiter(key = "user_wx_unbind", time = 60, count = 5, message = "解绑操作过于频繁，请稍后再试")
    public BaseResponse<Boolean> userUnbindWx(HttpServletRequest request) {
        boolean result = userService.userUnbindWx(request);
        return ResultUtils.success(result);
    }

    /**
     * [重构] 微信扫码解绑：轮询状态安全验证
     */
    @GetMapping("/unbind/wx/check_status")
    public BaseResponse<Boolean> checkWxUnbindStatus(@RequestParam("sceneId") String sceneId, HttpServletRequest request) {
        if (StrUtil.isBlank(sceneId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场景ID为空");
        }
        String status = wxLoginManager.getSceneStatus(sceneId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "验证码已过期，请刷新重试");
        }
        if ("WAITING".equals(status)) {
            return ResultUtils.success(null); // 前端判断 null 则继续轮询
        } else {
            // status 为提取出的 openId。在此安全校验它必须得是当前已绑定微信才能解绑
            com.lumenglover.yuemupicturebackend.model.entity.User loginUser = userService.getLoginUser(request);
            if (loginUser.getMpOpenId() == null || !status.equals(loginUser.getMpOpenId())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的账号状态异常未绑定或请使用当前的微信验证");
            }
            boolean result = userService.userUnbindWx(request);
            wxLoginManager.removeReqCode(null, sceneId);
            return ResultUtils.success(result);
        }
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.isLogin(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    @RateLimiter(key = "user_change_password", time = 60, count = 5, message = "密码修改过于频繁，请稍后再试")
    public BaseResponse<Boolean> changePassword(@RequestBody UserModifyPassWord userModifyPassWord, HttpServletRequest request) {
        ThrowUtils.throwIf(userModifyPassWord == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.changePassword(userModifyPassWord, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    //用户注销 - 原来的简单注销方法，保留用于向后兼容
    @PostMapping("/destroy")
    public BaseResponse<Boolean> userDestroy(@RequestBody DeleteRequest userDestroyRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userDestroyRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 只能注销自己的账号
        ThrowUtils.throwIf(!loginUser.getId().equals(userDestroyRequest.getId()),
                ErrorCode.NO_AUTH_ERROR, "只能注销自己的账号");
        // 异步删除用户数据
        userService.asyncDeleteUserData(userDestroyRequest.getId());
        return ResultUtils.success(true);
    }

    // 安全注销 - 发送注销验证码
    @PostMapping("/get_destroy_code")
    @RateLimiter(key = "user_destroy_code", time = 3600, count = 3, message = "注销验证码获取过于频繁，请稍后再试")
    public BaseResponse<String> getDestroyCode(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null || StrUtil.isBlank(loginUser.getEmail())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录或邮箱未设置");
        }
        userService.sendEmailCode(loginUser.getEmail(), "userDestroy", request);
        return ResultUtils.success("注销验证码已发送至您的邮箱");
    }

    // 安全注销 - 执行安全注销
    @PostMapping("/destroy_secure")
    public BaseResponse<Boolean> userDestroySecure(@RequestBody UserDestroySecureRequest userDestroySecureRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userDestroySecureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.secureUserDestroy(loginUser.getId(), userDestroySecureRequest.getUserPassword(), userDestroySecureRequest.getCode());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update/avatar")
    public BaseResponse<String> updateUserAvatar(MultipartFile multipartFile,Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        String result = userService.updateUserAvatar(multipartFile,id, request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码
        final String DEFAULT_PASSWORD = CommonValue.DEFAULT_PASSWORD;
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        // 插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteUser(@RequestBody List<Long> deleteRequestList,
                                                 HttpServletRequest request) {
        // 参数校验，如果传入的删除请求列表为空，则抛出参数异常
        ThrowUtils.throwIf(deleteRequestList == null || deleteRequestList.isEmpty(), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 根据ID列表查询对应的图片列表
        List<User> pictureList = userService.listByIds(deleteRequestList);
        // 校验图片是否存在，如果查询到的图片列表为空，则抛出未找到资源异常
        ThrowUtils.throwIf(pictureList == null || pictureList.isEmpty(), ErrorCode.NOT_FOUND_ERROR);
        // 批量删除操作
        boolean result = userService.removeByIds(deleteRequestList);
        // 如果删除失败，抛出操作异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从MySQL删除
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @RateLimiter(key = "user_update", time = 60, count = 10, message = "用户更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 对请求参数进行敏感词过滤
        if (userUpdateRequest.getUserName() != null) {
            userUpdateRequest.setUserName(sensitiveUtil.filter(userUpdateRequest.getUserName()));
        }
        if (userUpdateRequest.getUserProfile() != null) {
            userUpdateRequest.setUserProfile(sensitiveUtil.filter(userUpdateRequest.getUserProfile()));
        }
        if (userUpdateRequest.getGender() != null) {
            userUpdateRequest.setGender(sensitiveUtil.filter(userUpdateRequest.getGender()));
        }
        if (userUpdateRequest.getRegion() != null) {
            userUpdateRequest.setRegion(sensitiveUtil.filter(userUpdateRequest.getRegion()));
        }
        if (userUpdateRequest.getUserTags() != null) {
            userUpdateRequest.setUserTags(sensitiveUtil.filter(userUpdateRequest.getUserTags()));
        }
        if (userUpdateRequest.getPersonalSign() != null) {
            userUpdateRequest.setPersonalSign(sensitiveUtil.filter(userUpdateRequest.getPersonalSign()));
        }
        if (userUpdateRequest.getInterestField() != null) {
            userUpdateRequest.setInterestField(sensitiveUtil.filter(userUpdateRequest.getInterestField()));
        }
        // 注意：homepageBg是URL字段，不进行敏感词过滤，避免URL被误伤
        if (userUpdateRequest.getThemePreference() != null) {
            userUpdateRequest.setThemePreference(sensitiveUtil.filter(userUpdateRequest.getThemePreference()));
        }
        if (userUpdateRequest.getVisibilitySetting() != null) {
            userUpdateRequest.setVisibilitySetting(sensitiveUtil.filter(userUpdateRequest.getVisibilitySetting()));
        }

        // 判断是否是管理员，管理员可以更新任意用户，普通用户只能更新自己
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null || !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)) {
            userUpdateRequest.setUserRole(UserConstant.DEFAULT_ROLE);
        }

        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);

        // 更新MySQL
        boolean result = userService.updateById(user);

        return ResultUtils.success(result);
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchUser(@RequestBody List<DeleteRequest> deleteRequestList) {
        if (CollectionUtils.isEmpty(deleteRequestList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取要删除的用户ID列表
        List<Long> ids = deleteRequestList.stream()
                .map(DeleteRequest::getId)
                .collect(Collectors.toList());

        // 批量删除MySQL数据
        boolean result = userService.removeByIds(ids);

        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 添加用户签到记录
     *
     * @param request
     * @return 当前是否已签到成功
     */
    @PostMapping("/add/sign_in")
    public BaseResponse<Boolean> addUserSignIn(HttpServletRequest request) {
        // 必须要登录才能签到
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.addUserSignIn(loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取用户签到记录
     *
     * @param year    年份（为空表示当前年份）
     * @param request
     * @return 签到记录映射
     */
    @GetMapping("/get/sign_in")
    public BaseResponse<List<Integer>> getUserSignInRecord(Integer year, HttpServletRequest request) {
        // 必须要登录才能获取
        User loginUser = userService.getLoginUser(request);
        List<Integer> userSignInRecord = userService.getUserSignInRecord(loginUser.getId(), year);
        return ResultUtils.success(userSignInRecord);
    }

    /**
     * 忘记密码
     */
    @PostMapping("/reset/password")
    @RateLimiter(key = "user_reset_password", time = 3600, count = 5, message = "密码重置过于频繁，请稍后再试")
    public BaseResponse<Boolean> resetPassword(@RequestBody UserResetPasswordRequest resetPasswordRequest) {
        if (resetPasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = resetPasswordRequest.getEmail();
        String newPassword = resetPasswordRequest.getNewPassword();
        String checkPassword = resetPasswordRequest.getCheckPassword();
        String code = resetPasswordRequest.getCode();

        if (StrUtil.hasBlank(email, newPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userService.resetPassword(email, newPassword, checkPassword, code);
        return ResultUtils.success(result);
    }

    /**
     * 用户封禁/解禁（仅管理员）
     */
    @PostMapping("/ban")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> banOrUnbanUser(@RequestBody UserUnbanRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取管理员信息
        User admin = userService.getLoginUser(httpRequest);

        boolean result = userService.banOrUnbanUser(request.getUserId(), request.getIsUnban(), admin);
        return ResultUtils.success(result);
    }

    /**
     * 导出用户数据（仅管理员）
     */
    @PostMapping("/export")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void exportUsers(@RequestBody UserExportRequest exportRequest,
                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            userService.exportUserData(exportRequest, httpRequest, httpResponse);
        } catch (IOException e) {
            log.error("导出用户数据失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "导出失败");
        }
    }

    /**
     * 获取用户公开信息
     *
     * @param userId 用户ID
     * @return 用户公开信息（不包含邮箱等敏感信息）
     */
    @GetMapping("/get/public")
    public BaseResponse<UserPublicVO> getUserPublicInfo(@RequestParam Long userId, HttpServletRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        UserPublicVO userPublicVO = userService.getUserPublicInfo(userId);

        // 添加用户浏览记录
        try {
            User visitor = userService.isLogin(request);
            if (visitor != null && !visitor.getId().equals(userId)) { // 避免自己浏览自己
                userService.addUserViewRecord(userId, visitor.getId(), request);
            }
        } catch (Exception e) {
            log.error("添加用户浏览记录失败", e);
        }

        return ResultUtils.success(userPublicVO);
    }

    /**
     * 获取用户权限设置
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 用户权限信息
     */
    @GetMapping("/get/permissions")
    public BaseResponse<UserUpdatePermissionsRequest> getUserPermissions(@RequestParam Long userId, HttpServletRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查权限：只能获取自己的权限信息
        if (!loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能获取自己的权限信息");
        }

        // 获取用户权限信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 构建权限信息响应
        UserUpdatePermissionsRequest permissions = new UserUpdatePermissionsRequest();
        permissions.setUserId(user.getId());
        permissions.setAllowPrivateChat(user.getAllowPrivateChat());
        permissions.setAllowFollow(user.getAllowFollow());
        permissions.setShowFollowList(user.getShowFollowList());
        permissions.setShowFansList(user.getShowFansList());

        return ResultUtils.success(permissions);
    }

    /**
     * 更新用户权限设置
     *
     * @param updateRequest 更新权限请求
     * @param request HTTP请求
     * @return 是否更新成功
     */
    @PostMapping("/update/permissions")
    public BaseResponse<Boolean> updateUserPermissions(@RequestBody UserUpdatePermissionsRequest updateRequest, HttpServletRequest request) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查权限：只能更新自己的权限
        if (!loginUser.getId().equals(updateRequest.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能更新自己的权限");
        }

        // 执行更新
        boolean result = userService.updateUserPermissions(
                updateRequest.getUserId(),
                updateRequest.getAllowPrivateChat(),
                updateRequest.getAllowFollow(),
                updateRequest.getShowFollowList(),
                updateRequest.getShowFansList()
        );

        return ResultUtils.success(result);
    }

    /**
     * 获取用户多设备登录设置
     *
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 是否允许多设备登录：1-允许、0-禁止
     */
    @GetMapping("/get/multi_device_login")
    public BaseResponse<Integer> getUserMultiDeviceLogin(@RequestParam Long userId, HttpServletRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查权限：只能获取自己的设置
        if (!loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能获取自己的多设备登录设置");
        }

        Integer multiDeviceLoginSetting = userService.getUserMultiDeviceLogin(userId);

        return ResultUtils.success(multiDeviceLoginSetting);
    }

    /**
     * 更新用户多设备登录设置
     *
     * @param updateRequest 更新多设备登录设置请求
     * @param request HTTP请求
     * @return 是否更新成功
     */
    @PostMapping("/update/multi_device_login")
    public BaseResponse<Boolean> updateUserMultiDeviceLogin(@RequestBody UserMultiDeviceLoginUpdateRequest updateRequest, HttpServletRequest request) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查权限：只能更新自己的设置
        if (!loginUser.getId().equals(updateRequest.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能更新自己的多设备登录设置");
        }

        // 执行更新
        boolean result = userService.updateUserMultiDeviceLogin(
                updateRequest.getUserId(),
                updateRequest.getAllowMultiDeviceLogin()
        );

        return ResultUtils.success(result);
    }
}

