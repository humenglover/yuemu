package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.api.aliyunai.AliYunAiApi;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.lumenglover.yuemupicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.lumenglover.yuemupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.DeleteRequest;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.CrawlerConstant;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.model.dto.picture.*;
import com.lumenglover.yuemupicturebackend.model.dto.picture.UpdatePictureDraftRequest;
import com.lumenglover.yuemupicturebackend.model.dto.picture.PicturePermissionRequest;
import com.lumenglover.yuemupicturebackend.model.dto.picture.CopyPictureRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.PictureTagCategory;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private TagService tagService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private AliYunAiApi aliYunAiApi;


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @RateLimiter(key = "picture_upload", time = 60, count = 60, message = "图片上传过于频繁，请稍后再试")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 对图片进行 AI 标签识别
     */
    @PostMapping("/ai_tag")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @RateLimiter(key = "picture_ai_tag", time = 60, count = 60, message = "AI 标签识别过于频繁，请稍后再试")
    public BaseResponse<List<String>> aiTag(
            @RequestPart("file") MultipartFile multipartFile,
            @RequestParam("pictureId") Long pictureId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        List<String> aiLabels = pictureService.aiTag(multipartFile, pictureId);
        return ResultUtils.success(aiLabels);
    }

    /**
     * 通过 URL 上传图片
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @RateLimiter(key = "picture_upload_url", time = 60, count = 60, message = "URL图片上传过于频繁，请稍后再试")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    @RateLimiter(key = "picture_delete", time = 60, count = 60, message = "图片删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> updatePicture(@RequestBody Picture picture) {
        return ResultUtils.success(pictureService.updatePicture(picture));
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 批量
     */
    @PostMapping("/batchOption")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchOperationPicture(@RequestBody PictureOperation pictureOperation,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureOperation == null || pictureOperation.getIds() == null || pictureOperation.getIds().isEmpty(), ErrorCode.PARAMS_ERROR);
        //判断是否登录
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        //调用service方法
        boolean result = pictureService.batchOperationPicture(pictureOperation);
        //返回结果
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    @RateLimiter(key = "picture_get_vo", time = 60, count = 60, message = "图片详情查询过于频繁，请稍后再试")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        return ResultUtils.success(pictureService.getPictureVOById(id, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类，有缓存）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {

        // 用户权限校验
        User loginUser = userService.isLogin(request);
        if (loginUser != null) {
            String userRole = loginUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 1. 参数校验
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "每页最多显示50条");

        return ResultUtils.success(pictureService.listPictureVOByPageWithCache(pictureQueryRequest, request));
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    @RateLimiter(key = "picture_list_vo", time = 60, count = 60, message = "图片列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        // 用户权限校验
        User loginUser = userService.isLogin(request);
        if (loginUser != null) {
            String userRole = loginUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 1. 参数校验
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "每页最多显示50条");

        // 3. 执行查询并返回结果
        return ResultUtils.success(pictureService.listPictureVOByPage(pictureQueryRequest, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    @RateLimiter(key = "picture_tag_category", time = 60, count = 120, message = "标签分类查询过于频繁，请稍后再试")
    public BaseResponse<PictureTagCategory> listPictureTagCategory(HttpServletRequest request) {
        User loginUser = userService.isLogin(request);
        PictureTagCategory pictureTagCategory = pictureService.listPictureTagCategory(loginUser);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取并创建图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    @RateLimiter(key = "picture_search_by_picture", time = 60, count = 10, message = "以图搜图过于频繁，请稍后再试")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getThumbnailUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 按照颜色搜索
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    @RateLimiter(key = "picture_search_by_color", time = 60, count = 15, message = "按颜色搜索过于频繁，请稍后再试")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                                    HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * top100的数据
     */
    /**
     * top100 的数据
     */
    @GetMapping("/top100/{id}")
    @RateLimiter(key = "picture_top100", time = 60, count = 20, message = "Top100图片查询过于频繁，请稍后再试")
    public BaseResponse<List<PictureVO>> getTop100Picture(@PathVariable Long id) {
        return ResultUtils.success(pictureService.getTop100PictureWithCache(id));
    }

    /**
     * 关注列表照片
     */
    @PostMapping("/follow")
    @RateLimiter(key = "picture_follow", time = 60, count = 25, message = "关注列表图片查询过于频繁，请稍后再试")
    public BaseResponse<Page<PictureVO>> getFollowPicture(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                          HttpServletRequest request) {
        return ResultUtils.success(pictureService.getFollowPicture(request, pictureQueryRequest));
    }

    /**
     * 上传帖子图片
     */
    @PostMapping("/upload/postimage")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPostImage(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPostPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 设置图片精选状态
     */
    @PostMapping("/feature")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> setPictureFeature(@RequestBody PictureFeatureRequest pictureFeatureRequest,
                                                   HttpServletRequest request) {
        if (pictureFeatureRequest == null || pictureFeatureRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.setPictureFeature(pictureFeatureRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取精选图片列表
     */
    @PostMapping("/feature/list")
    public BaseResponse<Page<PictureVO>> getFeaturePicture(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                           HttpServletRequest request) {
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 限制爬虫
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<PictureVO> picturePage = pictureService.getFeaturePicture(pictureQueryRequest, request);
        return ResultUtils.success(picturePage);
    }

    /**
     * 获取用户的所有草稿图片
     */
    @GetMapping("/draft/list")
    @RateLimiter(key = "picture_draft_list", time = 60, count = 15, message = "草稿图片列表查询过于频繁，请稍后再试")
    public BaseResponse<List<Picture>> listDraftPictures(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<Picture> draftPictures = pictureService.getAllDrafts(loginUser.getId());
        return ResultUtils.success(draftPictures);
    }

    /**
     * 获取用户的最新草稿图片
     */
    @GetMapping("/draft/latest")
    @RateLimiter(key = "picture_draft_latest", time = 60, count = 10, message = "最新草稿查询过于频繁，请稍后再试")
    public BaseResponse<Picture> getLatestDraft(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Picture latestDraft = pictureService.getLatestDraft(loginUser.getId());
        return ResultUtils.success(latestDraft);
    }

    /**
     * 更新图片草稿状态
     */
    @PostMapping("/draft/update")
    @RateLimiter(key = "picture_draft_update", time = 60, count = 10, message = "草稿状态更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updatePictureDraftStatus(@RequestBody UpdatePictureDraftRequest updatePictureDraftRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(updatePictureDraftRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.updatePictureDraftStatus(updatePictureDraftRequest.getPictureId(),
                updatePictureDraftRequest.getIsDraft(),
                loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 设置图片权限
     *
     * @param permissionRequest 权限设置请求参数
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/permission/set")
    public BaseResponse<Boolean> setPicturePermission(@RequestBody PicturePermissionRequest permissionRequest, HttpServletRequest request) {
        // 校验参数
        if (permissionRequest == null || permissionRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 调用服务层设置权限
        boolean result = pictureService.setPicturePermission(
                permissionRequest.getPictureId(),
                loginUser.getId(),
                permissionRequest.getAllowCollect(),
                permissionRequest.getAllowLike(),
                permissionRequest.getAllowComment(),
                permissionRequest.getAllowShare()
        );

        return ResultUtils.success(result);
    }

    /**
     * 获取图片权限（仅管理员可用）
     *
     * @param pictureId 图片ID
     * @param request HTTP请求
     * @return 图片权限信息
     */
    @GetMapping("/permission/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPicturePermission(Long pictureId, HttpServletRequest request) {
        if (pictureId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        }

        // 获取图片信息
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        return ResultUtils.success(picture);
    }

    /**
     * 分页获取用户的所有图片（非草稿、非删除）
     */
    @GetMapping("/user/all")
    public BaseResponse<Page<Picture>> getUserAllPictures(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<Picture> pictures = pictureService.getUserAllPictures(loginUser.getId(), current, size);
        return ResultUtils.success(pictures);
    }

    /**
     * 分页获取用户在其他空间的图片（非草稿、非删除）
     *
     * @param spaceId 要排除的空间ID
     * @param current 页码
     * @param size 页面大小
     * @param request HTTP请求
     * @return 分页的其他空间的图片列表
     */
    @GetMapping("/user/other-spaces")
    public BaseResponse<Page<Picture>> getUserPicturesFromOtherSpaces(
            Long spaceId, Long current, Long size,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<Picture> pictures = pictureService.getUserPicturesFromOtherSpaces(loginUser.getId(), spaceId, current, size);
        return ResultUtils.success(pictures);
    }

    /**
     * 从现有图片复制到新空间
     *
     * @param copyPictureRequest 复制图片请求参数
     * @param request HTTP请求
     * @return 复制后的图片信息
     */
    @PostMapping("/copy")
    public BaseResponse<PictureVO> copyPictureToNewSpace(@RequestBody CopyPictureRequest copyPictureRequest,
                                                         HttpServletRequest request) {
        // 验证参数
        ThrowUtils.throwIf(copyPictureRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(copyPictureRequest.getPictureId() == null, ErrorCode.PARAMS_ERROR, "原图片ID不能为空");

        User loginUser = userService.getLoginUser(request);

        // 复制图片到新空间
        Picture newPicture = pictureService.copyPictureToNewSpace(
                copyPictureRequest.getPictureId(),
                copyPictureRequest.getSpaceId(),
                loginUser);

        // 转换为VO并返回
        PictureVO pictureVO = pictureService.getPictureVO(newPicture, request);
        return ResultUtils.success(pictureVO);
    }
}
