package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserFollowsAddRequest;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.lumenglover.yuemupicturebackend.model.dto.userfollows.UserfollowsQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Userfollows;
import com.lumenglover.yuemupicturebackend.model.vo.FollowersAndFansVO;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;

import java.util.List;

/**
* @author 鹿梦
* @description 针对表【userfollows】的数据库操作Service
* @createDate 2025-01-14 20:49:17
*/
public interface UserFollowsService extends IService<Userfollows> {

    Boolean addUserFollows(UserFollowsAddRequest userFollowsAddRequest);

    Page<UserVO> getFollowOrFanList(UserfollowsQueryRequest userfollowsQueryRequest);

    Boolean findIsFollow(UserFollowsIsFollowsRequest userFollowsIsFollowsRequest);

    List<Long> getFollowList(Long id);

    FollowersAndFansVO getFollowAndFansCount(Long id);
}
