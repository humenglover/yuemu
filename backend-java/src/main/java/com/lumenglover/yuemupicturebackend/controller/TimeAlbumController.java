package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.timealbum.TimeAlbumHeartWallRequest;
import com.lumenglover.yuemupicturebackend.model.dto.timealbum.TimeAlbumPasswordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.TimeAlbum;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.service.TimeAlbumService;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 时光相册接口
 */
@RestController
@RequestMapping("/timeAlbum")
@Slf4j
public class TimeAlbumController {

    @Resource
    private TimeAlbumService timeAlbumService;

    @Resource
    private UserService userService;

    /**
     * 创建相册
     *
     * @param timeAlbum   相册信息
     * @param loveBoardId 爱心墙ID
     * @param request     请求
     * @return 相册ID
     */
    @PostMapping("/add")
    @RateLimiter(key = "time_album_add", time = 3600, count = 5, message = "时光相册创建过于频繁，请稍后再试")
    public BaseResponse<Long> addTimeAlbum(@RequestBody TimeAlbum timeAlbum,
                                           @RequestParam("loveBoardId") long loveBoardId,
                                           HttpServletRequest request) {
        if (timeAlbum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long timeAlbumId = timeAlbumService.createTimeAlbum(timeAlbum, loginUser.getId(), loveBoardId);
        return ResultUtils.success(timeAlbumId);
    }

    /**
     * 删除相册
     *
     * @param id        相册ID
     * @param loveBoardId 爱心墙ID
     * @param request   请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @RateLimiter(key = "time_album_delete", time = 60, count = 10, message = "时光相册删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteTimeAlbum(@RequestParam("id") long id,
                                                 @RequestParam("loveBoardId") long loveBoardId,
                                                 HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = timeAlbumService.deleteTimeAlbum(id, loginUser.getId(), loveBoardId);
        return ResultUtils.success(result);
    }

    /**
     * 更新相册
     *
     * @param timeAlbum   相册信息
     * @param loveBoardId 爱心墙ID
     * @param request     请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @RateLimiter(key = "time_album_update", time = 60, count = 10, message = "时光相册更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateTimeAlbum(@RequestBody TimeAlbum timeAlbum,
                                                 @RequestParam("loveBoardId") long loveBoardId,
                                                 HttpServletRequest request) {
        if (timeAlbum == null || timeAlbum.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = timeAlbumService.updateTimeAlbum(timeAlbum, loginUser.getId(), loveBoardId);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取相册
     *
     * @param id        相册ID
     * @param userId    用户ID
     * @param password  密码
     * @return 相册信息
     */
    @GetMapping("/get")
    @RateLimiter(key = "time_album_get", time = 60, count = 30, message = "时光相册详情查询过于频繁，请稍后再试")
    public BaseResponse<TimeAlbum> getTimeAlbumById(@RequestParam("id") long id,
                                                    @RequestParam(value = "userId", required = false) Long userId,
                                                    @RequestParam(value = "password", required = false) String password) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TimeAlbum timeAlbum = timeAlbumService.getTimeAlbumById(id, userId, password);
        return ResultUtils.success(timeAlbum);
    }

    /**
     * 上传爱心墙图片
     *
     * @param request 请求
     * @return 上传的图片列表
     */
    @PostMapping("/heart-wall/upload")
    @RateLimiter(key = "time_album_heart_upload", time = 3600, count = 20, message = "爱心墙图片上传过于频繁，请稍后再试")
    public BaseResponse<List<PictureVO>> uploadHeartWallPictures(TimeAlbumHeartWallRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null || request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        List<PictureVO> pictureVOList = timeAlbumService.uploadHeartWallPictures(request, loginUser.getId());
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 获取爱心墙图片列表
     *
     * @param albumId   相册ID
     * @param userId    用户ID
     * @param password  密码
     * @return 图片列表
     */
    @GetMapping("/heart-wall/list")
    @RateLimiter(key = "time_album_heart_list", time = 60, count = 30, message = "爱心墙图片列表查询过于频繁，请稍后再试")
    public BaseResponse<List<Picture>> getHeartWallPictures(@RequestParam("albumId") long albumId,
                                                            @RequestParam(value = "userId", required = false) Long userId,
                                                            @RequestParam(value = "password", required = false) String password) {
        if (albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<Picture> PictureList = timeAlbumService.getHeartWallPictures(albumId, userId, password);
        return ResultUtils.success(PictureList);
    }

    /**
     * 分页获取相册列表
     *
     * @param current   当前页号
     * @param pageSize  页面大小
     * @param loveBoardId 爱心墙ID
     * @param request   请求
     * @return 相册列表
     */
    @GetMapping("/list")
    @RateLimiter(key = "time_album_list", time = 60, count = 30, message = "时光相册列表查询过于频繁，请稍后再试")
    public BaseResponse<Page<TimeAlbum>> listTimeAlbum(
            @RequestParam("current") long current,
            @RequestParam("pageSize") long pageSize,
            @RequestParam("loveBoardId") long loveBoardId,
            HttpServletRequest request) {
        if (current <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 限制爬虫
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 构建查询条件
        QueryWrapper<TimeAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("loveBoardId", loveBoardId);

        queryWrapper.orderByDesc("createTime");
        Page<TimeAlbum> timeAlbumPage = timeAlbumService.page(new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(timeAlbumPage);
    }

    /**
     * 删除爱心墙照片
     *
     * @param pictureId 照片ID
     * @param albumId 相册ID
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/heart-wall/delete")
    @RateLimiter(key = "time_album_heart_delete", time = 60, count = 10, message = "爱心墙图片删除过于频繁，请稍后再试")
    public BaseResponse<Boolean> deleteHeartWallPicture(@RequestParam("pictureId") long pictureId,
                                                        @RequestParam("albumId") long albumId,
                                                        HttpServletRequest request) {
        if (pictureId <= 0 || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = timeAlbumService.deleteHeartWallPicture(pictureId, albumId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 设置相册密码
     *
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/password/set")
    @RateLimiter(key = "time_album_password_set", time = 60, count = 5, message = "相册密码设置过于频繁，请稍后再试")
    public BaseResponse<Boolean> setAlbumPassword(@RequestBody TimeAlbumPasswordRequest request,
                                                  HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null || StringUtils.isBlank(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = timeAlbumService.setAlbumPassword(request.getAlbumId(), request.getNewPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 修改相册密码
     *
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/password/update")
    @RateLimiter(key = "time_album_password_update", time = 60, count = 5, message = "相册密码更新过于频繁，请稍后再试")
    public BaseResponse<Boolean> updateAlbumPassword(@RequestBody TimeAlbumPasswordRequest request,
                                                     HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null ||
                StringUtils.isBlank(request.getOldPassword()) || StringUtils.isBlank(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = timeAlbumService.updateAlbumPassword(request.getAlbumId(),
                request.getOldPassword(), request.getNewPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 取消相册密码
     *
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/password/remove")
    @RateLimiter(key = "time_album_password_remove", time = 60, count = 5, message = "相册密码移除过于频繁，请稍后再试")
    public BaseResponse<Boolean> removeAlbumPassword(@RequestBody TimeAlbumPasswordRequest request,
                                                     HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null || StringUtils.isBlank(request.getOldPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = timeAlbumService.removeAlbumPassword(request.getAlbumId(), request.getOldPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }
}
