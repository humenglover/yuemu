package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.manager.auth.StpKit;
import com.lumenglover.yuemupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.lumenglover.yuemupicturebackend.model.dto.picture.PictureQueryRequest;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserFollowsAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserfollowsQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.PictureReviewStatusEnum;
import com.lumenglover.yuemupicturebackend.model.vo.FollowersAndFansVO;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.service.UserFollowsService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/userfollows")
public class UserFollowsController {
   @Resource
   private UserFollowsService userfollowsService;
   @Resource
   private PictureService pictureService;

   @Resource
   private UserService userService;

   /**
    * 关注、取关
    */
   @PostMapping("/adduserfollows")
   @RateLimiter(key = "user_follows_add", time = 60, count = 10, message = "关注操作过于频繁，请稍后再试")
   public BaseResponse<Boolean> addUserFollows(@RequestBody UserFollowsAddRequest userFollowsAddRequest, HttpServletRequest request){
      // 检查目标用户是否允许被关注
      User targetUser = userService.getById(userFollowsAddRequest.getFollowingId());
      if (targetUser != null && targetUser.getAllowFollow() != null && targetUser.getAllowFollow() == 0) {
         throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "对方不允许被关注");
      }
      return ResultUtils.success(userfollowsService.addUserFollows(userFollowsAddRequest));
   }

   /**
    * 查找是否关注
    */
   @PostMapping("/findisfollow")
   @RateLimiter(key = "user_follows_find", time = 60, count = 30, message = "关注状态查询过于频繁，请稍后再试")
   public BaseResponse<Boolean> findIsFollow(@RequestBody UserFollowsIsFollowsRequest userFollowsIsFollowsRequest){
      return ResultUtils.success(userfollowsService.findIsFollow(userFollowsIsFollowsRequest));
   }

   /**
    * 得到关注,粉丝列表
    */
   @PostMapping("/getfolloworfanlist")
   @RateLimiter(key = "user_follows_list", time = 60, count = 20, message = "关注列表查询过于频繁，请稍后再试")
   public BaseResponse<Page<UserVO>> getFollowOrFanList(@RequestBody UserfollowsQueryRequest userfollowsQueryRequest, HttpServletRequest request){
      // 检查目标用户是否允许展示其关注或粉丝列表
      Long targetUserId = userfollowsQueryRequest.getFollowingId() != null ? userfollowsQueryRequest.getFollowingId() : userfollowsQueryRequest.getFollowerId();
      User targetUser = userService.getById(targetUserId);
      if (targetUser != null) {
         // 如果查询关注列表（searchType为0），检查是否允许展示关注列表
         if (userfollowsQueryRequest.getSearchType() == 0 && targetUser.getShowFollowList() != null && targetUser.getShowFollowList() == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "对方不允许展示关注列表");
         }
         // 如果查询粉丝列表（searchType为1），检查是否允许展示粉丝列表
         else if (userfollowsQueryRequest.getSearchType() == 1 && targetUser.getShowFansList() != null && targetUser.getShowFansList() == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "对方不允许展示粉丝列表");
         }
      }
      return ResultUtils.success(userfollowsService.getFollowOrFanList(userfollowsQueryRequest));
   }

   /**
    * 得到关注和粉丝数量
    */
   @PostMapping("/getfollowandfanscount/{id}")
   @RateLimiter(key = "user_follows_count", time = 60, count = 50, message = "关注数量查询过于频繁，请稍后再试")
   public BaseResponse<FollowersAndFansVO> getFollowAndFansCount(@PathVariable Long id, HttpServletRequest request){
      // 检查目标用户是否允许展示其关注或粉丝列表（这通常意味着也允许查看数量）
      User targetUser = userService.getById(id);
      if (targetUser != null && targetUser.getShowFansList() != null && targetUser.getShowFansList() == 0) {
         throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "对方不允许展示粉丝列表");
      }
      return ResultUtils.success(userfollowsService.getFollowAndFansCount(id));

   }

   /**
    * 得到关注或者粉丝的公共的图片数据
    */
   @PostMapping("/getfolloworfanpicture")
   @RateLimiter(key = "user_follows_picture", time = 60, count = 20, message = "关注者图片查询过于频繁，请稍后再试")
   public BaseResponse<Page<PictureVO>> getFollowOrFanPicture(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request){
      long current = pictureQueryRequest.getCurrent();
      long size = pictureQueryRequest.getPageSize();
      // 限制爬虫
      ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);


      ThrowUtils.throwIf(pictureQueryRequest.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户id不能为空");
      pictureQueryRequest.setUserId(pictureQueryRequest.getUserId());
      pictureQueryRequest.setNullSpaceId(true);
      pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

      // 查询数据库
      Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
              pictureService.getQueryWrapper(pictureQueryRequest));
      // 获取封装类
      return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
   }
}
