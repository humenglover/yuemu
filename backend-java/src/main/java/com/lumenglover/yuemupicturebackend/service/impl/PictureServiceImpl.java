package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.api.aliyunai.AliYunAiApi;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.manager.auth.SpaceUserAuthManager;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.manager.upload.FilePictureUpload;
import com.lumenglover.yuemupicturebackend.manager.upload.PictureUploadTemplate;
import com.lumenglover.yuemupicturebackend.manager.upload.UrlPictureUpload;
import com.lumenglover.yuemupicturebackend.mapper.ChatMessageMapper;
import com.lumenglover.yuemupicturebackend.mapper.PictureMapper;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import java.util.HashSet;
import java.util.Set;
import com.lumenglover.yuemupicturebackend.model.dto.picture.*;
import com.lumenglover.yuemupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.enums.OperationEnum;
import com.lumenglover.yuemupicturebackend.model.enums.PictureReviewStatusEnum;
import com.lumenglover.yuemupicturebackend.model.vo.PictureTagCategory;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.model.entity.LikeRecord;
import com.lumenglover.yuemupicturebackend.model.entity.FavoriteRecord;
import com.lumenglover.yuemupicturebackend.model.entity.ViewRecord;
import com.lumenglover.yuemupicturebackend.utils.ColorSimilarUtils;
import com.lumenglover.yuemupicturebackend.utils.ColorTransformUtils;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import com.lumenglover.yuemupicturebackend.utils.SystemNotifyUtil;
import com.lumenglover.yuemupicturebackend.utils.TencentCloudImageAuditUtil;
import com.lumenglover.yuemupicturebackend.utils.SensitiveUtil;
import com.lumenglover.yuemupicturebackend.utils.PicturePermissionUtils;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import lombok.extern.slf4j.Slf4j;
import com.lumenglover.yuemupicturebackend.model.dto.viewrecord.ViewRecordAddRequest;
import com.lumenglover.yuemupicturebackend.service.ViewRecordService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import com.lumenglover.yuemupicturebackend.manager.auth.StpKit;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2024-12-11 20:45:51
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private PicturePermissionUtils picturePermissionUtils;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Autowired
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private UserFollowsService userfollowsService;

    @Resource
    private TagService tagService;

    @Resource
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    @Lazy
    private ShareRecordService shareRecordService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private CrawlerManager crawlerManager;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private TencentCloudImageAuditUtil tencentCloudImageAuditUtil;

    @Resource
    @Lazy
    private FavoriteRecordService favoriteRecordService;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Resource
    private SensitiveUtil sensitiveUtil;

    @Resource
    private ViewRecordService viewRecordService;

    @Resource
    private YoloService yoloService;



    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId（这样也兼容了公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原图片的空间 id 一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId); // 指定空间 id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        // 对公共图片的名称进行敏感词过滤
        if (spaceId == null) { // 公共图片
            picName = SensitiveUtil.filter(picName);
            if (picName == null) {
                picName = "未命名图片"; // 防止名称为null
            }
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setCategory(pictureUploadRequest.getCategoryName());
        // 对公共图片的标签进行敏感词过滤
        String tags = pictureUploadRequest.getTagName();
        if (spaceId == null && StrUtil.isNotBlank(tags)) { // 公共图片
            tags = SensitiveUtil.filter(tags);
            if (tags == null) {
                tags = pictureUploadRequest.getTagName(); // 如果过滤后为null，使用原始值
            }
        }
        picture.setTags(tags);
        // 对公共图片的简介进行敏感词过滤
        String introduction = pictureUploadRequest.getIntroduction();
        if (spaceId == null && StrUtil.isNotBlank(introduction)) { // 公共图片
            introduction = SensitiveUtil.filter(introduction);
            if (introduction == null) {
                introduction = pictureUploadRequest.getIntroduction(); // 如果过滤后为null，使用原始值
            }
        }
        picture.setIntroduction(introduction);
        picture.setUserId(loginUser.getId());
        // 设置草稿状态：只有公共空间上传时才设置为草稿状态，个人空间和团队空间直接设置为非草稿状态
        if (spaceId == null) {
            picture.setIsDraft(1); // 公共空间默认为草稿状态
        } else {
            picture.setIsDraft(0); // 个人空间和团队空间直接设置为非草稿状态
        }
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        if (picture == null) {
            return null;
        }

        // 增加浏览量
        incrementViewCount(picture.getId(), request);

        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 设置实时浏览量
        pictureVO.setViewCount(getViewCount(picture.getId()));

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }

        // 设置权限相关字段
        pictureVO.setAllowCollect(picture.getAllowCollect() != null && picture.getAllowCollect() == 1);
        pictureVO.setAllowLike(picture.getAllowLike() != null && picture.getAllowLike() == 1);
        pictureVO.setAllowComment(picture.getAllowComment() != null && picture.getAllowComment() == 1);
        pictureVO.setAllowShare(picture.getAllowShare() != null && picture.getAllowShare() == 1);

        // 设置点赞状态 - 使用新的通用点赞表
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            // 使用 LikeRecordService 的方法来检查点赞状态
            boolean isLiked = likeRecordService.isContentLiked(picture.getId(), 1, loginUser.getId());
            pictureVO.setIsLiked(isLiked ? 1 : 0);
            // 获取分享状态
            boolean isShared = shareRecordService.isContentShared(picture.getId(), 1, loginUser.getId());
            pictureVO.setIsShared(isShared ? 1 : 0);

            // 获取收藏状态
            boolean isFavorited = favoriteRecordService.hasFavorited(loginUser.getId(), picture.getId(), 1); // 1表示图片类型
            pictureVO.setIsFavorited(isFavorited ? 1 : 0);

            // 自动添加浏览记录
            try {
                addPictureViewRecord(picture.getId(), loginUser.getId(), request);
            } catch (Exception e) {
                log.error("添加图片浏览记录失败", e);
            }
        } else {
            pictureVO.setIsLiked(0);
            pictureVO.setIsShared(0);
            pictureVO.setIsFavorited(0);
        }

        return pictureVO;
    }

    /**
     * 获取图片VO（内部使用）
     */
    private PictureVO getPictureVOInternal(Picture picture, User loginUser, Map<Long, User> userMap) {
        if (picture == null) {
            return null;
        }

        // 增加浏览量
        incrementViewCount(picture.getId(), null);

        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 设置实时浏览量
        pictureVO.setViewCount(getViewCount(picture.getId()));

        //优化首页查询性能，只保留浏览量
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0 && userMap != null) {
            User user = userMap.get(userId);
            if (user != null) {
                UserVO userVO = userService.getUserVO(user);
                pictureVO.setUser(userVO);
            }
        }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        // 获取登录用户，可以是登录，可以未登录
        User loginUser = userService.isLogin(request);

        // 批量获取用户ID列表
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = userIdSet.isEmpty() ? new HashMap<>() :
                userService.listByIds(new ArrayList<>(userIdSet)).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));

        // 只查询图片和用户信息，不查询点赞、收藏等互动属性
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> getPictureVOWithoutInteraction(picture, loginUser, userMap))
                .collect(Collectors.toList());

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 获取图片VO（只包含基本信息和用户信息，无互动属性）
     */
    private PictureVO getPictureVOWithoutInteraction(Picture picture, User loginUser, Map<Long, User> userMap) {
        if (picture == null) {
            return null;
        }

        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 设置实时浏览量
        pictureVO.setViewCount(getViewCount(picture.getId()));

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0 && userMap != null) {
            User user = userMap.get(userId);
            if (user != null) {
                UserVO userVO = userService.getUserVO(user);
                pictureVO.setUser(userVO);
            }
        }

        // 设置权限相关字段
        pictureVO.setAllowCollect(picture.getAllowCollect() != null && picture.getAllowCollect() == 1);
        pictureVO.setAllowLike(picture.getAllowLike() != null && picture.getAllowLike() == 1);
        pictureVO.setAllowComment(picture.getAllowComment() != null && picture.getAllowComment() == 1);
        pictureVO.setAllowShare(picture.getAllowShare() != null && picture.getAllowShare() == 1);

        // 不设置点赞、分享、收藏等互动状态，直接设置为默认值
        pictureVO.setIsLiked(0);
        pictureVO.setIsShared(0);
        pictureVO.setIsFavorited(0);

        return pictureVO;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        // 排除草稿状态的数据
        queryWrapper.eq("isDraft", 0);

        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复，已是改状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 4. 数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());

        // 如果审核通过，将创建时间更新为当前时间
        if (Integer.valueOf(PictureReviewStatusEnum.PASS.getValue()).equals(reviewStatus)) {
            updatePicture.setCreateTime(new Date());
        }

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 5. 发送系统通知
        if (Integer.valueOf(PictureReviewStatusEnum.PASS.getValue()).equals(reviewStatus)) {
            // 审核通过通知
            SystemNotifyUtil.sendPictureApprovedNotify(oldPicture.getUserId(), id, oldPicture.getName());
        } else if (Integer.valueOf(PictureReviewStatusEnum.REJECT.getValue()).equals(reviewStatus)) {
            // 审核不通过通知
            SystemNotifyUtil.sendPictureRejectedNotify(oldPicture.getUserId(), id, oldPicture.getName(), reviewMessage);
        }


    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            // 管理员上传的图片默认为非草稿状态
            picture.setIsDraft(0);
            picture.setReviewTime(new Date());
        } else {
            // 非管理员上传时，先设置为草稿状态，不进行审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            picture.setIsDraft(1); // 上传时设置为草稿状态
            picture.setReviewMessage("待发布审核");
        }
    }

    /**
     * 使用腾讯云图片审核服务进行自动审核
     * @param picture 图片实体
     * @param loginUser 当前登录用户
     */
    private void performTencentCloudImageAudit(Picture picture, User loginUser) {
        try {
            // 对公共图片的名称和简介进行敏感词过滤
            if (picture.getSpaceId() == null) { // 公共图片
                String filteredName = SensitiveUtil.filter(picture.getName());
                if (filteredName != null && !Objects.equals(filteredName, picture.getName())) {
                    picture.setName(filteredName);
                    log.info("图片名称已过滤，ID: {}", picture.getId());
                }

                String filteredIntroduction = SensitiveUtil.filter(picture.getIntroduction());
                if (filteredIntroduction != null && !Objects.equals(filteredIntroduction, picture.getIntroduction())) {
                    picture.setIntroduction(filteredIntroduction);
                    log.info("图片简介已过滤，ID: {}", picture.getId());
                }

                String filteredTags = SensitiveUtil.filter(picture.getTags());
                if (filteredTags != null && !Objects.equals(filteredTags, picture.getTags())) {
                    picture.setTags(filteredTags);
                    log.info("图片标签已过滤，ID: {}", picture.getId());
                }
            }

            // 获取审核策略类型，可以从配置中读取
            String bizType = cosClientConfig.getAuditBizType();
            if (bizType == null) {
                bizType = ""; // 默认为空，使用系统默认审核策略
            } // 从配置中获取具体的审核策略类型

            // 对图片进行审核
            ImageAuditingResponse auditResponse =
                    tencentCloudImageAuditUtil.auditImageByUrl(picture.getUrl(), bizType);

            boolean isCompliant = tencentCloudImageAuditUtil.isImageCompliant(auditResponse);
            String auditLabel = tencentCloudImageAuditUtil.getAuditLabel(auditResponse);
            Integer auditScore = tencentCloudImageAuditUtil.getAuditScore(auditResponse);

            if (isCompliant) {
                // 图片合规，自动通过审核
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewMessage("腾讯云图片审核: 自动通过 (标签: " + auditLabel + ", 分数: " + auditScore + ")");
                picture.setReviewerId(0L); // 系统审核
                picture.setReviewTime(new Date());
                // 图片合规，设置为非草稿状态
                picture.setIsDraft(0);
                log.info("图片审核通过，ID: {}, URL: {}, 标签: {}, 分数: {}",
                        picture.getId(), picture.getUrl(), auditLabel, auditScore);



                // 发送系统通知给用户，告知审核通过
                try {
                    SystemNotifyUtil.sendPictureApprovedNotify(picture.getUserId(),
                            picture.getId(), picture.getName());
                } catch (Exception e) {
                    log.error("发送审核通过系统通知失败，用户ID: {}, 图片ID: {}",
                            picture.getUserId(), picture.getId(), e);
                }
            } else {
                // 图片不合规，需要人工审核
                picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
                picture.setReviewMessage("腾讯云图片审核: 疑似违规 (标签: " + auditLabel + ", 分数: " + auditScore + ", 需要人工审核)");
                log.info("图片审核未通过，需要人工审核，ID: {}, URL: {}, 标签: {}, 分数: {}",
                        picture.getId(), picture.getUrl(), auditLabel, auditScore);

                // 发送邮件通知管理员进行人工审核
                // 如果图片ID为null，说明是新增图片，先保存到数据库再发送通知
                if (picture.getId() == null) {
                    // 保存图片到数据库以获取ID
                    this.save(picture);
                }
                notifyAdminForManualReview(picture, auditLabel, auditScore);
            }
        } catch (Exception e) {
            log.error("腾讯云图片审核服务调用失败，图片ID: {}, URL: {}, 错误: {}",
                    picture.getId(), picture.getUrl(), e.getMessage());

            // 如果审核服务调用失败，设置为待审核状态，由人工审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            picture.setReviewMessage("图片审核服务异常，需要人工审核");

            // 发送邮件通知管理员
            // 如果图片ID为null，说明是新增图片，先保存到数据库再发送通知
            if (picture.getId() == null) {
                // 保存图片到数据库以获取ID
                this.save(picture);
            }
            notifyAdminForManualReview(picture, "审核服务异常", 0);
        }
    }

    /**
     * 发送邮件通知管理员进行人工审核
     * @param picture 图片实体
     * @param auditLabel 审核标签
     * @param auditScore 审核分数
     */
    private void notifyAdminForManualReview(Picture picture, String auditLabel, Integer auditScore) {
        // 发送邮件通知管理员
        log.info("发送通知给管理员，图片需要人工审核，图片ID: {}, 标签: {}, 分数: {}",
                picture.getId(), auditLabel, auditScore);

        try {
            // 从配置中获取管理员邮箱
            String adminEmail = getAdminEmail();
            if (adminEmail != null && !adminEmail.isEmpty()) {
                String htmlContent = buildReviewNotificationContent(picture, auditLabel, auditScore);
                emailSenderUtil.sendReviewEmail(adminEmail, htmlContent);
                log.info("审核通知邮件已发送给管理员: {}", adminEmail);
            } else {
                log.warn("未配置管理员邮箱，无法发送审核通知");
            }
        } catch (Exception e) {
            log.error("发送审核通知邮件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建审核通知邮件内容
     * @param picture 图片信息
     * @param auditLabel 审核标签
     * @param auditScore 审核分数
     * @return 邮件HTML内容
     */
    private String buildReviewNotificationContent(Picture picture, String auditLabel, Integer auditScore) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>图片审核通知</h2>");
        content.append("<p><strong>图片ID:</strong> ").append(picture.getId()).append("</p>");

        // 获取上传用户信息
        User user = userService.getById(picture.getUserId());
        if (user != null) {
            content.append("<p><strong>上传用户ID:</strong> ").append(picture.getUserId()).append("</p>");
            content.append("<p><strong>上传用户名:</strong> ").append(user.getUserName()).append("</p>");
            content.append("<p><strong>上传用户邮箱:</strong> ").append(user.getEmail()).append("</p>");
            content.append("<p><strong>上传用户角色:</strong> ").append(user.getUserRole()).append("</p>");
        } else {
            content.append("<p><strong>上传用户ID:</strong> ").append(picture.getUserId()).append("</p>");
        }

        content.append("<p><strong>图片名称:</strong> ").append(picture.getName() != null ? picture.getName() : "未命名").append("</p>");
        content.append("<p><strong>图片URL:</strong> <a href=\"").append(picture.getUrl()).append("\" target=\"_blank\">点击查看</a></p>");
        content.append("<p><strong>审核标签:</strong> ").append(auditLabel).append("</p>");
        content.append("<p><strong>审核分数:</strong> ").append(auditScore).append("</p>");
        content.append("<p><strong>上传时间:</strong> ").append(picture.getCreateTime()).append("</p>");
        content.append("<p><strong>图片简介:</strong> ").append(picture.getIntroduction() != null ? picture.getIntroduction() : "无").append("</p>");
        content.append("<p><strong>图片分类:</strong> ").append(picture.getCategory() != null ? picture.getCategory() : "无").append("</p>");
        content.append("<p><strong>图片标签:</strong> ").append(picture.getTags() != null ? picture.getTags() : "无").append("</p>");
        content.append("<p><strong>空间ID:</strong> ").append(picture.getSpaceId() != null ? picture.getSpaceId() : "公共空间").append("</p>");

        content.append("<p>请登录系统进行人工审核。</p>");

        return content.toString();
    }

    /**
     * 获取管理员邮箱
     * @return 管理员邮箱地址
     */
    private String getAdminEmail() {
        // 从配置中获取管理员邮箱，从application配置中读取
        return cosClientConfig.getAdminEmail();
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            pictureUploadRequest.setCategoryName(pictureUploadByBatchRequest.getCategoryName());
            pictureUploadRequest.setTagName(JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTagName()));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationPicture(PictureOperation pictureOperation) {
        //获取批量操作类型
        long operationType = pictureOperation.getOperationType();
        //获取批量操作图片id
        List<Long> pictureIds = pictureOperation.getIds();
        boolean result = false;

        //批量删除
        if (operationType == OperationEnum.DELETE.getValue()) {
            //删除图片
            List<Picture> pictureList = listByIds(pictureIds);
            ThrowUtils.throwIf(pictureList == null || pictureList.isEmpty(), ErrorCode.NOT_FOUND_ERROR);

            result = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                try {
                    // 批量删除MySQL数据
                    boolean deleteResult = removeByIds(pictureIds);
                    if (!deleteResult) {
                        return false;
                    }

                    // 删除图片文件
                    for (Picture oldPicture : pictureList) {
                        this.clearPictureFile(oldPicture);
                    }

                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            }));
        }
        //批量通过或不通过
        else if (Integer.valueOf(OperationEnum.APPROVE.getValue()).equals(Long.valueOf(operationType).intValue()) ||
                Integer.valueOf(OperationEnum.REJECT.getValue()).equals(Long.valueOf(operationType).intValue())) {
            // 设置审核状态
            Integer reviewStatus = Integer.valueOf(OperationEnum.APPROVE.getValue()).equals(Long.valueOf(operationType).intValue()) ?
                    PictureReviewStatusEnum.PASS.getValue() :
                    PictureReviewStatusEnum.REJECT.getValue();

            // 更新 MySQL 数据
            result = update()
                    .set("reviewStatus", reviewStatus)
                    .set("reviewTime", new Date())
                    .set("reviewMessage", Integer.valueOf(OperationEnum.APPROVE.getValue()).equals(Long.valueOf(operationType).intValue()) ?
                            "批量审核通过" : "批量审核不通过")
                    .in("id", pictureIds)
                    .update();
        }

        return result;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        if (oldPicture == null) {
            // 若 oldPicture 为 null，直接返回，避免空指针异常
            return;
        }
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();

        // 预上传状态(-3)和帖子图片(-1)只需验证用户所有权
        if (spaceId != null && (spaceId == -3L || spaceId == -1L)) {
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            return;
        }

        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            try {
                // 操作数据库
                boolean result = this.removeById(pictureId);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

                // 更新空间的使用额度，释放额度
                if(finalSpaceId != null){
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, oldPicture.getSpaceId())
                            .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                            .setSql("totalCount = totalCount - 1")
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }



                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Long pictureId = pictureEditRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 检查权限 - 修正参数数量
        checkPictureAuth(loginUser, oldPicture);

        // 创建新的图片对象
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        // 设置默认值
        if (picture.getIsDownload() == null) {
            picture.setIsDownload(1);  // 默认允许下载
        }

        // 保留原有数据中的一些字段
        picture.setUrl(oldPicture.getUrl());
        picture.setThumbnailUrl(oldPicture.getThumbnailUrl());
        picture.setPicSize(oldPicture.getPicSize());
        picture.setPicWidth(oldPicture.getPicWidth());
        picture.setPicHeight(oldPicture.getPicHeight());
        picture.setPicScale(oldPicture.getPicScale());
        picture.setPicFormat(oldPicture.getPicFormat());
        picture.setPicColor(oldPicture.getPicColor());
        picture.setUserId(oldPicture.getUserId());
        picture.setSpaceId(oldPicture.getSpaceId());
        picture.setCreateTime(oldPicture.getCreateTime());

        // 对公共图片的名称、简介和标签进行敏感词过滤
        if (picture.getSpaceId() == null) { // 公共图片
            String filteredName = SensitiveUtil.filter(picture.getName());
            if (filteredName != null && !Objects.equals(filteredName, picture.getName())) {
                picture.setName(filteredName);
                log.info("编辑图片名称已过滤，ID: {}", picture.getId());
            }

            String filteredIntroduction = SensitiveUtil.filter(picture.getIntroduction());
            if (filteredIntroduction != null && !Objects.equals(filteredIntroduction, picture.getIntroduction())) {
                picture.setIntroduction(filteredIntroduction);
                log.info("编辑图片简介已过滤，ID: {}", picture.getId());
            }

            if (picture.getTags() != null) {
                String filteredTags = SensitiveUtil.filter(picture.getTags());
                if (filteredTags != null && !Objects.equals(filteredTags, picture.getTags())) {
                    picture.setTags(filteredTags);
                    log.info("编辑图片标签已过滤，ID: {}", picture.getId());
                }
            }
        }

        // 设置编辑时间
        picture.setEditTime(new Date());

        // 先更新图片到数据库
        boolean success = this.updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 检查是否是系统管理员
        if (userService.isAdmin(loginUser)) {
            // 系统管理员直接放行
        } else if (loginUser.getId().equals(space.getUserId())) {
            // 空间所有者直接放行
        } else {
            // 检查用户在空间中的状态，如果是待审核状态，则不允许访问
            SpaceUser spaceUser =
                    spaceUserService.getOne(spaceUserService.getQueryWrapper(
                            new SpaceUserQueryRequest() {{
                                setSpaceId(spaceId);
                                setUserId(loginUser.getId());
                            }}
                    ));

            if (spaceUser != null && spaceUser.getStatus() != null && spaceUser.getStatus() == 0) {
                // 用户状态为0表示待审核，抛出权限错误
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户正在审核中，暂无权限访问空间内容");
            }

            // 检查查看权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权查看该空间的图片");
        }
        // 3. 查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .eq(Picture::getIsDraft, 0) // 排除草稿状态的数据
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());
        // 5. 返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }

        // 3. 查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .eq(Picture::getIsDraft, 0) // 排除草稿状态的数据
                .list();
        if (pictureList.isEmpty()) {
            return;
        }

        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);

        // 5. 操作数据库进行批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");


    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    @Override
    public void crawlerDetect(HttpServletRequest request) {
        crawlerManager.detectNormalRequest(request);
    }

    @Override
    public List<PictureVO> getTop100Picture(Long id) {
        String cacheKey = RedisConstant.TOP_100_PIC_REDIS_KEY_PREFIX + id;

        // 尝试从缓存获取
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                // 反序列化缓存数据
                List<PictureVO> cachedList = JSONUtil.toList(cachedJson, PictureVO.class);
                return cachedList;
            } catch (Exception e) {
                log.warn("获取图片榜单缓存失败: {}", e.getMessage());
            }
        }

        // 缓存未命中，查询数据库
        List<Picture> pictureList = getTop100PictureList(id);

        // 如果没有图片数据，直接返回空列表
        if (pictureList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取浏览量
        Map<Long, Long> viewCountMap = new HashMap<>();
        List<String> viewCountKeys = pictureList.stream()
                .map(picture -> String.format("picture:viewCount:%d", picture.getId()))
                .collect(Collectors.toList());
        if (!viewCountKeys.isEmpty()) {
            List<String> redisViewCounts = stringRedisTemplate.opsForValue().multiGet(viewCountKeys);
            for (int i = 0; i < pictureList.size(); i++) {
                Picture picture = pictureList.get(i);
                String redisCount = redisViewCounts.get(i);
                long baseCount = picture.getViewCount() != null ? picture.getViewCount() : 0L;
                long increment = redisCount != null ? Long.parseLong(redisCount) : 0L;
                viewCountMap.put(picture.getId(), baseCount + increment);
            }
        }

        // 批量获取用户信息
        Set<Long> userIds = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 只在有用户ID时才查询用户信息
        Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));
        } else {
            userMap = Collections.emptyMap();
        }

        List<PictureVO> result = pictureList.stream()
                .map(picture -> {
                    PictureVO pictureVO = PictureVO.objToVo(picture);
                    // 设置实时浏览量
                    pictureVO.setViewCount(viewCountMap.getOrDefault(picture.getId(), 0L));
                    // 设置用户信息
                    Long userId = picture.getUserId();
                    if (userId != null && userId > 0) {
                        User user = userMap.get(userId);
                        if (user != null) {
                            UserVO userVO = userService.getUserVO(user);
                            pictureVO.setUser(userVO);
                        }
                    }
                    // 默认未点赞
                    pictureVO.setIsLiked(0);
                    return pictureVO;
                })
                .collect(Collectors.toList());

        // 将结果存入缓存，设置过期时间
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result),
                    RedisConstant.TOP_100_PIC_REDIS_KEY_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("设置图片榜单缓存失败: {}", e.getMessage());
        }

        return result;
    }

    @Transactional
    @Override
    public Page<PictureVO> getFollowPicture(HttpServletRequest request, PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, size);

        // 查询是否登录
        User currentUser = userService.getLoginUser(request);

        // 处理用户未登录的情况
        if (currentUser == null) {
            return new Page<>();
        }

        // 获取用户 id
        Long id = currentUser.getId();

        // 获取关注列表
        List<Long> followList = userfollowsService.getFollowList(id);

        // 确保 followList 不为空且不包含 null 元素
        followList = followList.stream()
                .filter(item -> item!= null)
                .collect(Collectors.toList());

        if (followList.isEmpty()) {
            return new Page<>();
        }

        // 创建 QueryWrapper 筛选出 userId 在关注列表中的图片
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("userId", followList)
                .eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                .and(wrap -> wrap.isNull("spaceId").or().eq("spaceId", 0))
                .eq("isDraft", 0) // 排除草稿状态的数据
                .orderByDesc("createTime");

        // 获取图片列表
        Page<Picture> picturePage = this.page(page, queryWrapper);
        List<Picture> pictureList = picturePage.getRecords();

        // 批量获取用户ID列表
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = userIdSet.isEmpty() ? new HashMap<>() :
                userService.listByIds(new ArrayList<>(userIdSet)).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));

        // 将 Picture 列表转换为 PictureVO 列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> getPictureVOInternal(picture, currentUser, userMap))
                .collect(Collectors.toList());

        Page<PictureVO> pictureVOPage = new Page<>(current, size, picturePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public PictureVO uploadPostPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 设置帖子图片的特殊属性
        if (pictureUploadRequest == null) {
            pictureUploadRequest = new PictureUploadRequest();
        }
        pictureUploadRequest.setSpaceId(-1L);  // 使用 -1 表示帖子图片

        // 上传图片，得到图片信息
        String uploadPathPrefix = String.format("post/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(-1L);  // 指定为帖子图片
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setCategory(pictureUploadRequest.getCategoryName());
        // 对帖子图片的标签进行敏感词过滤
        String tags = pictureUploadRequest.getTagName();
        if (StrUtil.isNotBlank(tags)) {
            tags = SensitiveUtil.filter(tags);
            if (tags == null) {
                tags = pictureUploadRequest.getTagName(); // 如果过滤后为null，使用原始值
            }
        }
        picture.setTags(tags);
        // 对帖子图片的简介进行敏感词过滤
        String introduction = pictureUploadRequest.getIntroduction();
        if (StrUtil.isNotBlank(introduction)) {
            introduction = SensitiveUtil.filter(introduction);
            if (introduction == null) {
                introduction = pictureUploadRequest.getIntroduction(); // 如果过滤后为null，使用原始值
            }
        }
        picture.setIntroduction(introduction);
        picture.setUserId(loginUser.getId());
        // 设置默认为草稿状态,帖子默认通过
        picture.setIsDraft(0);

        // 对帖子图片进行机器审核
        try {
            // 获取审核策略类型，可以从配置中读取
            String bizType = cosClientConfig.getAuditBizType();
            if (bizType == null) {
                bizType = ""; // 默认为空，使用系统默认审核策略
            }

            // 对图片进行审核
            ImageAuditingResponse auditResponse =
                    tencentCloudImageAuditUtil.auditImageByUrl(picture.getUrl(), bizType);

            boolean isCompliant = tencentCloudImageAuditUtil.isImageCompliant(auditResponse);
            String auditLabel = tencentCloudImageAuditUtil.getAuditLabel(auditResponse);
            Integer auditScore = tencentCloudImageAuditUtil.getAuditScore(auditResponse);

            if (!isCompliant) {
                log.warn("帖子图片审核未通过，URL: {}, 标签: {}, 分数: {}", picture.getUrl(), auditLabel, auditScore);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像审核未通过: " + auditLabel + " (分数: " + auditScore + ")");
            } else {
                // 图片合规，设置为审核通过状态
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewMessage("帖子图片自动过审: 腾讯云图片审核通过 (标签: " + auditLabel + ", 分数: " + auditScore + ")");
            }
        } catch (Exception e) {
            log.error("帖子图片审核服务调用失败，URL: {}, 错误: {}", picture.getUrl(), e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图像审核服务异常，上传失败");
        }

        // 操作数据库
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            return picture;
        });

        return PictureVO.objToVo(picture);
    }

    private Map<Long, User> getUserMap(List<Picture> pictureList) {
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        // 检查 userIdSet 是否为空
        if (userIdSet.isEmpty()) {
            return null;
        }

        return userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));
    }

    private Map<Long, Boolean> getPictureIdIsLikedMap(User currentUser, List<Picture> pictureList) {
        if (pictureList.isEmpty()) {
            return null;
        }

        Set<Long> pictureIdSet = pictureList.stream()
                .map(Picture::getId)
                .collect(Collectors.toSet());

        // 使用新的通用点赞表查询
        QueryWrapper<LikeRecord> likeQueryWrapper = new QueryWrapper<>();
        likeQueryWrapper.in("targetId", pictureIdSet)
                .eq("userId", currentUser.getId())
                .eq("targetType", 1)  // 1表示图片类型
                .eq("isLiked", true);

        List<LikeRecord> likeRecords = likeRecordService.list(likeQueryWrapper);

        return likeRecords.stream()
                .collect(Collectors.toMap(
                        LikeRecord::getTargetId,
                        like -> true,
                        (b1, b2) -> b1
                ));
    }

    /**
     * 获取图片分享状态映射
     */
    private Map<Long, Boolean> getPictureIdIsSharedMap(User currentUser, List<Picture> pictureList) {
        if (pictureList.isEmpty()) {
            return null;
        }

        Set<Long> pictureIdSet = pictureList.stream()
                .map(Picture::getId)
                .collect(Collectors.toSet());

        // 查询分享记录
        QueryWrapper<ShareRecord> shareQueryWrapper = new QueryWrapper<>();
        shareQueryWrapper.in("targetId", pictureIdSet)
                .eq("userId", currentUser.getId())
                .eq("targetType", 1)  // 1表示图片类型
                .eq("isShared", true);

        List<ShareRecord> shareRecords = shareRecordService.list(shareQueryWrapper);

        return shareRecords.stream()
                .collect(Collectors.toMap(
                        ShareRecord::getTargetId,
                        share -> true,
                        (b1, b2) -> b1
                ));
    }

    /**
     * 填充图片 VO 信息
     */
    private void fillPictureVOInfo(List<PictureVO> pictureVOList, Map<Long, User> userIdUserMap,
                                   Map<Long, Boolean> pictureIdIsLikedMap, Map<Long, Boolean> pictureIdIsSharedMap) {
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            Long pictureId = pictureVO.getId();
            User user = userIdUserMap.get(userId);
            pictureVO.setUser(userService.getUserVO(user));

            // 设置点赞状态
            if (pictureIdIsLikedMap != null) {
                pictureVO.setIsLiked(pictureIdIsLikedMap.getOrDefault(pictureId, false) ? 1 : 0);
            } else {
                pictureVO.setIsLiked(0);
            }

            // 设置分享状态
            if (pictureIdIsSharedMap != null) {
                pictureVO.setIsShared(pictureIdIsSharedMap.getOrDefault(pictureId, false) ? 1 : 0);
            } else {
                pictureVO.setIsShared(0);
            }
        });
    }


    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    /**
     * 增加图片浏览量
     */
    private void incrementViewCount(Long pictureId, HttpServletRequest request) {
        // 检查是否需要增加浏览量
        if (!crawlerManager.detectViewRequest(request, pictureId)) {
            return;
        }

        // 使用 Redis 进行计数
        String viewCountKey = String.format("picture:viewCount:%d", pictureId);
        String lockKey = String.format("picture:viewCount:lock:%d", pictureId);

        try {
            // 获取分布式锁
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 增加浏览量
                stringRedisTemplate.opsForValue().increment(viewCountKey);

                // 当浏览量达到一定阈值时，更新数据库
                String viewCountStr = stringRedisTemplate.opsForValue().get(viewCountKey);
                if (viewCountStr != null && Long.parseLong(viewCountStr) % 100 == 0) {
                    this.update()
                            .setSql("viewCount = viewCount + " + viewCountStr)
                            .eq("id", pictureId)
                            .update();
                    // 更新后重置 Redis 计数
                    stringRedisTemplate.delete(viewCountKey);
                }
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 获取图片浏览量
     */
    @Override
    public long getViewCount(Long pictureId) {
        // 先从 Redis 获取增量
        String viewCountKey = String.format("picture:viewCount:%d", pictureId);
        String incrementCount = stringRedisTemplate.opsForValue().get(viewCountKey);

        // 从数据库获取基础浏览量
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            return 0L;
        }

        // 合并数据库和 Redis 的浏览量
        long baseCount = picture.getViewCount() != null ? picture.getViewCount() : 0L;
        long increment = incrementCount != null ? Long.parseLong(incrementCount) : 0L;

        return baseCount + increment;
    }

    /**
     * 获取 top100 图片列表
     */
    private List<Picture> getTop100PictureList(Long id) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .isNull("spaceId")
                .eq("reviewStatus", 1)
                .eq("isDraft", 0); // 排除草稿状态的数据

        // 根据不同时间范围查询
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);

        switch (id.intValue()) {
            case 1: // 日榜
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 2: // 周榜
                calendar.add(Calendar.WEEK_OF_MONTH, -1);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 3: // 月榜
                calendar.add(Calendar.MONTH, -1);
                queryWrapper.ge("createTime", calendar.getTime());
                break;
            case 4: // 总榜
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 所有榜单类型都使用热榜分数排序，完全去除时间影响
        queryWrapper.orderByDesc("hotScore");

        // 限制返回前100条
        Page<Picture> page = this.page(new Page<>(1, 100), queryWrapper);
        return page.getRecords();
    }

    @Override
    public boolean updatePicture(Picture picture) {
        // 更新数据库
        boolean success = this.updateById(picture);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        return true;
    }

    @Override
    public PictureVO getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        } else {
            // 如果是公共空间图片，需要检查公共空间权限
            User loginUser = null;
            try {
                loginUser = userService.getLoginUser(request);
            } catch (Exception e) {
                // 如果未登录，loginUser保持为null
            }
        }

        // 获取权限列表
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 如果未登录，loginUser保持为null
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);

        // 获取图片VO
        PictureVO pictureVO = this.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);

        // 获取聊天总数
        String chatCountKey = String.format("picture:chatCount:%d", id);
        String cachedChatCount = stringRedisTemplate.opsForValue().get(chatCountKey);

        if (cachedChatCount != null) {
            // 如果缓存存在，直接使用缓存的值
            pictureVO.setChatCount(Long.valueOf(cachedChatCount));
        } else {
            // 缓存不存在，查询数据库
            QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("pictureId", id)
                    .eq("isDelete", 0);
            long chatCount = chatMessageMapper.selectCount(queryWrapper);

            // 更新缓存，设置5分钟过期时间
            stringRedisTemplate.opsForValue().set(
                    chatCountKey,
                    String.valueOf(chatCount),
                    300 + RandomUtil.randomInt(0, 60), // 添加随机过期时间，避免缓存雪崩
                    TimeUnit.SECONDS
            );

            pictureVO.setChatCount(chatCount);
        }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Long spaceId = pictureQueryRequest.getSpaceId();

        // 用户权限校验
        User currentUser = null;
        try {
            currentUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 如果未登录，currentUser保持为null
        }
        if (currentUser != null) {
            String userRole = currentUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        crawlerDetect(request);

        // 处理查询条件
        if (spaceId != null) {
            // 空间内图片查询，需要检查用户在空间中的状态
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 检查是否是系统管理员
            if (currentUser != null && userService.isAdmin(currentUser)) {
                // 系统管理员直接放行
            } else if (currentUser != null && currentUser.getId().equals(space.getUserId())) {
                // 空间所有者直接放行
            } else {
                // 检查用户在空间中的状态，如果是待审核状态，则不允许访问
                if (currentUser != null) {
                    // 检查用户是否为待审核状态
                    User finalCurrentUser = currentUser;
                    SpaceUser spaceUser =
                            spaceUserService.getOne(spaceUserService.getQueryWrapper(
                                    new SpaceUserQueryRequest() {{
                                        setSpaceId(spaceId);
                                        setUserId(finalCurrentUser.getId());
                                    }}
                            ));

                    if (spaceUser != null && spaceUser.getStatus() != null && spaceUser.getStatus() == 0) {
                        // 用户状态为0表示待审核，抛出权限错误
                        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户正在审核中，暂无权限访问空间内容");
                    }
                }

                // 检查查看权限
                boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
                ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权查看该空间的图片");
            }
        } else {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }

        // 构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = RedisConstant.PUBLIC_PIC_REDIS_KEY_PREFIX + hashKey;

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapper(pictureQueryRequest);

        // 如果是公共空间查询（没有指定spaceId），应用权限过滤
        if (spaceId == null) {
            User loginUser = null;
            try {
                loginUser = userService.getLoginUser(request);
            } catch (Exception e) {
                // 如果未登录，loginUser保持为null
            }
            filterPictureQueryByPermission(queryWrapper, loginUser);
        }

        // 查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, size), queryWrapper);
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);

        // 更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return pictureVOPage;
    }

    @Override
    public List<PictureVO> getTop100PictureWithCache(Long id) {
        // 构建缓存key
        String cacheKey = RedisConstant.TOP_100_PIC_REDIS_KEY_PREFIX + id;

        // 尝试从缓存获取
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toList(cachedValue, PictureVO.class);
        }

        // 缓存未命中,查询数据库
        List<Picture> pictureList = this.getTop100PictureList(id);

        // 批量获取用户ID列表
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = userIdSet.isEmpty() ? new HashMap<>() :
                userService.listByIds(new ArrayList<>(userIdSet)).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (u1, u2) -> u1));

        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> this.getPictureVOWithoutInteraction(picture, null, userMap))
                .collect(Collectors.toList());

        // 更新缓存
        int cacheExpireTime = (int) (RedisConstant.TOP_100_PIC_REDIS_KEY_EXPIRE_TIME + RandomUtil.randomInt(0, 6000));
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(pictureVOList),
                cacheExpireTime, TimeUnit.SECONDS);

        return pictureVOList;
    }

    @Override
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        Long spaceId = pictureQueryRequest.getSpaceId();
        Long userId = pictureQueryRequest.getUserId();
        User loginUser = null;

        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户只能看公开图库
            if (spaceId != null || userId != null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
        }

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);

        // 处理不同的查询场景
        if (spaceId != null) {
            // 空间内图片查询
            handleSpaceQuery(spaceId, loginUser, queryWrapper);
        } else if (userId != null) {
            // 用户图片查询
            handleUserQuery(userId, loginUser, queryWrapper);
        } else {
            // 公共图库查询 - 自定义排序逻辑
            handlePublicQueryWithCustomSort(queryWrapper);
            // 对公共查询应用权限过滤
            filterPictureQueryByPermission(queryWrapper, loginUser);
        }

        // 执行查询
        Page<Picture> picturePage = this.page(
                new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                queryWrapper
        );

        // 返回结果
        return getPictureVOPage(picturePage, request);
    }

    /**
     * 处理空间图片查询
     */
    private void handleSpaceQuery(Long spaceId, User loginUser, QueryWrapper<Picture> queryWrapper) {
        // 检查空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 检查是否是系统管理员
        if (loginUser != null && userService.isAdmin(loginUser)) {
            queryWrapper.eq("spaceId", spaceId)
                    .eq("isDraft", 0); // 排除草稿状态的数据
            return; // 系统管理员直接放行
        }

        // 检查是否是空间所有者
        if (loginUser != null && loginUser.getId().equals(space.getUserId())) {
            queryWrapper.eq("spaceId", spaceId)
                    .eq("isDraft", 0); // 排除草稿状态的数据
            return; // 空间所有者直接放行
        }

        // 检查用户在空间中的状态，如果是待审核状态，则不允许访问
        if (loginUser != null) {
            // 检查用户是否为待审核状态
            SpaceUser spaceUser =
                    spaceUserService.getOne(spaceUserService.getQueryWrapper(
                            new SpaceUserQueryRequest() {{
                                setSpaceId(spaceId);
                                setUserId(loginUser.getId());
                            }}
                    ));

            if (spaceUser != null && spaceUser.getStatus() != null && spaceUser.getStatus() == 0) {
                // 用户状态为0表示待审核，抛出权限错误
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户正在审核中，暂无权限访问空间内容");
            }
        }

        // 检查查看权限
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权查看该空间的图片");
        queryWrapper.eq("spaceId", spaceId)
                .eq("isDraft", 0); // 排除草稿状态的数据
    }

    /**
     * 处理用户图片查询
     */
    private void handleUserQuery(Long userId, User loginUser, QueryWrapper<Picture> queryWrapper) {
        // 只能查看自己的图片
        ThrowUtils.throwIf(loginUser == null || !loginUser.getId().equals(userId),
                ErrorCode.NO_AUTH_ERROR, "只能查看自己的图片");
        queryWrapper.eq("userId", userId)
                .isNull("spaceId")
                .eq("isDraft", 0); // 排除草稿状态的数据
    }

    /**
     * 处理公开图库查询 - 自定义排序逻辑
     */
    private void handlePublicQueryWithCustomSort(QueryWrapper<Picture> queryWrapper) {
        // 获取当前用户
        User loginUser = null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            loginUser = userService.isLogin(request);
            // 封禁用户不能查看公共图库
            if (loginUser != null && CrawlerConstant.BAN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "封禁用户禁止查看图片,请联系管理员");
            }
        } catch (Exception ignored) {
            // 未登录用户可以继续查看
        }

        // 公开图库只显示审核通过的图片
        queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                .isNull("spaceId")
                .eq("isDraft", 0); // 排除草稿状态的数据

        // 普通用户按推荐分数倒序排序，分数相同时按时间倒序
        queryWrapper.orderByDesc("recommendScore", "createTime");
    }

    @Override
    public boolean setPictureFeature(PictureFeatureRequest pictureFeatureRequest, User loginUser) {
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()),
                ErrorCode.NO_AUTH_ERROR, "仅管理员可设置精选");

        Long pictureId = pictureFeatureRequest.getId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 更新精选状态
        picture.setIsFeature(pictureFeatureRequest.getIsFeature());
        boolean result = this.updateById(picture);

        // 发送系统通知
        if (result && Integer.valueOf(1).equals(pictureFeatureRequest.getIsFeature())) {
            // 设置为精选时发送通知
            SystemNotifyUtil.sendPictureFeaturedNotify(picture.getUserId(), pictureId, picture.getName());
        }

        return result;
    }

    @Override
    public Page<PictureVO> getFeaturePicture(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isFeature", 1)
                .eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                .isNull("spaceId")
                .eq("isDraft", 0) // 排除草稿状态的数据
                .orderByDesc("createTime");

        // 执行查询
        Page<Picture> picturePage = this.page(new Page<>(current, size), queryWrapper);

        // 返回结果
        return getPictureVOPage(picturePage, request);
    }

    @Override
    public List<Picture> getAllDrafts(Long userId) {
        return this.lambdaQuery()
                .eq(Picture::getUserId, userId)
                .eq(Picture::getIsDraft, 1)
                .isNull(Picture::getSpaceId) // 只获取公共空间的草稿图片
                .orderByDesc(Picture::getCreateTime)
                .list();
    }

    @Override
    public Picture getLatestDraft(Long userId) {
        return this.lambdaQuery()
                .eq(Picture::getUserId, userId)
                .eq(Picture::getIsDraft, 1)
                .isNull(Picture::getSpaceId) // 只获取公共空间的草稿图片
                .orderByDesc(Picture::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public boolean updatePictureDraftStatus(Long pictureId, Integer isDraft, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureId == null || isDraft == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 3. 校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 4. 更新草稿状态
        Picture updatePicture = new Picture();
        updatePicture.setId(pictureId);
        updatePicture.setIsDraft(isDraft);

        // 如果是从草稿状态变为非草稿状态，需要更新编辑时间
        if (oldPicture.getIsDraft() == 1 && isDraft == 0) {
            updatePicture.setEditTime(new Date());
        }

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新草稿状态失败");

        // 如果是从草稿状态变为非草稿状态（即发布），则触发机器审核
        if (oldPicture.getIsDraft() == 1 && isDraft == 0) {
            // 获取完整的图片信息用于审核
            Picture pictureForAudit = this.getById(pictureId);

            // 触发机器审核
            performTencentCloudImageAudit(pictureForAudit, loginUser);

            // 审核完成后再次更新数据库以保存审核结果
            this.updateById(pictureForAudit);
        }



        return result;
    }

    @Override
    public List<PictureHotScoreDto> selectPictureScoreData(long offset, long pageSize) {
        // 只查询公共空间、审核通过且非草稿状态的图片
        return this.baseMapper.selectPictureScoreDataWithFilters(offset, pageSize);
    }

    @Override
    public long countPictureScoreData() {
        // 统计公共空间、审核通过且非草稿状态的图片
        return this.baseMapper.countPictureScoreData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchHotScore(List<Picture> pictures) {
        if (pictures == null || pictures.isEmpty()) {
            return false;
        }

        // 将大批次拆分成小批次以减小事务粒度
        int batchSize = 50; // 每批50条记录
        for (int i = 0; i < pictures.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, pictures.size());
            List<Picture> batch = pictures.subList(i, endIndex);

            // 批量更新这一小批次
            boolean result = this.updateBatchById(batch);
            if (!result) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchRecommendScore(List<Picture> pictures) {
        if (pictures == null || pictures.isEmpty()) {
            return false;
        }

        // 将大批次拆分成小批次以减小事务粒度
        int batchSize = 50; // 每批50条记录
        for (int i = 0; i < pictures.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, pictures.size());
            List<Picture> batch = pictures.subList(i, endIndex);

            // 批量更新这一小批次
            boolean result = this.updateBatchById(batch);
            if (!result) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Long selectMaxPictureId() {
        return this.baseMapper.selectMaxPictureId();
    }

    @Override
    public List<PictureHotScoreDto> selectPictureScoreDataInRange(long minId, long maxId, long offset, long pageSize) {
        // 只查询公共空间、审核通过且非草稿状态的图片
        return this.baseMapper.selectPictureScoreDataInRangeWithFilters(minId, maxId, offset, pageSize);
    }

    @Override
    public void addPictureViewRecord(long pictureId, long userId, HttpServletRequest request) {
        try {
            ViewRecordAddRequest viewRecordAddRequest = new ViewRecordAddRequest();
            viewRecordAddRequest.setUserId(userId);
            viewRecordAddRequest.setTargetId(pictureId);
            viewRecordAddRequest.setTargetType(1); // 1-图片

            viewRecordService.addViewRecord(viewRecordAddRequest, request);
        } catch (Exception e) {
            log.error("添加图片浏览记录失败", e);
        }
    }

    @Override
    public PictureTagCategory listPictureTagCategory(User loginUser) {
        String cacheKey = "picture:tag_category:list";

        // 如果用户已登录，使用用户私有缓存
        if (loginUser != null) {
            cacheKey = "picture:tag_category:list:" + loginUser.getId();
        }

        // 尝试从缓存获取
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                // 反序列化缓存数据
                PictureTagCategory cachedData = JSONUtil.toBean(cachedJson, PictureTagCategory.class);
                return cachedData;
            } catch (Exception e) {
                log.warn("获取图片标签分类缓存失败: {}", e.getMessage());
            }
        }

        // 缓存未命中，查询数据库默认全局分类和标签
        List<String> tagList = tagService.listTag();
        List<String> categoryList = categoryService.listCategory();

        // 如果用户已登录，进行个性化排序
        if (loginUser != null) {
            try {
                Map<String, Integer> tagWeightMap = new HashMap<>();
                Map<String, Integer> categoryWeightMap = new HashMap<>();
                Long userId = loginUser.getId();
                log.info("[个性化推荐] 开始计算用户 {} 的分类权重", userId);

                // 获取近期交互记录，包含目标类型 1（图片）

                // 1. 获取近期收藏
                List<FavoriteRecord> favorites = favoriteRecordService.list(new QueryWrapper<FavoriteRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 1)
                        .orderByDesc("createTime")
                        .last("limit 20"));
                log.info("[个性化推荐] 用户 {} 近期收藏图片 {} 条 (isFavorite=true)", userId, favorites.size());

                // 2. 获取近期点赞
                List<LikeRecord> likes = likeRecordService.list(new QueryWrapper<LikeRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 1)
                        .orderByDesc("lastLikeTime")
                        .last("limit 20"));
                log.info("[个性化推荐] 用户 {} 近期点赞图片 {} 条", userId, likes.size());

                // 3. 获取近期浏览
                List<ViewRecord> views = viewRecordService.list(new QueryWrapper<ViewRecord>()
                        .eq("userId", userId)
                        .eq("targetType", 1)
                        .orderByDesc("createTime")
                        .last("limit 50"));
                log.info("[个性化推荐] 用户 {} 近期浏览图片 {} 条", userId, views.size());

                // 统一收集图片ID
                Set<Long> pictureIds = new HashSet<>();
                favorites.forEach(f -> pictureIds.add(f.getTargetId()));
                likes.forEach(l -> pictureIds.add(l.getTargetId()));
                views.forEach(v -> pictureIds.add(v.getTargetId()));

                // 批量查询图片信息
                if (!pictureIds.isEmpty()) {
                    List<Picture> pictureList = this.listByIds(pictureIds);
                    Map<Long, Picture> pictureMap = pictureList.stream().collect(Collectors.toMap(Picture::getId, p -> p));

                    // 计算权重 (浏览=1, 点赞=2, 收藏=3)
                    views.forEach(v -> addWeight(pictureMap.get(v.getTargetId()), tagWeightMap, categoryWeightMap, 1));
                    likes.forEach(l -> addWeight(pictureMap.get(l.getTargetId()), tagWeightMap, categoryWeightMap, 2));
                    favorites.forEach(f -> addWeight(pictureMap.get(f.getTargetId()), tagWeightMap, categoryWeightMap, 3));
                    log.info("[个性化推荐] 用户 {} categoryWeightMap={}", userId, categoryWeightMap);

                    // 对默认列表进行降序排序
                    tagList.sort((a, b) -> categoryWeightMap.getOrDefault(b, 0) - categoryWeightMap.getOrDefault(a, 0));
                    categoryList.sort((a, b) -> categoryWeightMap.getOrDefault(b, 0) - categoryWeightMap.getOrDefault(a, 0));
                    log.info("[个性化推荐] 用户 {} 排序后 categoryList={}", userId, categoryList);
                }
            } catch (Exception e) {
                log.error("计算用户个性化分类推荐失败", e);
            }
        }

        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);

        // 将结果存入缓存 (公共缓存1小时，私有缓存5分钟以提升更新率)
        try {
            long expireTime = loginUser != null ? 300 : 3600;
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(pictureTagCategory),
                    expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("设置图片标签分类缓存失败: {}", e.getMessage());
        }

        return pictureTagCategory;
    }

    /**
     * 为标签和分类添加交互权重分数
     */
    private void addWeight(Picture p, Map<String, Integer> tagWeightMap, Map<String, Integer> categoryWeightMap, int weight) {
        if (p == null) return;

        // 累加分类
        if (StrUtil.isNotBlank(p.getCategory())) {
            categoryWeightMap.put(p.getCategory(), categoryWeightMap.getOrDefault(p.getCategory(), 0) + weight);
        }

        // 累加标签
        if (StrUtil.isNotBlank(p.getTags())) {
            try {
                List<String> tags = JSONUtil.toList(p.getTags(), String.class);
                for (String tag : tags) {
                    tagWeightMap.put(tag, tagWeightMap.getOrDefault(tag, 0) + weight);
                }
            } catch (Exception ignored) {
                // 如果解析失败则尝试使用逗号分隔 (因为历史原因有可能存的是字符串数组或逗号分隔)
                String[] tags = p.getTags().split(",");
                for (String tag : tags) {
                    if (StrUtil.isNotBlank(tag)) {
                        tagWeightMap.put(tag.trim(), tagWeightMap.getOrDefault(tag.trim(), 0) + weight);
                    }
                }
            }
        }
    }


    @Override
    public Map<Boolean, List<Long>> groupPictureByNew(List<Long> ids, long newPictureHours) {
        Map<Boolean, List<Long>> result = new HashMap<>();
        result.put(true, new ArrayList<>());  // 新图片
        result.put(false, new ArrayList<>()); // 旧图片

        if (ids == null || ids.isEmpty()) {
            return result;
        }

        // 计算时间阈值
        Instant thresholdTime = Instant.now().minus(newPictureHours, ChronoUnit.HOURS);

        // 查询图片信息，只获取公共空间、审核通过且非草稿状态的图片
        List<Picture> pictures = this.list(new QueryWrapper<Picture>()
                .in("id", ids)
                .isNull("spaceId")  // 只查询公共空间图片
                .eq("reviewStatus", 1)  // 只查询审核通过的图片
                .eq("isDraft", 0)  // 只查询非草稿状态的图片
        );

        // 按时间分组
        for (Picture picture : pictures) {
            if (picture.getCreateTime().toInstant().isAfter(thresholdTime)) {
                result.get(true).add(picture.getId());
            } else {
                result.get(false).add(picture.getId());
            }
        }

        return result;
    }

    @Override
    public QueryWrapper<Picture> filterPictureQueryByPermission(QueryWrapper<Picture> queryWrapper, User loginUser) {
        // 如果是管理员，可以查看所有审核通过且未删除的图片
        if (loginUser != null && userService.isAdmin(loginUser)) {
            return queryWrapper.eq("reviewStatus", 1).eq("isDelete", 0);
        }

        // 非管理员用户只能查看公共空间的图片
        queryWrapper.isNull("spaceId").eq("reviewStatus", 1).eq("isDelete", 0).eq("isDraft", 0);



        return queryWrapper;
    }

    @Override
    public boolean setPicturePermission(Long pictureId, Long userId, Integer allowCollect, Integer allowLike, Integer allowComment, Integer allowShare) {
        // 1. 校验参数
        if (pictureId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID和用户ID不能为空");
        }

        // 2. 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 3. 权限校验：只有图片作者或管理员可以设置权限
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        if (!userService.isAdmin(user) && !picture.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限设置图片权限");
        }

        // 4. 校验参数值的有效性
        if (allowCollect != null && !asList(0, 1).contains(allowCollect)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许收藏参数无效");
        }

        if (allowLike != null && !asList(0, 1).contains(allowLike)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许点赞参数无效");
        }

        if (allowComment != null && !asList(0, 1).contains(allowComment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许评论参数无效");
        }

        if (allowShare != null && !asList(0, 1).contains(allowShare)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "允许分享参数无效");
        }

        // 5. 构建更新对象
        Picture updatePicture = new Picture();
        updatePicture.setId(pictureId);

        if (allowCollect != null) {
            updatePicture.setAllowCollect(allowCollect);
        }
        if (allowLike != null) {
            updatePicture.setAllowLike(allowLike);
        }
        if (allowComment != null) {
            updatePicture.setAllowComment(allowComment);
        }
        if (allowShare != null) {
            updatePicture.setAllowShare(allowShare);
        }

        // 6. 更新数据库
        boolean result = this.updateById(updatePicture);

        // 7. 清除相关缓存
        if (result) {
            // 清除该图片的缓存
            String pictureCacheKey = String.format("picture:detail:%d", pictureId);
            stringRedisTemplate.delete(pictureCacheKey);

            // 清除可能影响的列表缓存
            String listCacheKeyPattern = "picture:list:*";
            Set<String> keys = stringRedisTemplate.keys(listCacheKeyPattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }

        return result;
    }

    @Override
    public Page<Picture> getUserAllPictures(Long userId, long current, long size) {
        // 构建查询条件：查询指定用户的图片，排除草稿状态的图片
        // 注意：由于isDelete字段使用了@TableLogic注解，MP会自动排除删除的记录
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .and(wrapper -> wrapper.eq("isDraft", 0).or().isNull("isDraft"))  // 不是草稿或草稿字段为空（默认为非草稿）
                .orderByDesc("createTime");  // 按创建时间倒序排列

        // 执行分页查询并返回结果
        return this.page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public Page<Picture> getUserPicturesFromOtherSpaces(Long userId, Long excludeSpaceId, long current, long size) {
        // 构建查询条件：查询指定用户在其他空间的图片，排除草稿状态的图片
        // 注意：由于isDelete字段使用了@TableLogic注解，MP会自动排除删除的记录
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .and(wrapper -> {
                    // 排除指定的空间ID，但同时保留公共空间（spaceId为null）
                    if (excludeSpaceId != null) {
                        wrapper.ne("spaceId", excludeSpaceId).or().isNull("spaceId");
                    } else {
                        // 如果excludeSpaceId为null，则排除公共空间（此时不会有公共空间的图片被返回）
                        wrapper.isNotNull("spaceId");
                    }
                })
                .and(wrapper -> wrapper.eq("isDraft", 0).or().isNull("isDraft"))  // 不是草稿或草稿字段为空（默认为非草稿）
                .orderByDesc("createTime");  // 按创建时间倒序排列

        // 执行分页查询并返回结果
        return this.page(new Page<>(current, size), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Picture copyPictureToNewSpace(Long pictureId, Long newSpaceId, User loginUser) {
        // 1. 验证参数
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");

        // 2. 查询原图片
        Picture originalPicture = this.getById(pictureId);
        ThrowUtils.throwIf(originalPicture == null, ErrorCode.NOT_FOUND_ERROR, "原图片不存在");

        // 3. 验证原图片是否属于当前用户
        if (!Objects.equals(originalPicture.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能复制自己上传的图片");
        }

        // 4. 如果目标空间不为null，验证新空间是否存在且属于当前用户
        if (newSpaceId != null && newSpaceId > 0) {
            Space newSpace = spaceService.getById(newSpaceId);
            ThrowUtils.throwIf(newSpace == null, ErrorCode.NOT_FOUND_ERROR, "目标空间不存在");
            if (!Objects.equals(newSpace.getUserId(), loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "目标空间不属于当前用户");
            }
        }

        // 5. 验证原图片是否为非草稿、非删除状态
        if ((originalPicture.getIsDraft() != null && originalPicture.getIsDraft().equals(1))
                || (originalPicture.getIsDelete() != null && originalPicture.getIsDelete().equals(1))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能复制草稿或已删除的图片");
        }

        // 6. 检查是否已经在目标空间存在相同的图片（通过URL判断）
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", originalPicture.getUrl())
                .eq("spaceId", newSpaceId != null ? newSpaceId : null)  // 如果newSpaceId为null，这里也匹配null值
                .eq("userId", loginUser.getId());
        Picture existingPicture = this.getOne(queryWrapper);
        if (existingPicture != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该图片已在目标空间中存在");
        }

        // 7. 复制图片信息到新对象
        Picture newPicture = new Picture();
        BeanUtil.copyProperties(originalPicture, newPicture);

        // 8. 设置新图片的特定属性
        newPicture.setId(null);  // 生成新的ID
        newPicture.setSpaceId(newSpaceId);  // 设置新的空间ID，可能是null（公共空间）
        newPicture.setCreateTime(new Date());  // 设置创建时间为当前时间
        newPicture.setUpdateTime(new Date());  // 设置更新时间为当前时间
        newPicture.setEditTime(null);  // 新图片没有编辑时间

        // 9. 保持审核状态（因为原图已经审核通过）
        // 不需要重新设置审核状态，直接复制即可

        // 10. 设置初始统计数据为0
        newPicture.setViewCount(0L);
        newPicture.setLikeCount(0L);
        newPicture.setFavoriteCount(0L);
        newPicture.setCommentCount(0L);
        newPicture.setShareCount(0L);

        // 11. 保存新图片
        boolean saveResult = this.save(newPicture);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "复制图片失败");

        // 12. 如果目标空间不为null，更新空间统计信息
        if (newSpaceId != null && newSpaceId > 0) {
            Space newSpace = spaceService.getById(newSpaceId);
            newSpace.setTotalCount(newSpace.getTotalCount() + 1);
            newSpace.setTotalSize(newSpace.getTotalSize() + (originalPicture.getPicSize() != null ? originalPicture.getPicSize() : 0));
            spaceService.updateById(newSpace);
        }

        // 13. 返回新创建的图片
        return newPicture;
    }

    @Override
    public List<String> aiTag(MultipartFile file, Long pictureId) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
        // 2. 查询图片是否存在
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 3. 调用 YOLO 服务识别标签
        try {
            List<String> aiLabels = yoloService.getLabels(file);
            if (CollUtil.isNotEmpty(aiLabels)) {
                // 4. 更新图片记录
                picture.setAiLabels(JSONUtil.toJsonStr(aiLabels));
                boolean success = this.updateById(picture);
                ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "保存 AI 标签失败");
            }
            return aiLabels;
        } catch (Exception e) {
            log.error("AI 标签识别失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 标签识别失败: " + e.getMessage());
        }
    }
}

