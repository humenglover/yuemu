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
import com.lumenglover.yuemupicturebackend.esdao.EsUserDao;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.model.dto.user.*;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsUser;
import com.lumenglover.yuemupicturebackend.model.vo.LoginUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
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
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EsUserDao esUserDao;


    /**
     * 获取防刷验证码
     */

    @GetMapping("/getcode")
    public BaseResponse<Map<String, String>> getCode() {
        Map<String, String> captchaData = userService.getCaptcha();
        return ResultUtils.success(captchaData);
    }

    /**
     * 获取邮箱验证码
     */
    @PostMapping("/get_emailcode")
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

    //用户注销
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
        if (result) {
            // 从ES删除
            esUserDao.deleteById(deleteRequest.getId());
        }
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
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
        if (result) {
            // 更新ES
            // 获取完整的用户信息
            User updatedUser = userService.getById(user.getId());
            // 转换为ES实体
            EsUser esUser = new EsUser();
            BeanUtils.copyProperties(updatedUser, esUser);
            esUserDao.save(esUser);
        }

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
        if (result) {
            // 批量删除ES数据
            ids.forEach(id -> esUserDao.deleteById(id));
        }

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

}
