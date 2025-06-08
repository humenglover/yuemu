package com.lumenglover.yuemupicturebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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
import com.lumenglover.yuemupicturebackend.esdao.EsPictureDao;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.manager.CounterManager;
import com.lumenglover.yuemupicturebackend.manager.CrawlerManager;
import com.lumenglover.yuemupicturebackend.manager.FileManager;
import com.lumenglover.yuemupicturebackend.manager.auth.SpaceUserAuthManager;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.manager.upload.FilePictureUpload;
import com.lumenglover.yuemupicturebackend.manager.upload.PictureUploadTemplate;
import com.lumenglover.yuemupicturebackend.manager.upload.UrlPictureUpload;
import com.lumenglover.yuemupicturebackend.mapper.ChatMessageMapper;
import com.lumenglover.yuemupicturebackend.mapper.PictureMapper;
import com.lumenglover.yuemupicturebackend.model.dto.file.UploadPictureResult;
import com.lumenglover.yuemupicturebackend.model.dto.picture.*;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsPicture;
import com.lumenglover.yuemupicturebackend.model.enums.OperationEnum;
import com.lumenglover.yuemupicturebackend.model.enums.PictureReviewStatusEnum;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.ColorSimilarUtils;
import com.lumenglover.yuemupicturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private UserfollowsService userfollowsService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    @Lazy
    private ShareRecordService shareRecordService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private CrawlerManager crawlerManager;

    @Resource
    private ChatMessageMapper chatMessageMapper;

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
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setCategory(pictureUploadRequest.getCategoryName());
        picture.setTags(pictureUploadRequest.getTagName());
        picture.setUserId(loginUser.getId());
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
        // this.clearPictureFile(oldPicture);
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

        // 设置点赞状态 - 使用新的通用点赞表
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            // 使用 LikeRecordService 的方法来检查点赞状态
            boolean isLiked = likeRecordService.isContentLiked(picture.getId(), 1, loginUser.getId());
            pictureVO.setIsLiked(isLiked ? 1 : 0);
            // 获取分享状态
            boolean isShared = shareRecordService.isContentShared(picture.getId(), 1, loginUser.getId());
            pictureVO.setIsShared(isShared ? 1 : 0);
        } else {
            pictureVO.setIsLiked(0);
            pictureVO.setIsShared(0);
        }

        return pictureVO;
    }

    /**
     * 获取图片VO（内部使用）
     */
    private PictureVO getPictureVOInternal(Picture picture, User loginUser) {
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
         if (userId != null && userId > 0) {
             User user = userService.getById(userId);
             UserVO userVO = userService.getUserVO(user);
             pictureVO.setUser(userVO);
         }

        // // 设置点赞状态 - 使用新的通用点赞表
        // if (loginUser != null) {
        //     boolean isLiked = likeRecordService.isContentLiked(picture.getId(), 1, loginUser.getId());
        //     pictureVO.setIsLiked(isLiked ? 1 : 0);
        //     // 获取分享状态
        //     boolean isShared = shareRecordService.isContentShared(picture.getId(), 1, loginUser.getId());
        //     pictureVO.setIsShared(isShared ? 1 : 0);
        // } else {
        //     pictureVO.setIsLiked(0);
        //     pictureVO.setIsShared(0);
        // }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        // 获取登录用户，可以是登录，可以是未登录
        User loginUser = userService.isLogin(request);

        // 填充信息
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> getPictureVOInternal(picture, loginUser))
                .collect(Collectors.toList());

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
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
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 5. 同步更新 ES 数据
        try {
            // 先查询 ES 中是否存在该数据
            Optional<EsPicture> esOptional = esPictureDao.findById(id);
            EsPicture esPicture;
            if (esOptional.isPresent()) {
                // 如果存在，获取现有数据并更新审核相关字段
                esPicture = esOptional.get();
                esPicture.setReviewStatus(reviewStatus);
                esPicture.setReviewMessage(reviewMessage);
                esPicture.setReviewerId(loginUser.getId());
                esPicture.setReviewTime(updatePicture.getReviewTime());
            } else {
                // 如果不存在，从 MySQL 获取完整数据并创建新的 ES 文档
                Picture fullPicture = this.getById(id);
                esPicture = new EsPicture();
                BeanUtils.copyProperties(fullPicture, esPicture);
            }
            // 保存或更新到 ES
            esPictureDao.save(esPicture);
        } catch (Exception e) {
            log.error("Failed to sync picture review status to ES, pictureId: {}", id, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
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
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
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

                    // 批量删除ES数据
                    pictureIds.forEach(id -> {
                        try {
                            esPictureDao.deleteById(id);
                        } catch (Exception e) {
                            log.error("Delete picture from ES failed, pictureId: {}", id, e);
                        }
                    });

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
        else if (operationType == OperationEnum.APPROVE.getValue() ||
                operationType == OperationEnum.REJECT.getValue()) {
            try {
                // 设置审核状态
                Integer reviewStatus = operationType == OperationEnum.APPROVE.getValue() ?
                        PictureReviewStatusEnum.PASS.getValue() :
                        PictureReviewStatusEnum.REJECT.getValue();

                // 更新 MySQL 数据
                result = update()
                        .set("reviewStatus", reviewStatus)
                        .set("reviewTime", new Date())
                        .set("reviewMessage", operationType == OperationEnum.APPROVE.getValue() ?
                                "批量审核通过" : "批量审核不通过")
                        .in("id", pictureIds)
                        .update();

                if (result) {
                    // 批量更新 ES 数据
                    List<EsPicture> esPictures = new ArrayList<>();
                    for (Long pictureId : pictureIds) {
                        Optional<EsPicture> esOptional = esPictureDao.findById(pictureId);
                        EsPicture esPicture;
                        if (esOptional.isPresent()) {
                            // 如果存在，只更新审核状态
                            esPicture = esOptional.get();
                            esPicture.setReviewStatus(reviewStatus);
                            esPicture.setReviewTime(new Date());
                            esPicture.setReviewMessage(operationType == OperationEnum.APPROVE.getValue() ?
                                    "批量审核通过" : "批量审核不通过");
                        } else {
                            // 如果不存在，从 MySQL 获取完整数据
                            Picture picture = this.getById(pictureId);
                            if (picture != null) {
                                esPicture = new EsPicture();
                                BeanUtils.copyProperties(picture, esPicture);
                                esPicture.setReviewStatus(reviewStatus);
                                esPicture.setReviewTime(new Date());
                                esPicture.setReviewMessage(operationType == OperationEnum.APPROVE.getValue() ?
                                        "批量审核通过" : "批量审核不通过");
                            } else {
                                continue;
                            }
                        }
                        esPictures.add(esPicture);
                    }

                    if (!esPictures.isEmpty()) {
                        // 批量保存到 ES
                        esPictureDao.saveAll(esPictures);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sync pictures review status to ES, pictureIds: {}, operationType: {}",
                        pictureIds, operationType, e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
            }
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

                // 从ES删除
                try {
                    esPictureDao.deleteById(pictureId);
                } catch (Exception e) {
                    log.error("Delete picture from ES failed, pictureId: {}", pictureId, e);
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

        // 设置编辑时间
        picture.setEditTime(new Date());

        // 更新图片
        boolean success = this.updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);

        // 同步到ES
        try {
            Optional<EsPicture> esPictureOptional = esPictureDao.findById(pictureId);
            if (esPictureOptional.isPresent()) {
                EsPicture esPicture = esPictureOptional.get();
                BeanUtils.copyProperties(picture, esPicture);
                esPictureDao.save(esPicture);
            }
        } catch (Exception e) {
            log.error("同步ES数据失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步ES数据失败");
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
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

        // 6. 同步更新 ES 数据
        try {
            List<EsPicture> esPictures = new ArrayList<>();
            for (Picture picture : pictureList) {
                Long pictureId = picture.getId();
                Optional<EsPicture> esOptional = esPictureDao.findById(pictureId);
                EsPicture esPicture;
                if (esOptional.isPresent()) {
                    esPicture = esOptional.get();
                    if (StrUtil.isNotBlank(nameRule)) {
                        esPicture.setName(picture.getName());
                    }
                    if (StrUtil.isNotBlank(category)) {
                        esPicture.setCategory(category);
                    }
                    if (CollUtil.isNotEmpty(tags)) {
                        esPicture.setTags(JSONUtil.toJsonStr(tags));
                    }
                } else {
                    Picture fullPicture = this.getById(pictureId);
                    esPicture = new EsPicture();
                    BeanUtils.copyProperties(fullPicture, esPicture);
                }
                esPictures.add(esPicture);
            }
            // 批量保存到 ES
            esPictureDao.saveAll(esPictures);
        } catch (Exception e) {
            log.error("Failed to sync pictures to ES during batch edit, pictureIds: {}",
                    pictureIdList.toString(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }
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
        List<Picture> pictureList = getTop100PictureList(id);

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
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return pictureList.stream()
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
                .orderByDesc("createTime");

        // 获取图片列表
        Page<Picture> picturePage = this.page(page, queryWrapper);
        List<Picture> pictureList = picturePage.getRecords();

        // 将 Picture 列表转换为 PictureVO 列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> getPictureVOInternal(picture, currentUser))
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
        picture.setTags(pictureUploadRequest.getTagName());
        picture.setUserId(loginUser.getId());

        // 帖子图片特殊处理：自动设置为审核通过状态
        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        picture.setReviewMessage("帖子照片自动过审");

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
                .eq("reviewStatus", 1);

        // 根据不同时间范围查询
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);

        switch (id.intValue()) {
            case 1: // 日榜
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case 2: // 周榜
                calendar.add(Calendar.WEEK_OF_MONTH, -1);
                break;
            case 3: // 月榜
                calendar.add(Calendar.MONTH, -1);
                break;
            case 4: // 总榜
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (id < 4) {
            queryWrapper.ge("createTime", calendar.getTime());
        }

        // 修改排序规则，加入浏览量因素
        queryWrapper.orderByDesc(
                "(likeCount * 0.4 + commentCount * 0.3 + viewCount * 0.2 + shareCount * 0.1)"
        );

        queryWrapper.last("LIMIT 100");
        return this.list(queryWrapper);
    }

    @Override
    public boolean updatePicture(Picture picture) {
        // 更新数据库
        boolean success = this.updateById(picture);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 同步更新 ES 数据
        try {
            Optional<EsPicture> esOptional = esPictureDao.findById(picture.getId());
            EsPicture esPicture;
            if (esOptional.isPresent()) {
                esPicture = esOptional.get();
                esPicture.setName(picture.getName());
                esPicture.setIntroduction(picture.getIntroduction());
                esPicture.setCategory(picture.getCategory());
                esPicture.setTags(picture.getTags());
                esPicture.setEditTime(picture.getEditTime());
                esPicture.setReviewStatus(picture.getReviewStatus());
                esPicture.setReviewMessage(picture.getReviewMessage());
            } else {
                esPicture = new EsPicture();
                BeanUtils.copyProperties(picture, esPicture);
            }
            esPictureDao.save(esPicture);
            return true;
        } catch (Exception e) {
            log.error("同步ES数据失败, pictureId: {}", picture.getId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步ES数据失败");
        }
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
        }

        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
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
        User currentUser = userService.getLoginUser(request);
        if (currentUser != null) {
            String userRole = currentUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        crawlerDetect(request);

        // 处理查询条件
        if (spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }

        // 构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = RedisConstant.PUBLIC_PIC_REDIS_KEY_PREFIX + hashKey;

        // 查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, size),
                this.getQueryWrapper(pictureQueryRequest));
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
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> this.getPictureVO(picture, null))
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
            // 公开图库查询 - 自定义排序逻辑
            handlePublicQueryWithCustomSort(queryWrapper);
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

        // 检查是否是空间所有者
        if (loginUser != null && loginUser.getId().equals(space.getUserId())) {
            queryWrapper.eq("spaceId", spaceId);
            return; // 空间所有者直接放行
        }

        // 检查查看权限
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权查看该空间的图片");
        queryWrapper.eq("spaceId", spaceId);
    }

    /**
     * 处理用户图片查询
     */
    private void handleUserQuery(Long userId, User loginUser, QueryWrapper<Picture> queryWrapper) {
        // 只能查看自己的图片
        ThrowUtils.throwIf(loginUser == null || !loginUser.getId().equals(userId),
                ErrorCode.NO_AUTH_ERROR, "只能查看自己的图片");
        queryWrapper.eq("userId", userId)
                .isNull("spaceId");
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
                .isNull("spaceId");

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
        return this.updateById(picture);
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
                .orderByDesc("createTime");

        // 执行查询
        Page<Picture> picturePage = this.page(new Page<>(current, size), queryWrapper);

        // 返回结果
        return getPictureVOPage(picturePage, request);
    }
}

