package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.FriendLinkMapper;
import com.lumenglover.yuemupicturebackend.model.dto.friendlink.FriendLinkQueryRequest;
import com.lumenglover.yuemupicturebackend.model.entity.FriendLink;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.enums.SiteTypeEnum;
import com.lumenglover.yuemupicturebackend.service.FriendLinkService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 友情链接服务实现类
 */
@Service
@Slf4j
public class FriendLinkServiceImpl extends ServiceImpl<FriendLinkMapper, FriendLink>
        implements FriendLinkService {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public long addFriendLink(FriendLink friendLink, long loginUserId) {
        // 参数校验
        if (friendLink == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String siteName = friendLink.getSiteName();
        String siteUrl = friendLink.getSiteUrl();
        if (StringUtils.isAnyBlank(siteName, siteUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站名称和链接不能为空");
        }

        // 校验网站分类是否合法
        String siteType = friendLink.getSiteType();
        if (StringUtils.isNotBlank(siteType)) {
            try {
                SiteTypeEnum.valueOf(siteType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站分类不合法");
            }
        }

        // 校验用户是否存在
        User user = userService.getById(loginUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 补充信息
        friendLink.setUserId(loginUserId);
        friendLink.setStatus(0); // 待审核
        friendLink.setViewCount(0L);
        friendLink.setClickCount(0L);
        friendLink.setWeight(0);
        friendLink.setCreateTime(new Date());
        friendLink.setUpdateTime(new Date());

        // 保存
        boolean result = this.save(friendLink);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加失败");
        }
        return friendLink.getId();
    }

    @Override
    public boolean deleteFriendLink(long id, long loginUserId) {
        // 校验存在
        FriendLink friendLink = this.getById(id);
        if (friendLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 仅本人或管理员可删除
        User loginUser = userService.getById(loginUserId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (!loginUser.getUserRole().equals("admin") && !friendLink.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = this.removeById(id);
        if (result) {
            // 删除相关缓存
            refreshCache();
        }
        return result;
    }

    @Override
    public boolean updateFriendLink(FriendLink friendLink, long loginUserId) {
        // 参数校验
        if (friendLink == null || friendLink.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验网站分类是否合法
        String siteType = friendLink.getSiteType();
        if (StringUtils.isNotBlank(siteType)) {
            try {
                SiteTypeEnum.valueOf(siteType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站分类不合法");
            }
        }

        // 校验存在
        FriendLink oldLink = this.getById(friendLink.getId());
        if (oldLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 仅本人或管理员可修改
        User loginUser = userService.getById(loginUserId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (!loginUser.getUserRole().equals("admin") && !oldLink.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 管理员可以修改所有字段，普通用户只能修改部分字段
        if (!loginUser.getUserRole().equals("admin")) {
            // 不能修改这些字段
            friendLink.setStatus(oldLink.getStatus());
            friendLink.setReviewMessage(oldLink.getReviewMessage());
            friendLink.setViewCount(oldLink.getViewCount());
            friendLink.setClickCount(oldLink.getClickCount());
            friendLink.setWeight(oldLink.getWeight());
            friendLink.setUserId(oldLink.getUserId());
        }

        friendLink.setUpdateTime(new Date());
        boolean result = this.updateById(friendLink);
        if (result) {
            // 更新成功后刷新缓存
            refreshCache();
        }
        return result;
    }

    @Override
    public FriendLink getFriendLinkById(long id) {
        FriendLink friendLink = this.getById(id);
        if (friendLink != null) {
            // 增加浏览量
            incrementViewCount(id);
            // 设置实时浏览量和点击量
            friendLink.setViewCount(getViewCount(id));
            friendLink.setClickCount(getClickCount(id));

            // 替换URL为自定义域名
            friendLink.replaceUrlWithCustomDomain();
        }
        return friendLink;
    }

    @Override
    public Page<FriendLink> listFriendLinksByPage(FriendLinkQueryRequest friendLinkQueryRequest, HttpServletRequest request) {
        long current = friendLinkQueryRequest.getCurrent();
        long size = friendLinkQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);

        // 构建查询条件
        QueryWrapper<FriendLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);

        // 获取当前登录用户
        User loginUser = userService.isLogin(request);
        boolean isAdmin = loginUser != null && "admin".equals(loginUser.getUserRole());

        // 非管理员只能看到已审核通过的友链
        if (!isAdmin) {
            queryWrapper.eq("status", 1);
        } else if (friendLinkQueryRequest.getStatus() != null) {
            // 管理员可以根据状态筛选
            queryWrapper.eq("status", friendLinkQueryRequest.getStatus());
        }

        // 网站名称模糊搜索
        String siteName = friendLinkQueryRequest.getSiteName();
        if (StrUtil.isNotBlank(siteName)) {
            queryWrapper.like("siteName", siteName);
        }

        // 网站类型筛选
        String siteType = friendLinkQueryRequest.getSiteType();
        if (StrUtil.isNotBlank(siteType)) {
            // 校验分类是否合法
            try {
                SiteTypeEnum.valueOf(siteType.toUpperCase());
                queryWrapper.eq("siteType", siteType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站类型不合法");
            }
        }

        // 排序处理
        String sortField = friendLinkQueryRequest.getSortField();
        String sortOrder = friendLinkQueryRequest.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        } else {
            // 默认按权重和创建时间降序
            queryWrapper.orderByDesc("weight", "createTime");
        }

        // 查询数据库
        Page<FriendLink> friendLinkPage = this.page(new Page<>(current, size), queryWrapper);
        List<FriendLink> records = friendLinkPage.getRecords();

        // 批量获取实时浏览量和点击量，并增加浏览量
        if (records != null && !records.isEmpty()) {
            // 准备Redis keys
            List<String> viewCountKeys = new ArrayList<>();
            List<String> clickCountKeys = new ArrayList<>();
            List<String> viewLockKeys = new ArrayList<>();

            for (FriendLink link : records) {
                String viewCountKey = String.format("friend_link:viewCount:%d", link.getId());
                String clickCountKey = String.format("friend_link:clickCount:%d", link.getId());
                String viewLockKey = String.format("friend_link:viewCount:lock:%d", link.getId());

                viewCountKeys.add(viewCountKey);
                clickCountKeys.add(clickCountKey);
                viewLockKeys.add(viewLockKey);

                // 尝试增加浏览量
                try {
                    // 获取分布式锁
                    Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(viewLockKey, "1", 10, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(locked)) {
                        try {
                            // 增加浏览量
                            Long newCount = stringRedisTemplate.opsForValue().increment(viewCountKey);

                            // 当浏览量达到一定阈值时，更新数据库
                            if (newCount != null && newCount % 100 == 0) {
                                this.update()
                                        .setSql("viewCount = viewCount + " + newCount)
                                        .eq("id", link.getId())
                                        .update();
                                // 更新后重置Redis计数
                                stringRedisTemplate.delete(viewCountKey);
                            }
                        } finally {
                            // 释放锁
                            stringRedisTemplate.delete(viewLockKey);
                        }
                    }
                } catch (Exception e) {
                    log.error("增加浏览量失败, linkId: {}", link.getId(), e);
                }
            }

            // 批量获取Redis中的增量数据
            List<String> viewCounts = stringRedisTemplate.opsForValue().multiGet(viewCountKeys);
            List<String> clickCounts = stringRedisTemplate.opsForValue().multiGet(clickCountKeys);

            // 更新实时数据
            for (int i = 0; i < records.size(); i++) {
                FriendLink link = records.get(i);

                // 更新浏览量
                long baseViewCount = link.getViewCount() != null ? link.getViewCount() : 0L;
                String redisViewCount = viewCounts != null && viewCounts.get(i) != null ? viewCounts.get(i) : "0";
                link.setViewCount(baseViewCount + Long.parseLong(redisViewCount));

                // 更新点击量
                long baseClickCount = link.getClickCount() != null ? link.getClickCount() : 0L;
                String redisClickCount = clickCounts != null && clickCounts.get(i) != null ? clickCounts.get(i) : "0";
                link.setClickCount(baseClickCount + Long.parseLong(redisClickCount));
            }
        }

        // 替换URL为自定义域名
        friendLinkPage.getRecords().forEach(FriendLink::replaceUrlWithCustomDomain);

        return friendLinkPage;
    }

    @Override
    public boolean reviewFriendLink(long id, int status, String reviewMessage, long loginUserId) {
        // 校验是否为管理员
        User loginUser = userService.getById(loginUserId);
        if (loginUser == null || !loginUser.getUserRole().equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能审核");
        }

        // 校验友链是否存在
        FriendLink friendLink = this.getById(id);
        if (friendLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 设置审核信息
        friendLink.setStatus(status);
        friendLink.setReviewMessage(reviewMessage);
        friendLink.setUpdateTime(new Date());

        boolean result = this.updateById(friendLink);
        if (result) {
            // 审核成功后刷新缓存
            refreshCache();
        }
        return result;
    }

    @Override
    public boolean increaseClickCount(long id) {
        // 使用Redis计数
        String clickCountKey = String.format("friend_link:clickCount:%d", id);
        String lockKey = String.format("friend_link:clickCount:lock:%d", id);

        try {
            // 获取分布式锁
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 增加点击量
                stringRedisTemplate.opsForValue().increment(clickCountKey);

                // 当点击量达到一定阈值时，更新数据库
                String clickCountStr = stringRedisTemplate.opsForValue().get(clickCountKey);
                if (clickCountStr != null && Long.parseLong(clickCountStr) % 100 == 0) {
                    boolean result = this.update()
                            .setSql("clickCount = clickCount + " + clickCountStr)
                            .eq("id", id)
                            .update();
                    if (result) {
                        // 更新后重置Redis计数
                        stringRedisTemplate.delete(clickCountKey);
                    }
                    return result;
                }
                return true;
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
        return false;
    }

    @Override
    public List<Map<String, String>> listSiteTypes() {
        return Arrays.stream(SiteTypeEnum.values())
                .map(type -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", type.getName());
                    map.put("value", type.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendLink> listRecommendFriendLinks(int limit) {
        // 构建缓存key
        String cacheKey = RedisConstant.FRIEND_LINK_REDIS_KEY_PREFIX + "recommend:" + limit;

        // 尝试从缓存获取
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            return JSONUtil.toList(cachedValue, FriendLink.class);
        }

        // 缓存未命中，查询数据库
        QueryWrapper<FriendLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                .eq("isDelete", 0)
                .orderByDesc("weight") // 首先按权重排序
                .orderByDesc("clickCount") // 其次按点击量排序
                .orderByDesc("viewCount") // 最后按浏览量排序
                .last("LIMIT " + limit); // 限制返回数量

        List<FriendLink> recommendList = this.list(queryWrapper);

        // 更新缓存
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300); // 5-10分钟过期
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(recommendList),
                cacheExpireTime, TimeUnit.SECONDS);

        return recommendList;
    }

    @Override
    public boolean updateWeight(long id, int weight, long loginUserId) {
        // 校验是否为管理员
        User loginUser = userService.getById(loginUserId);
        if (loginUser == null || !loginUser.getUserRole().equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能修改权重");
        }

        // 校验友链是否存在
        FriendLink friendLink = this.getById(id);
        if (friendLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 更新权重
        friendLink.setWeight(weight);
        friendLink.setUpdateTime(new Date());

        boolean result = this.updateById(friendLink);
        if (result) {
            // 更新成功后刷新缓存
            refreshCache();
        }
        return result;
    }

    @Override
    public long countByType(String siteType) {
        if (StringUtils.isBlank(siteType)) {
            return 0;
        }

        // 校验分类是否合法
        try {
            SiteTypeEnum.valueOf(siteType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站分类不合法");
        }

        // 构建缓存key
        String cacheKey = RedisConstant.FRIEND_LINK_REDIS_KEY_PREFIX + "count:" + siteType;

        // 尝试从缓存获取
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            return Long.parseLong(cachedValue);
        }

        // 缓存未命中，查询数据库
        QueryWrapper<FriendLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                .eq("isDelete", 0)
                .eq("siteType", siteType);

        long count = this.count(queryWrapper);

        // 更新缓存
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300); // 5-10分钟过期
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(count),
                cacheExpireTime, TimeUnit.SECONDS);

        return count;
    }

    @Override
    public long getViewCount(Long id) {
        if (id == null) {
            return 0L;
        }

        // 先从Redis获取增量
        String viewCountKey = String.format("friend_link:viewCount:%d", id);
        String incrementCount = stringRedisTemplate.opsForValue().get(viewCountKey);

        // 从数据库获取基础浏览量
        FriendLink friendLink = this.getById(id);
        if (friendLink == null) {
            return 0L;
        }

        // 合并数据库和Redis的浏览量
        long baseCount = friendLink.getViewCount() != null ? friendLink.getViewCount() : 0L;
        long increment = incrementCount != null ? Long.parseLong(incrementCount) : 0L;

        return baseCount + increment;
    }

    /**
     * 获取实时点击量
     */
    public long getClickCount(Long id) {
        if (id == null) {
            return 0L;
        }

        // 先从Redis获取增量
        String clickCountKey = String.format("friend_link:clickCount:%d", id);
        String incrementCount = stringRedisTemplate.opsForValue().get(clickCountKey);

        // 从数据库获取基础点击量
        FriendLink friendLink = this.getById(id);
        if (friendLink == null) {
            return 0L;
        }

        // 合并数据库和Redis的点击量
        long baseCount = friendLink.getClickCount() != null ? friendLink.getClickCount() : 0L;
        long increment = incrementCount != null ? Long.parseLong(incrementCount) : 0L;

        return baseCount + increment;
    }

    /**
     * 增加浏览量
     */
    private void incrementViewCount(Long id) {
        // 使用Redis进行计数
        String viewCountKey = String.format("friend_link:viewCount:%d", id);
        String lockKey = String.format("friend_link:viewCount:lock:%d", id);

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
                            .eq("id", id)
                            .update();
                    // 更新后重置Redis计数
                    stringRedisTemplate.delete(viewCountKey);
                }
            }
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 每天凌晨2点自动刷新缓存
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Override
    public void refreshCache() {
        // 清除所有友链相关的缓存
        Set<String> keys = stringRedisTemplate.keys(RedisConstant.FRIEND_LINK_REDIS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
