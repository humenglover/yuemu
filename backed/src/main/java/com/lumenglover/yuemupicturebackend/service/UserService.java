package com.lumenglover.yuemupicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserExportRequest;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserModifyPassWord;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.LoginUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 鹿梦
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-12-10 10:39:52
 */
public interface UserService extends IService<User> {

    /**
     * 验证用户输入的验证码是否正确
     *
     * @param userInputCaptcha 用户输入的验证码
     * @param serververifycode 服务器端存储的加密后的验证码
     * @return 如果验证成功返回true，否则返回false
     */
    boolean validateCaptcha(String userInputCaptcha, String serververifycode);
    /**
     * 用户注册
     *
     * @param email 邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code 验证码
     * @return 新用户 id
     */
    long userRegister(String email, String userPassword, String checkPassword, String code);

    /**
     * 用户登录
     *
     * @param accountOrEmail 账号或邮箱
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String accountOrEmail, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 判断是否是登录态
     */
    User isLogin(HttpServletRequest request);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    boolean changePassword(UserModifyPassWord userModifyPassWord, HttpServletRequest request);

    boolean isAdmin(User loginUser);

    String updateUserAvatar(MultipartFile multipartFile, Long id, HttpServletRequest request);

    Map<String, String> getCaptcha();

    /**
     * 添加用户签到记录
     * @param userId 用户 id
     * @return 当前用户是否已签到成功
     */
    boolean addUserSignIn(long userId);

    /**
     * 获取用户某个年份的签到记录
     *
     * @param userId 用户 id
     * @param year   年份（为空表示当前年份）
     * @return 签到记录映射
     */
    List<Integer> getUserSignInRecord(long userId, Integer year);

    /**
     * 发送邮箱验证码
     * @param email 邮箱
     * @param type 验证码类型
     * @param request HTTP请求
     */
    void sendEmailCode(String email, String type, HttpServletRequest request);

    /**
     * 修改绑定邮箱
     * @param newEmail 新邮箱
     * @param code 验证码
     * @param request HTTP请求
     * @return 是否修改成功
     */
    boolean changeEmail(String newEmail, String code, HttpServletRequest request);

    /**
     * 重置密码
     * @param email 邮箱
     * @param newPassword 新密码
     * @param checkPassword 确认密码
     * @param code 验证码
     * @return 是否重置成功
     */
    boolean resetPassword(String email, String newPassword, String checkPassword, String code);


    /**
     * 封禁/解禁用户
     * @param userId 目标用户id
     * @param isUnban true-解禁，false-封禁
     * @param admin 执行操作的管理员
     * @return 是否操作成功
     */
    boolean banOrUnbanUser(Long userId, Boolean isUnban, User admin);

    void asyncDeleteUserData(Long id);

    /**
     * 导出用户数据
     * @param exportRequest 导出请求
     * @param httpRequest HTTP请求
     * @param httpResponse HTTP响应
     */
    void exportUserData(UserExportRequest exportRequest, HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) throws IOException;
}
