package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.friendlink.FriendLinkQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.FriendLink;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 友情链接服务
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 创建友链
     * @param friendLink 友链信息
     * @param loginUserId 当前登录用户id
     * @return 新创建的友链id
     */
    long addFriendLink(FriendLink friendLink, long loginUserId);

    /**
     * 删除友链
     * @param id 友链id
     * @param loginUserId 当前登录用户id
     * @return 是否成功
     */
    boolean deleteFriendLink(long id, long loginUserId);

    /**
     * 更新友链
     * @param friendLink 友链信息
     * @param loginUserId 当前登录用户id
     * @return 是否成功
     */
    boolean updateFriendLink(FriendLink friendLink, long loginUserId);

    /**
     * 根据id获取友链
     * @param id 友链id
     * @return 友链信息
     */
    FriendLink getFriendLinkById(long id);

    /**
     * 分页获取友链列表
     * @param friendLinkQueryRequest 查询条件
     * @param request HTTP请求
     * @return 友链分页列表
     */
    Page<FriendLink> listFriendLinksByPage(FriendLinkQueryRequest friendLinkQueryRequest, HttpServletRequest request);

    /**
     * 审核友链
     * @param id 友链id
     * @param status 审核状态
     * @param reviewMessage 审核信息
     * @param loginUserId 当前登录用户id
     * @return 是否成功
     */
    boolean reviewFriendLink(long id, int status, String reviewMessage, long loginUserId);

    /**
     * 增加点击量
     * @param id 友链id
     * @return 是否成功
     */
    boolean increaseClickCount(long id);

    /**
     * 获取所有网站类型
     * @return 网站类型列表
     */
    List<Map<String, String>> listSiteTypes();

    /**
     * 获取推荐友链列表
     * @param limit 限制数量
     * @return 推荐友链列表
     */
    List<FriendLink> listRecommendFriendLinks(int limit);

    /**
     * 更新友链权重
     * @param id 友链id
     * @param weight 权重值
     * @param loginUserId 当前登录用户id
     * @return 是否成功
     */
    boolean updateWeight(long id, int weight, long loginUserId);

    /**
     * 统计某类型的友链数量
     * @param siteType 网站类型
     * @return 友链数量
     */
    long countByType(String siteType);

    /**
     * 获取实时浏览量
     * @param id 友链id
     * @return 浏览量
     */
    long getViewCount(Long id);

    /**
     * 刷新缓存
     */
    void refreshCache();
}
