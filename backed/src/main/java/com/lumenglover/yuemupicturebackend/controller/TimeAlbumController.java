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
