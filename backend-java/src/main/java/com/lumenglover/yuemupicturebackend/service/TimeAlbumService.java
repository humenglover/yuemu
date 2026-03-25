package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.timealbum.TimeAlbumHeartWallRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.TimeAlbum;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.model.entity.User;

import java.util.List;

/**
 * 时光相册服务
 */
public interface TimeAlbumService extends IService<TimeAlbum> {

    /**
     * 创建相册
     * @param timeAlbum 相册信息
     * @param loginUserId 当前登录用户ID
     * @param loveBoardId 恋爱板ID
     * @return 相册ID
     */
    long createTimeAlbum(TimeAlbum timeAlbum, long loginUserId, long loveBoardId);

    /**
     * 删除相册
     * @param id 相册ID
     * @param loginUserId 当前登录用户ID
     * @param loveBoardId 恋爱板ID
     * @return 是否成功
     */
    boolean deleteTimeAlbum(long id, long loginUserId, long loveBoardId);

    /**
     * 更新相册
     * @param timeAlbum 相册信息
     * @param loginUserId 当前登录用户ID
     * @param loveBoardId 恋爱板ID
     * @return 是否成功
     */
    boolean updateTimeAlbum(TimeAlbum timeAlbum, long loginUserId, long loveBoardId);

    /**
     * 获取相册信息
     * @param id 相册ID
     * @param userId 用户ID（可选）
     * @param password 访问密码（非所有者访问私密相册时需要）
     * @return 相册信息
     */
    TimeAlbum getTimeAlbumById(long id, Long userId, String password);

    /**
     * 上传爱心墙图片
     * @param request 上传请求
     * @param loginUserId 当前登录用户ID
     * @return 上传的图片列表
     */
    List<PictureVO> uploadHeartWallPictures(TimeAlbumHeartWallRequest request, long loginUserId);
    /**
     * 获取爱心墙图片列表
     * @param albumId 相册ID
     * @param userId 用户ID（可选）
     * @param password 访问密码（非所有者访问私密相册时需要）
     * @return 图片列表
     */
    List<Picture> getHeartWallPictures(long albumId, Long userId, String password);

    /**
     * 删除爱心墙照片
     * @param pictureId 照片ID
     * @param albumId 相册ID
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean deleteHeartWallPicture(long pictureId, long albumId, User loginUser);

    /**
     * 设置相册密码
     * @param albumId 相册ID
     * @param password 新密码
     * @param loginUserId 当前登录用户ID
     * @return 是否成功
     */
    boolean setAlbumPassword(Long albumId, String password, Long loginUserId);

    /**
     * 修改相册密码
     * @param albumId 相册ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @param loginUserId 当前登录用户ID
     * @return 是否成功
     */
    boolean updateAlbumPassword(Long albumId, String oldPassword, String newPassword, Long loginUserId);

    /**
     * 取消相册密码
     * @param albumId 相册ID
     * @param password 当前密码
     * @param loginUserId 当前登录用户ID
     * @return 是否成功
     */
    boolean removeAlbumPassword(Long albumId, String password, Long loginUserId);
}
