package com.lumenglover.yuemupicturebackend.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.UserFollowsService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 图片权限检查工具类
 */
@Component
public class PicturePermissionUtils {

    @Resource
    private UserService userService;

    @Resource
    private UserFollowsService userFollowsService;

    /**
     * 检查用户是否有查看图片的权限
     *
     * @param picture 图片对象
     * @param user    当前登录用户，可以为null（未登录）
     * @return 是否有查看权限
     */
    public boolean canViewPicture(Picture picture, User user) {
        // 1. 管理员可以查看所有审核通过且未删除的图片
        if (user != null && userService.isAdmin(user)) {
            return picture.getReviewStatus() == 1 && picture.getIsDelete() == 0;
        }

        // 2. 图片作者可以查看自己的图片（无论状态如何）
        if (user != null && picture.getUserId() != null && user.getId().equals(picture.getUserId())) {
            return picture.getIsDelete() == 0; // 只要没删除就能查看
        }

        // 3. 非管理员用户只能查看公共空间的图片（审核通过、未删除、非草稿）
        if (picture.getSpaceId() != null) {
            return false; // 不是公共空间图片，不能查看
        }

        if (picture.getReviewStatus() != 1 || picture.getIsDelete() != 0 || picture.getIsDraft() != 0) {
            return false; // 审核未通过、已删除或为草稿，不能查看
        }
        return false;
    }

    /**
     * 检查用户是否有收藏图片的权限
     *
     * @param picture 图片对象
     * @param user    当前登录用户
     * @return 是否有收藏权限
     */
    public boolean canCollectPicture(Picture picture, User user) {
        // 1. 管理员可以收藏所有图片
        if (user != null && userService.isAdmin(user)) {
            return picture.getIsDelete() == 0;
        }

        // 2. 未登录用户不能收藏
        if (user == null) {
            return false;
        }

        // 3. 图片作者不能收藏自己的图片
        if (picture.getUserId() != null && user.getId().equals(picture.getUserId())) {
            return false;
        }

        // 4. 图片必须是审核通过、未删除、非草稿的公共空间图片
        if (picture.getReviewStatus() != 1 || picture.getIsDelete() != 0 ||
                picture.getIsDraft() != 0 || picture.getSpaceId() != null) {
            return false;
        }

        // 5. 检查图片是否允许收藏
        Integer allowCollect = picture.getAllowCollect();
        return allowCollect != null && allowCollect == 1;
    }

    /**
     * 检查用户是否有点赞图片的权限
     *
     * @param picture 图片对象
     * @param user    当前登录用户
     * @return 是否有点赞权限
     */
    public boolean canLikePicture(Picture picture, User user) {
        // 1. 管理员可以点赞所有图片
        if (user != null && userService.isAdmin(user)) {
            return picture.getIsDelete() == 0;
        }

        // 2. 未登录用户不能点赞
        if (user == null) {
            return false;
        }

        // 3. 图片作者不能点赞自己的图片
        if (picture.getUserId() != null && user.getId().equals(picture.getUserId())) {
            return false;
        }

        // 4. 图片必须是审核通过、未删除、非草稿的公共空间图片
        if (picture.getReviewStatus() != 1 || picture.getIsDelete() != 0 ||
                picture.getIsDraft() != 0 || picture.getSpaceId() != null) {
            return false;
        }

        // 5. 检查图片是否允许点赞
        Integer allowLike = picture.getAllowLike();
        return allowLike != null && allowLike == 1;
    }

    /**
     * 检查用户是否有评论图片的权限
     *
     * @param picture 图片对象
     * @param user    当前登录用户
     * @return 是否有评论权限
     */
    public boolean canCommentPicture(Picture picture, User user) {
        // 1. 管理员可以评论所有图片
        if (user != null && userService.isAdmin(user)) {
            return picture.getIsDelete() == 0;
        }

        // 2. 未登录用户不能评论
        if (user == null) {
            return false;
        }

        // 3. 图片作者可以评论自己的图片
        if (picture.getUserId() != null && user.getId().equals(picture.getUserId())) {
            return picture.getIsDelete() == 0;
        }

        // 4. 图片必须是审核通过、未删除、非草稿的公共空间图片
        if (picture.getReviewStatus() != 1 || picture.getIsDelete() != 0 ||
                picture.getIsDraft() != 0 || picture.getSpaceId() != null) {
            return false;
        }

        // 5. 检查图片是否允许评论
        Integer allowComment = picture.getAllowComment();
        return allowComment != null && allowComment == 1;
    }

    /**
     * 检查用户是否有分享图片的权限
     *
     * @param picture 图片对象
     * @param user    当前登录用户
     * @return 是否有分享权限
     */
    public boolean canSharePicture(Picture picture, User user) {
        // 1. 管理员可以分享所有图片
        if (user != null && userService.isAdmin(user)) {
            return picture.getIsDelete() == 0;
        }

        // 2. 未登录用户不能分享
        if (user == null) {
            return false;
        }

        // 3. 图片作者不能分享自己的图片（应使用复制链接等方式）
        if (picture.getUserId() != null && user.getId().equals(picture.getUserId())) {
            return false;
        }

        // 4. 图片必须是审核通过、未删除、非草稿的公共空间图片
        if (picture.getReviewStatus() != 1 || picture.getIsDelete() != 0 ||
                picture.getIsDraft() != 0 || picture.getSpaceId() != null) {
            return false;
        }

        // 5. 检查图片是否允许分享
        Integer allowShare = picture.getAllowShare();
        return allowShare != null && allowShare == 1;
    }
}
