package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.CommonValue;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.manager.FileManager;
import com.lumenglover.yuemupicturebackend.manager.WxLoginManager;
import com.lumenglover.yuemupicturebackend.manager.auth.StpKit;
import com.lumenglover.yuemupicturebackend.mapper.UserMapper;
import com.lumenglover.yuemupicturebackend.mapper.UserSignInRecordMapper;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserModifyPassWord;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.user.UserExportRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.enums.UserRoleEnum;
import com.lumenglover.yuemupicturebackend.model.vo.LoginUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.SafeUserVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserPublicVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.service.PostService;
import com.lumenglover.yuemupicturebackend.service.PostAttachmentService;
import com.lumenglover.yuemupicturebackend.service.ViewRecordService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import com.lumenglover.yuemupicturebackend.utils.TencentCloudImageAuditUtil;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.utils.DefaultAvatarUtil;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * @author 鹿梦
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-12-10 10:39:52
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserMapper userMapper;
    @Resource
    private FileManager fileManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserSignInRecordMapper userSignInRecordMapper;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Resource
    @Lazy
    private CrawlerManager crawlerManager;

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    @Lazy
    private PostService postService;

    @Resource
    @Lazy
    private PostAttachmentService postAttachmentService;

    @Resource
    private TencentCloudImageAuditUtil tencentCloudImageAuditUtil;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    @Resource
    private SensitiveUtil sensitiveUtil;

    @Resource
    private ViewRecordService viewRecordService;

    @Resource
    private WxLoginManager wxLoginManager;



    /**
     * 用户注册
     *
     * @param email       邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code         验证码
     * @return 用户注册成功后的ID
     */
    @Override
    public long userRegister(String email, String userPassword, String checkPassword, String code) {
        // 1. 校验
        if (StrUtil.hasBlank(email, userPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 校验验证码
        String verifyCodeKey = String.format("email:code:verify:register:%s", email);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        synchronized (email.intern()) {
            // 检查邮箱是否已被注册
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
            }

            // 检查账号是否已被使用
            String userAccount = email.substring(0, email.indexOf("@")); // 使用邮箱前缀作为账号
            // 对账号进行敏感词过滤
            userAccount = sensitiveUtil.filter(userAccount);
            queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                // 如果账号已存在，则在后面加上随机数
                userAccount = userAccount + RandomUtil.randomNumbers(4);
            }

            // 2. 加密
            String encryptPassword = DigestUtil.md5Hex(CommonValue.DEFAULT_SALT + userPassword);
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setEmail(email);
            user.setUserPassword(encryptPassword);
            // 使用账号作为默认用户名，同样进行敏感词过滤
            String userName = sensitiveUtil.filter(userAccount);
            user.setUserName(userName);
            user.setUserRole(UserRoleEnum.USER.getValue());
            // 设置随机默认头像
            user.setUserAvatar(DefaultAvatarUtil.getRandomAvatar());
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            // 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);
            return user.getId();
        }
    }

    /**
     * 用户登录
     *
     * @param accountOrEmail 账号或邮箱
     * @param userPassword  用户密码
     * @param request      HttpServletRequest对象
     * @return 登录后的用户信息视图对象（LoginUserVO）
     */
    @Override
    public LoginUserVO userLogin(String accountOrEmail, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(accountOrEmail, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtil.md5Hex(CommonValue.DEFAULT_SALT + userPassword);
        // 3. 查询用户
        // 3. 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPassword", encryptPassword)
                .and(wrapper -> {
                    // 仅支持邮箱登录
                    wrapper.eq("email", accountOrEmail);
                    // 如果是纯数字，则支持悦木号（ID）登录
                    if (accountOrEmail.matches("\\d+")) {
                        wrapper.or().eq("id", accountOrEmail);
                    }
                });
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, accountOrEmail cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 4. 记录用户登录态
        // 设置 Sa-Token 登录态
        StpKit.SPACE.login(user.getId());
        // 在 Sa-Token Session 中存入完整的用户信息
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        return SecureUtil.md5(CommonValue.DEFAULT_SALT + userPassword);
    }

    // 以下是其他未修改的方法，省略了详细代码，可根据实际情况继续完善或优化

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从 Sa-Token 中获取登录信息
        if (!StpKit.SPACE.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        User user = (User) StpKit.SPACE.getSession().get(UserConstant.USER_LOGIN_STATE);
        if (user == null) {
            // 如果Sa-Token中没有用户信息，从数据库获取并更新
            Long userId = StpKit.SPACE.getLoginIdAsLong();
            user = this.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            // 更新 Sa-Token 中的用户信息
            StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        }

        // 替换URL为自定义域名
        user.replaceUrlWithCustomDomain();
        return user;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        return LoginUserVO.objToVo(user);
    }

    @Override
    public User isLogin(HttpServletRequest request) {
        // 判断是否已经登录
        if (!StpKit.SPACE.isLogin()) {
            return null;
        }

        User user = (User) StpKit.SPACE.getSession().get(UserConstant.USER_LOGIN_STATE);
        if (user == null) {
            // 如果Sa-Token中没有用户信息，从数据库获取
            Long userId = StpKit.SPACE.getLoginIdAsLong();
            user = this.getById(userId);
            if (user == null) {
                return null;
            }
            // 更新 Sa-Token 中的用户信息
            StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        }

        // 替换URL为自定义域名
        user.replaceUrlWithCustomDomain();
        return user;
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.objToVo(user);
    }

    /**
     * 获得脱敏后的用户信息（根据权限判断是否显示邮箱）
     *
     * @param user
     * @param loginUser 当前登录用户
     * @return
     */
    public UserVO getUserVO(User user, User loginUser) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        // 如果不是本人或管理员，将邮箱设置为空
        if (loginUser == null || (!loginUser.getId().equals(user.getId()) && !isAdmin(loginUser))) {
            userVO.setEmail(null);
        }
        return userVO;
    }

    /**
     * 获得安全的脱敏后的用户信息（不包含邮箱等敏感信息）
     *
     * @param user
     * @return
     */
    @Override
    public SafeUserVO getSafeUserVO(User user) {
        if (user == null) {
            return null;
        }
        return SafeUserVO.objToVo(user);
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 移除 Sa-Token 登录态
        if (StpKit.SPACE.isLogin()) {
            StpKit.SPACE.logout();
        }
        // 如果未登录，也认为登出成功（静默处理）
        return true;
    }

    /**
     * 异步删除用户相关数据
     */
    @Async("asyncExecutor")
    public void asyncDeleteUserData(Long userId) {
        try {
            // 1. 删除用户发布的图片
            QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
            pictureQueryWrapper.eq("userId", userId);
            List<Picture> pictureList = pictureService.list(pictureQueryWrapper);
            if (!pictureList.isEmpty()) {
                // 删除数据库记录
                pictureService.remove(pictureQueryWrapper);
            }

            // 2. 删除用户发布的帖子
            QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
            postQueryWrapper.eq("userId", userId);
            List<Post> postList = postService.list(postQueryWrapper);
            if (!postList.isEmpty()) {
                // 删除帖子附件
                List<Long> postIds = postList.stream()
                        .map(Post::getId)
                        .collect(Collectors.toList());
                QueryWrapper<PostAttachment> attachmentQueryWrapper = new QueryWrapper<>();
                attachmentQueryWrapper.in("postId", postIds);
                postAttachmentService.remove(attachmentQueryWrapper);
                // 删除帖子
                postService.remove(postQueryWrapper);
            }

            // 3. 删除用户数据
            this.removeById(userId);

            // 4. 清理相关缓存
            String userKey = String.format("user:ban:%d", userId);
            stringRedisTemplate.delete(userKey);

            log.info("用户相关数据删除完成, userId={}", userId);
        } catch (Exception e) {
            log.error("删除用户相关数据失败, userId={}", userId, e);
            // 这里不抛出异常，因为是异步操作，主流程已经完成
        }
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String gender = userQueryRequest.getGender();
        String region = userQueryRequest.getRegion();
        String userTags = userQueryRequest.getUserTags();
        String personalSign = userQueryRequest.getPersonalSign();
        String interestField = userQueryRequest.getInterestField();
        String homepageBg = userQueryRequest.getHomepageBg();
        String themePreference = userQueryRequest.getThemePreference();
        String visibilitySetting = userQueryRequest.getVisibilitySetting();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StrUtil.isNotBlank(gender), "gender", gender);
        queryWrapper.like(StrUtil.isNotBlank(region), "region", region);
        queryWrapper.like(StrUtil.isNotBlank(userTags), "userTags", userTags);
        queryWrapper.like(StrUtil.isNotBlank(personalSign), "personalSign", personalSign);
        queryWrapper.like(StrUtil.isNotBlank(interestField), "interestField", interestField);
        queryWrapper.like(StrUtil.isNotBlank(homepageBg), "homepageBg", homepageBg);
        queryWrapper.like(StrUtil.isNotBlank(themePreference), "themePreference", themePreference);
        queryWrapper.like(StrUtil.isNotBlank(visibilitySetting), "visibilitySetting", visibilitySetting);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean changePassword(UserModifyPassWord userModifyPassWord, HttpServletRequest request) {
        if(StrUtil.hasBlank(userModifyPassWord.getOldPassword(), userModifyPassWord.getNewPassword(), userModifyPassWord.getCheckPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if(!userModifyPassWord.getNewPassword().equals(userModifyPassWord.getCheckPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if(userModifyPassWord.getNewPassword().length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能小于8位");
        }
        //查询是否有这个用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", userModifyPassWord.getId());
        String encryptPassword = getEncryptPassword(userModifyPassWord.getOldPassword());
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        user.setUserPassword(getEncryptPassword(userModifyPassWord.getNewPassword()));
        // 更新MySQL
        boolean result = userMapper.updateById(user) > 0;
        return result;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public String updateUserAvatar(MultipartFile multipartFile, Long id, HttpServletRequest request) {
        //判断用户是否存在
        User user = userMapper.selectById(id);
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        //判断用户是否登录
        User loginUser = getLoginUser(request);
        if(loginUser == null || !loginUser.getId().equals(id)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        //判断文件是否为空
        if(multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        //判断文件类型
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        // 对上传的图像进行机器审核
        try {
            // 获取审核策略类型，可以从配置中读取
            String bizType = cosClientConfig.getAuditBizType();
            if (bizType == null) {
                bizType = ""; // 默认为空，使用系统默认审核策略
            }

            // 对图像进行审核
            ImageAuditingResponse auditResponse =
                    tencentCloudImageAuditUtil.auditImageByUrl(uploadPictureResult.getUrl(), bizType);

            boolean isCompliant = tencentCloudImageAuditUtil.isImageCompliant(auditResponse);
            String auditLabel = tencentCloudImageAuditUtil.getAuditLabel(auditResponse);
            Integer auditScore = tencentCloudImageAuditUtil.getAuditScore(auditResponse);

            if (!isCompliant) {
                log.warn("图像审核未通过，URL: {}, 标签: {}, 分数: {}", uploadPictureResult.getUrl(), auditLabel, auditScore);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像审核未通过: " + auditLabel + " (分数: " + auditScore + ")");
            }
        } catch (Exception e) {
            log.error("图像审核服务调用失败，URL: {}, 错误: {}", uploadPictureResult.getUrl(), e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图像审核服务异常，上传失败");
        }

        //更新用户图像
        user.setUserAvatar(uploadPictureResult.getUrl());
        // 更新MySQL
        boolean result = userMapper.updateById(user) > 0;
        return uploadPictureResult.getUrl();
    }

    @Override
    public Map<String, String> getCaptcha() {
        // 仅包含数字的字符集
        String characters = "0123456789";
        // 生成 4 位数字验证码
        RandomGenerator randomGenerator = new RandomGenerator(characters, 4);
        // 定义图片的显示大小，并创建验证码对象
        ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(320, 100, 4, 4);
        shearCaptcha.setGenerator(randomGenerator);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        shearCaptcha.write(outputStream);
        byte[] captchaBytes = outputStream.toByteArray();
        String base64Captcha = Base64.getEncoder().encodeToString(captchaBytes);
        String captchaCode = shearCaptcha.getCode();

        // 使用 Hutool 的 MD5 加密
        String encryptedCaptcha = DigestUtil.md5Hex(captchaCode);

        // 将加密后的验证码和 Base64 编码的图片存储到 Redis 中，设置过期时间为 5 分钟（300 秒）
        stringRedisTemplate.opsForValue().set("captcha:" + encryptedCaptcha, captchaCode, 300, TimeUnit.SECONDS);

        Map<String, String> data = new HashMap<>();
        data.put("base64Captcha", base64Captcha);
        data.put("encryptedCaptcha", encryptedCaptcha);
        return data;
    }

    /**
     * 添加用户签到记录
     * @param userId 用户 id
     * @return 当前用户是否已签到成功
     */
    @Override
    public boolean addUserSignIn(long userId) {
        LocalDate date = LocalDate.now();
        int currentYear = date.getYear();
        String redisKey = RedisConstant.getUserSignInRedisKey(currentYear, userId);

        // 获取 Redis 的 BitMap
        RBitSet signInBitSet = redissonClient.getBitSet(redisKey);
        int dayOfYear = date.getDayOfYear();

        // 查询当天有没有签到
        if (!signInBitSet.get(dayOfYear)) {
            // 如果当前未签到，则设置Redis
            signInBitSet.set(dayOfYear, true);

            // 设置 Redis 键的过期时间到当年最后一天
            LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
            Duration timeUntilEndOfYear = Duration.between(
                    LocalDateTime.now(),
                    endOfYear.atTime(23, 59, 59)
            );
            redissonClient.getBucket(redisKey).expire(timeUntilEndOfYear);
        }

        return true;
    }

    /**
     * 获取用户某个年份的签到记录
     *
     * @param userId 用户 id
     * @param year   年份（为空表示当前年份）
     * @return 签到记录映射
     */
    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        int currentYear = LocalDate.now().getYear();
        List<Integer> signInDays = new ArrayList<>();

        if (year != currentYear) {
            // 非当年数据直接从MySQL查询
            QueryWrapper<UserSignInRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                    .eq("year", year);

            UserSignInRecord record = userSignInRecordMapper.selectOne(queryWrapper);
            if (record != null && record.getSignInData() != null) {
                byte[] signInData = record.getSignInData();
                // 解析bitmap数据
                for (int day = 1; day <= 366; day++) {
                    int byteIndex = (day - 1) / 8;
                    int bitIndex = (day - 1) % 8;
                    if ((signInData[byteIndex] & (1 << bitIndex)) != 0) {
                        signInDays.add(day);
                    }
                }
            }
            return signInDays;
        }

        // 当年数据从Redis获取
        String redisKey = RedisConstant.getUserSignInRedisKey(year, userId);
        RBitSet signInBitSet = redissonClient.getBitSet(redisKey);

        // 如果Redis中没有数据，从MySQL加载
        if (!signInBitSet.isExists()) {
            QueryWrapper<UserSignInRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                    .eq("year", year);

            UserSignInRecord record = userSignInRecordMapper.selectOne(queryWrapper);
            if (record != null && record.getSignInData() != null) {
                byte[] signInData = record.getSignInData();
                // 将MySQL中的bitmap数据加载到Redis
                for (int day = 1; day <= 366; day++) {
                    int byteIndex = (day - 1) / 8;
                    int bitIndex = (day - 1) % 8;
                    if ((signInData[byteIndex] & (1 << bitIndex)) != 0) {
                        signInBitSet.set(day, true);
                    }
                }

                // 设置过期时间到年底
                LocalDate endOfYear = LocalDate.of(year, 12, 31);
                Duration timeUntilEndOfYear = Duration.between(
                        LocalDateTime.now(),
                        endOfYear.atTime(23, 59, 59)
                );
                redissonClient.getBucket(redisKey).expire(timeUntilEndOfYear);
            }
        }

        // 从Redis的bitmap中获取签到记录
        BitSet bitSet = signInBitSet.asBitSet();
        int index = bitSet.nextSetBit(0);
        while (index >= 0) {
            signInDays.add(index);
            index = bitSet.nextSetBit(index + 1);
        }

        return signInDays;
    }

    @Override
    public boolean validateCaptcha(String userInputCaptcha, String serververifycode) {
        if (userInputCaptcha!= null && serververifycode!= null) {
            // 使用Hutool对用户输入的验证码进行MD5加密
            String encryptedVerifycode = DigestUtil.md5Hex(userInputCaptcha);
            if(encryptedVerifycode.equals(serververifycode)){
                return true;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
    }

    @Override
    public User getLoginUserByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        try {
            // 从 Sa-Token 中获取用户 ID
            Object loginId = StpKit.SPACE.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }

            // 获取用户信息
            Long userId = Long.parseLong(loginId.toString());
            User user = this.getById(userId);

            if (user == null || user.getId() == null) {
                return null;
            }

            return user;
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return null;
        }
    }

    /**
     * 校验用户相关输入参数的合法性
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码（注册时需要，登录时传null）
     * @param isRegister    是否为注册操作
     */
    private void validateUserInputParams(String userAccount, String userPassword, String checkPassword, boolean isRegister) {
        if (StrUtil.hasBlank(userAccount, userPassword) || (isRegister && StrUtil.hasBlank(checkPassword))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度不能小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度不能小于8位");
        }
        if (isRegister &&!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 根据注册信息构建User对象
     *
     * @param userAccount    用户账户
     * @param encryptPassword 加密后的密码
     * @return 构建好的User对象
     */
    private User buildUserForRegistration(String userAccount, String encryptPassword) {
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        return user;
    }

    @Override
    public boolean removeById(Serializable id) {
        // 从MySQL删除
        boolean result = super.removeById(id);
        return result;
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        // 从MySQL批量删除
        boolean result = super.removeByIds(idList);
        return result;
    }

    @Override
    public boolean updateById(User entity) {
        // 对可能包含敏感内容的字段进行过滤
        if (entity.getUserName() != null) {
            entity.setUserName(sensitiveUtil.filter(entity.getUserName()));
        }
        if (entity.getUserProfile() != null) {
            entity.setUserProfile(sensitiveUtil.filter(entity.getUserProfile()));
        }
        if (entity.getGender() != null) {
            entity.setGender(sensitiveUtil.filter(entity.getGender()));
        }
        if (entity.getRegion() != null) {
            entity.setRegion(sensitiveUtil.filter(entity.getRegion()));
        }
        if (entity.getUserTags() != null) {
            entity.setUserTags(sensitiveUtil.filter(entity.getUserTags()));
        }
        if (entity.getPersonalSign() != null) {
            entity.setPersonalSign(sensitiveUtil.filter(entity.getPersonalSign()));
        }
        if (entity.getInterestField() != null) {
            entity.setInterestField(sensitiveUtil.filter(entity.getInterestField()));
        }
        // 注意：homepageBg是URL字段，不进行敏感词过滤，避免URL被误伤
        // if (entity.getHomepageBg() != null) {
        //     entity.setHomepageBg(sensitiveUtil.filter(entity.getHomepageBg()));
        // }
        if (entity.getThemePreference() != null) {
            entity.setThemePreference(sensitiveUtil.filter(entity.getThemePreference()));
        }
        if (entity.getVisibilitySetting() != null) {
            entity.setVisibilitySetting(sensitiveUtil.filter(entity.getVisibilitySetting()));
        }

        // 更新MySQL
        boolean result = super.updateById(entity);

        // 如果更新成功，同时更新Sa-Token中的用户信息
        if (result && entity.getId() != null) {
            // 检查当前用户是否已登录，并且更新的是当前用户的信息
            if (StpKit.SPACE.isLogin() && StpKit.SPACE.getLoginIdAsLong() == entity.getId().longValue()) {
                // 从数据库获取完整更新后的用户信息
                User updatedUser = this.getById(entity.getId());
                if (updatedUser != null) {
                    // 更新Sa-Token会话中的用户信息
                    StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, updatedUser);
                }
            }
        }

        return result;
    }

    @Override
    public void sendEmailCode(String email, String type, HttpServletRequest request) {
        if (StrUtil.hasBlank(email, type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);

        // 获取客户端IP
        String clientIp = request.getRemoteAddr();
        String ipKey = String.format("email:code:ip:%s", clientIp);
        String emailKey = String.format("email:code:email:%s", email);

        // 检查IP是否频繁请求验证码
        String ipCount = stringRedisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && Integer.parseInt(ipCount) >= 5) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求验证码过于频繁，请稍后再试");
        }

        // 检查邮箱是否频繁请求验证码
        String emailCount = stringRedisTemplate.opsForValue().get(emailKey);
        if (emailCount != null && Integer.parseInt(emailCount) >= 3) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该邮箱请求验证码过于频繁，请稍后再试");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 发送验证码
        try {
            emailSenderUtil.sendEmail(email, code);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送验证码失败");
        }

        // 记录IP和邮箱的请求次数，设置1小时过期
        stringRedisTemplate.opsForValue().increment(ipKey, 1);
        stringRedisTemplate.expire(ipKey, 1, TimeUnit.HOURS);

        stringRedisTemplate.opsForValue().increment(emailKey, 1);
        stringRedisTemplate.expire(emailKey, 1, TimeUnit.HOURS);

        // 将验证码存入Redis，设置5分钟过期
        String verifyCodeKey = String.format("email:code:verify:%s:%s", type, email);
        stringRedisTemplate.opsForValue().set(verifyCodeKey, code, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean changeEmail(String newEmail, String code, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(newEmail, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!newEmail.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }

        // 2. 校验验证码
        String verifyCodeKey = String.format("email:code:verify:changeEmail:%s", newEmail);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        // 3. 获取当前登录用户
        User loginUser = getLoginUser(request);

        synchronized (newEmail.intern()) {
            // 4. 检查新邮箱是否已被使用
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", newEmail);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被使用");
            }

            // 5. 更新邮箱
            User user = new User();
            user.setId(loginUser.getId());
            user.setEmail(newEmail);
            boolean result = this.updateById(user);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改邮箱失败");
            }

            // 6. 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);
            return true;
        }
    }

    @Override
    public boolean resetPassword(String email, String newPassword, String checkPassword, String code) {
        // 1. 校验参数
        if (StrUtil.hasBlank(email, newPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 2. 校验邮箱格式
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }

        // 3. 校验密码
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 4. 校验验证码
        String verifyCodeKey = String.format("email:code:verify:resetPassword:%s", email);
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        // 5. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 6. 更新密码
        String encryptPassword = getEncryptPassword(newPassword);
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setUserPassword(encryptPassword);
        boolean result = this.updateById(updateUser);

        if (result) {
            // 7. 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);

        }

        return result;
    }

    @Override
    public boolean banOrUnbanUser(Long userId, Boolean isUnban, User admin) {
        // 1. 校验参数
        if (userId == null || userId <= 0 || isUnban == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验管理员权限
        if (!UserConstant.ADMIN_ROLE.equals(admin.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能执行此操作");
        }

        // 3. 获取目标用户信息
        User targetUser = this.getById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 4. 检查当前状态是否需要变更
        boolean isBanned = CrawlerConstant.BAN_ROLE.equals(targetUser.getUserRole());
        if (isUnban == isBanned) {
            // 5. 更新用户角色
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setUserRole(isUnban ? UserConstant.DEFAULT_ROLE : CrawlerConstant.BAN_ROLE);
            updateUser.setUpdateTime(new Date());
            boolean result = this.updateById(updateUser);

            if (result) {
                // 6. 记录操作日志
                log.info("管理员[{}]{}用户[{}]",
                        admin.getUserAccount(),
                        isUnban ? "解封" : "封禁",
                        targetUser.getUserAccount());

                // 7. 处理Redis缓存
                String banKey = String.format("user:ban:%d", userId);
                if (isUnban) {
                    stringRedisTemplate.delete(banKey);
                } else {
                    stringRedisTemplate.opsForValue().set(banKey, "1");
                }


            }

            return result;
        } else {
            // 状态已经是目标状态
            String operation = isUnban ? "解封" : "封禁";
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("该用户当前%s不需要%s", isUnban ? "未被封禁" : "已被封禁", operation));
        }
    }

    @Override
    public void exportUserData(UserExportRequest exportRequest, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) throws IOException {
        if (exportRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            // 1. 创建工作表
            HSSFSheet sheet = workbook.createSheet("Sheet1");

            // 2. 创建表头
            String[] headers = {"用户ID", "账号", "邮箱", "昵称", "角色", "创建时间", "状态"};
            HSSFRow headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 3. 查询数据
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            setTimeRangeCondition(queryWrapper, exportRequest);
            List<User> users = this.list(queryWrapper);

            // 4. 填充数据
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int rowIndex = 1;

            for (User user : users) {
                HSSFRow dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(String.valueOf(user.getId()));
                dataRow.createCell(1).setCellValue(user.getUserAccount());
                dataRow.createCell(2).setCellValue(user.getEmail());
                dataRow.createCell(3).setCellValue(user.getUserName());
                dataRow.createCell(4).setCellValue(getUserRoleText(user.getUserRole()));
                dataRow.createCell(5).setCellValue(user.getCreateTime() != null ?
                        sdf.format(user.getCreateTime()) : "");
                dataRow.createCell(6).setCellValue(getUserStatusText(user.getUserRole()));
            }

            // 5. 设置响应头
            httpResponse.setContentType("application/vnd.ms-excel;charset=utf-8");
            httpResponse.setCharacterEncoding("UTF-8");

            // 6. 处理文件名
            String fileName = generateExportFileName(exportRequest.getType(),
                    exportRequest.getStartTime(), exportRequest.getEndTime());
            httpResponse.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xls");

            // 7. 写入响应
            workbook.write(httpResponse.getOutputStream());
        } catch (IOException e) {
            log.error("导出用户数据失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出失败");
        }
    }

    /**
     * 获取用户角色文本
     */
    private String getUserRoleText(String role) {
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return "管理员";
        } else if (UserConstant.DEFAULT_ROLE.equals(role)) {
            return "普通用户";
        } else if (CrawlerConstant.BAN_ROLE.equals(role)) {
            return "已封禁";
        }
        return "未知";
    }

    /**
     * 获取用户状态文本
     */
    private String getUserStatusText(String role) {
        if (CrawlerConstant.BAN_ROLE.equals(role)) {
            return "已封禁";
        }
        return "正常";
    }

    /**
     * 生成导出文件名
     */
    private String generateExportFileName(Integer type, Date startTime, Date endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String timeStr = sdf.format(new Date());

        String periodStr;
        switch (type) {
            case 1:
                periodStr = "日报";
                break;
            case 2:
                periodStr = "周报";
                break;
            case 3:
                periodStr = "月报";
                break;
            case 4:
                periodStr = "年报";
                break;
            case 5:
                if (startTime != null && endTime != null) {
                    periodStr = sdf.format(startTime) + "-" + sdf.format(endTime);
                } else {
                    periodStr = "自定义";
                }
                break;
            default:
                periodStr = "未知";
        }

        return String.format("用户数据_%s", periodStr);
    }

    /**
     * 设置时间范围查询条件
     */
    private void setTimeRangeCondition(QueryWrapper<User> queryWrapper, UserExportRequest request) {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);

        // 根据类型设置时间范围
        switch (request.getType()) {
            case 1: // 天
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 2: // 周
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 3: // 月
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 4: // 年
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 5: // 自定义
                if (request.getStartTime() != null) {
                    queryWrapper.ge("createTime", request.getStartTime());
                }
                if (request.getEndTime() != null) {
                    queryWrapper.le("createTime", request.getEndTime());
                }
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的导出类型");
        }
    }

    @Override
    public UserPublicVO getUserPublicInfo(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 查询用户信息
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        return UserPublicVO.objToVo(user);
    }

    @Override
    public void addUserViewRecord(long userId, long visitorId, HttpServletRequest request) {
        try {
            ViewRecordAddRequest viewRecordAddRequest = new ViewRecordAddRequest();
            viewRecordAddRequest.setUserId(visitorId); // 访客ID
            viewRecordAddRequest.setTargetId(userId); // 被浏览的用户ID
            viewRecordAddRequest.setTargetType(4); // 4-用户



            viewRecordService.addViewRecord(viewRecordAddRequest, request);
        } catch (Exception e) {
            log.error("添加用户浏览记录失败", e);
        }
    }

    @Override
    public boolean secureUserDestroy(long userId, String userPassword, String code) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 获取用户信息
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 3. 验证用户密码
        String encryptPassword = getEncryptPassword(userPassword);
        if (!encryptPassword.equals(user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 4. 验证邮箱验证码
        String verifyCodeKey = String.format("email:code:verify:userDestroy:%s", user.getEmail());
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        // 5. 执行用户数据删除
        asyncDeleteUserData(userId);

        // 6. 删除验证码
        stringRedisTemplate.delete(verifyCodeKey);

        return true;
    }

    @Override
    public boolean updateUserPermissions(Long userId, Integer allowPrivateChat, Integer allowFollow, Integer showFollowList, Integer showFansList) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 验证参数的有效性
        List<Integer> validValues = Arrays.asList(0, 1);
        if (allowPrivateChat != null && !validValues.contains(allowPrivateChat)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许私聊参数值非法");
        }
        if (allowFollow != null && !validValues.contains(allowFollow)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许被关注参数值非法");
        }
        if (showFollowList != null && !validValues.contains(showFollowList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "展示关注列表参数值非法");
        }
        if (showFansList != null && !validValues.contains(showFansList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "展示粉丝列表参数值非法");
        }

        // 构建更新对象
        User updateUser = new User();
        updateUser.setId(userId);

        if (allowPrivateChat != null) {
            updateUser.setAllowPrivateChat(allowPrivateChat);
        }
        if (allowFollow != null) {
            updateUser.setAllowFollow(allowFollow);
        }
        if (showFollowList != null) {
            updateUser.setShowFollowList(showFollowList);
        }
        if (showFansList != null) {
            updateUser.setShowFansList(showFansList);
        }

        // 执行更新
        boolean result = this.updateById(updateUser);

        if (result) {
            log.info("用户权限更新成功，用户ID：{}", userId);
        } else {
            log.error("用户权限更新失败，用户ID：{}", userId);
        }

        return result;
    }

    @Override
    public boolean updateUserMultiDeviceLogin(Long userId, Integer allowMultiDeviceLogin) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 验证参数的有效性
        List<Integer> validValues = Arrays.asList(0, 1);
        if (allowMultiDeviceLogin != null && !validValues.contains(allowMultiDeviceLogin)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许多设备登录参数值非法");
        }

        // 使用Mapper直接更新
        int result = userMapper.updateAllowMultiDeviceLogin(userId, allowMultiDeviceLogin);

        if (result > 0) {
            log.info("用户多设备登录设置更新成功，用户ID：{}，设置值：{}", userId, allowMultiDeviceLogin);

            // 直接处理用户设置更新
            if (allowMultiDeviceLogin != null && allowMultiDeviceLogin == 0) {
                // 用户设置为不允许多设备登录，立即踢掉该用户除当前会话外的所有登录
                log.info("开始处理用户 {} 的不允许多设备登录设置", userId);

                // 获取当前操作的token（如果存在）
                String currentToken = null;
                if (StpKit.SPACE.getLoginIdDefaultNull() != null &&
                        StpKit.SPACE.getLoginIdAsLong() == userId) {
                    currentToken = StpKit.SPACE.getTokenValue();
                    log.info("当前操作用户 {} 的Token: {}", userId, currentToken);
                }

                // 获取该用户的所有登录token列表 - 使用StpKit.SPACE确保一致性
                java.util.List<String> tokenList = StpKit.SPACE.getTokenValueListByLoginId(userId);
                log.info("在踢人前，用户 {} 共有 {} 个登录会话", userId, tokenList.size());

                // 踢掉该用户除当前会话外的所有登录会话
                if (tokenList.size() > 0) {
                    if (currentToken != null && tokenList.size() > 1) {
                        // 如果有当前token且存在多个会话，只踢掉其他会话，保留当前会话
                        log.info("踢掉用户 {} 的其他 {} 个登录会话，保留当前会话", userId, tokenList.size() - 1);
                        for (String token : tokenList) {
                            if (!token.equals(currentToken)) {
                                StpKit.SPACE.logoutByTokenValue(token);
                                log.info("用户 {} 不允许多设备登录，已踢出其他登录设备，Token: {}", userId, token);
                            }
                        }
                    } else {
                        // 如果没有当前token或只有一个会话，踢掉所有会话
                        log.info("踢掉用户 {} 的所有 {} 个登录会话", userId, tokenList.size());
                        for (String token : tokenList) {
                            StpKit.SPACE.logoutByTokenValue(token);
                            log.info("用户 {} 不允许多设备登录，已踢出登录设备，Token: {}", userId, token);
                        }
                    }
                } else {
                    log.info("用户 {} 没有活跃的登录会话，无需踢人操作", userId);
                }

                // 确保所有会话都被清理，等待片刻让异步操作完成
                try {
                    Thread.sleep(100); // 短暂等待确保踢人操作完成
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return true;
        } else {
            log.error("用户多设备登录设置更新失败，用户ID：{}", userId);
            return false;
        }
    }

    @Override
    public Integer getUserMultiDeviceLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 使用Mapper查询
        return userMapper.getAllowMultiDeviceLogin(userId);
    }

    @Override
    public LoginUserVO userLoginByWxOpenId(String openId, HttpServletRequest request) {
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少微信识别码");
        }

        // 2. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mpOpenId", openId);
        User user = this.getOne(queryWrapper);

        // 3. 用户不存在，则自动注册
        if (user == null) {
            user = new User();
            user.setMpOpenId(openId);
            user.setUserAccount("wx_" + RandomUtil.randomString(8));
            user.setUserName("微信用户_" + RandomUtil.randomString(4));
            user.setUserAvatar(DefaultAvatarUtil.getRandomAvatar());
            user.setUserRole(UserRoleEnum.USER.getValue());
            // 统一设置默认密码为 12345678
            user.setUserPassword(getEncryptPassword(CommonValue.DEFAULT_PASSWORD));
            boolean result = this.save(user);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "快捷注册失败");
            }
        }

        // 4. 检查是否被封禁
        if (CrawlerConstant.BAN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该账号已被封禁");
        }

        // 5. 记录登录态
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        return getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByWxCode(String code, HttpServletRequest request) {
        // 1. 校验验证码获取 openId
        String openId = wxLoginManager.getOpenIdByCode(code);
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        LoginUserVO loginUserVO = this.userLoginByWxOpenId(openId, request);

        // 6. 成功后删除验证码
        wxLoginManager.removeCode(code);

        return loginUserVO;
    }

    @Override
    public boolean userBindWxByOpenId(String openId, HttpServletRequest request) {
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少微信识别码");
        }

        // 2. 获取当前登录用户
        User loginUser = this.getLoginUser(request);
        Long userId = loginUser.getId();

        // 3. 唯一性校验：当前账号是否已绑定微信
        if (StrUtil.isNotBlank(loginUser.getMpOpenId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账号已绑定微信，请勿重复绑定");
        }

        // 4. 唯一性校验：该微信是否已被其他账号绑定
        synchronized (openId.intern()) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("mpOpenId", openId);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该微信已绑定其他账号，请先解绑或换个微信");
            }

            // 5. 更新绑定信息
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setMpOpenId(openId);
            boolean result = this.updateById(updateUser);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "绑定失败，数据库错误");
            }

            // 6. 成功后更新 Session 中的用户信息
            loginUser.setMpOpenId(openId);
            StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, loginUser);

            return true;
        }
    }

    @Override
    public boolean userBindWx(String code, HttpServletRequest request) {
        // 1. 校验验证码获取 openId
        String openId = wxLoginManager.getOpenIdByCode(code);
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }

        boolean result = this.userBindWxByOpenId(openId, request);

        // 删除验证码
        if (result) {
            wxLoginManager.removeCode(code);
        }

        return result;
    }

    @Override
    public boolean userUnbindWx(HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = this.getLoginUser(request);
        Long userId = loginUser.getId();

        // 2. 检查是否已绑定微信
        if (StrUtil.isBlank(loginUser.getMpOpenId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您的账号尚未绑定微信，无需解绑");
        }

        // 3. 显式更新 mpOpenId 为 null (解决 MyBatis-Plus 默认忽略 null 的问题)
        boolean result = this.lambdaUpdate()
                .set(User::getMpOpenId, null)
                .eq(User::getId, userId)
                .update();

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解绑失败，数据库错误");
        }

        // 4. 更新 Session 中的用户信息
        loginUser.setMpOpenId(null);
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, loginUser);

        log.info("用户解绑微信成功，用户ID：{}", userId);
        return true;
    }
}
