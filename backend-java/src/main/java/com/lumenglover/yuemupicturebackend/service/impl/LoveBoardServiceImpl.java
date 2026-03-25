package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.LoveBoardMapper;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoard;
import com.lumenglover.yuemupicturebackend.service.LoveBoardService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardAdminRequest;
import com.lumenglover.yuemupicturebackend.model.dto.loveboard.LoveBoardBatchRequest;
import com.lumenglover.yuemupicturebackend.utils.SqlUtils;
import com.lumenglover.yuemupicturebackend.constant.CommonConstant;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

/**
 * 恋爱画板服务实现类
 */
@Service
public class LoveBoardServiceImpl extends ServiceImpl<LoveBoardMapper, LoveBoard>
        implements LoveBoardService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOVE_BOARD_VIEW_COUNT_KEY = "love_board:view_count:";
    private static final long CACHE_EXPIRE_TIME = 7;
    private static final TimeUnit CACHE_EXPIRE_TIME_UNIT = TimeUnit.DAYS;

    @Override
    public boolean hasLoveBoard(long userId) {
        QueryWrapper<LoveBoard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createLoveBoard(LoveBoard loveBoard, long loginUserId) {
        if (loveBoard == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查用户是否已经创建过恋爱画板
        if (hasLoveBoard(loginUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经创建过恋爱画板了");
        }
        // 设置创建者
        loveBoard.setUserId(loginUserId);
        // 设置默认值
        loveBoard.setLikeCount(0L);
        loveBoard.setStatus(1);
        boolean result = this.save(loveBoard);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败");
        }
        return loveBoard.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLoveBoard(LoveBoard loveBoard, long loginUserId) {
        if (loveBoard == null || loveBoard.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        LoveBoard oldLoveBoard = this.getById(loveBoard.getId());
        if (oldLoveBoard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人可修改
        if (!oldLoveBoard.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 不允许修改的字段
        loveBoard.setUserId(oldLoveBoard.getUserId());
        loveBoard.setCreateTime(oldLoveBoard.getCreateTime());
        loveBoard.setLikeCount(oldLoveBoard.getLikeCount());
        return this.updateById(loveBoard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLoveBoard(long id, long loginUserId) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoveBoard loveBoard = this.getById(id);
        if (loveBoard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人可删除
        if (!loveBoard.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return this.removeById(id);
    }

    @Override
    public LoveBoard getLoveBoardById(long id, Long loginUserId) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoveBoard loveBoard = this.getById(id);
        if (loveBoard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 如果是画板主人，直接返回
        if (loginUserId != null && loginUserId.equals(loveBoard.getUserId())) {
            // 即使是主人访问也增加浏览量
            Long realTimeViewCount = incrementViewCount(id);
            loveBoard.setViewCount(realTimeViewCount);

            // 替换URL为自定义域名
            loveBoard.replaceUrlWithCustomDomain();
            return loveBoard;
        }

        // 如果不是画板主人，检查是否开放查看权限
        if (loveBoard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该恋爱画板未开放查看权限");
        }

        // 增加浏览量并设置实时浏览量
        Long realTimeViewCount = incrementViewCount(id);
        loveBoard.setViewCount(realTimeViewCount);

        // 替换URL为自定义域名
        loveBoard.replaceUrlWithCustomDomain();
        return loveBoard;
    }

    @Override
    public boolean isLoveBoardOwner(long loveBoardId, long userId) {
        if (loveBoardId <= 0 || userId <= 0) {
            return false;
        }
        // 查询恋爱板
        LoveBoard loveBoard = this.getById(loveBoardId);
        if (loveBoard == null || loveBoard.getIsDelete() == 1) {
            return false;
        }
        // 检查用户是否是恋爱板的所有者
        return loveBoard.getUserId().equals(userId);
    }

    @Override
    public Long incrementViewCount(Long loveBoardId) {
        String key = LOVE_BOARD_VIEW_COUNT_KEY + loveBoardId;
        try {
            // 获取当前 Redis 中的浏览量
            Object currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                // 如果 Redis 中没有，从数据库加载
                LoveBoard loveBoard = this.getById(loveBoardId);
                if (loveBoard != null) {
                    // 设置初始值为数据库值
                    redisTemplate.opsForValue().set(key, loveBoard.getViewCount());
                    currentCount = loveBoard.getViewCount();
                } else {
                    currentCount = 0L;
                }
            }

            // 增加浏览量
            Long newCount = redisTemplate.opsForValue().increment(key);

            // 每增加10次就同步一次数据库
            if (newCount % 10 == 0) {
                syncViewCountToDb(loveBoardId);
            }

            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_TIME, CACHE_EXPIRE_TIME_UNIT);

            return newCount;
        } catch (Exception e) {
            log.error("增加浏览量失败, loveBoardId: {}");
            // 如果 Redis 操作失败，直接更新数据库
            LoveBoard loveBoard = this.getById(loveBoardId);
            if (loveBoard != null) {
                Long newCount = loveBoard.getViewCount() + 1;
                loveBoard.setViewCount(newCount);
                this.updateById(loveBoard);
                return newCount;
            }
            return 0L;
        }
    }

    @Override
    public Long getViewCount(Long loveBoardId) {
        String key = LOVE_BOARD_VIEW_COUNT_KEY + loveBoardId;
        try {
            Object viewCount = redisTemplate.opsForValue().get(key);
            if (viewCount != null) {
                return Long.parseLong(viewCount.toString());
            }
        } catch (Exception e) {
            log.error("获取浏览量失败, loveBoardId: {}");
        }

        // 如果 Redis 获取失败，从数据库获取
        LoveBoard loveBoard = this.getById(loveBoardId);
        if (loveBoard != null) {
            // 重新设置到 Redis
            redisTemplate.opsForValue().set(key, loveBoard.getViewCount(), CACHE_EXPIRE_TIME, CACHE_EXPIRE_TIME_UNIT);
            return loveBoard.getViewCount();
        }
        return 0L;
    }

    @Override
    public void syncViewCountToDb(Long loveBoardId) {
        String key = LOVE_BOARD_VIEW_COUNT_KEY + loveBoardId;
        try {
            Object viewCount = redisTemplate.opsForValue().get(key);
            if (viewCount != null) {
                this.update()
                        .set("viewCount", Long.parseLong(viewCount.toString()))
                        .eq("id", loveBoardId)
                        .update();
            }
        } catch (Exception e) {
            log.error("同步浏览量到数据库失败, loveBoardId: {}");
        }
    }

    /**
     * 每天凌晨3点同步浏览量到数据库
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledSyncViewCount() {
        // 获取所有需要同步的key
        String pattern = LOVE_BOARD_VIEW_COUNT_KEY + "*";
        redisTemplate.keys(pattern).forEach(key -> {
            String loveBoardIdStr = key.substring(LOVE_BOARD_VIEW_COUNT_KEY.length());
            try {
                Long loveBoardId = Long.parseLong(loveBoardIdStr);
                syncViewCountToDb(loveBoardId);
            } catch (NumberFormatException ignored) {
                // 忽略非法的key
            }
        });
    }

    /**
     * 获取管理员查询条件构造器
     */
    @Override
    public QueryWrapper<LoveBoard> getAdminQueryWrapper(LoveBoardAdminRequest loveBoardAdminRequest) {
        if (loveBoardAdminRequest == null) {
            return null;
        }
        Long id = loveBoardAdminRequest.getId();
        Long userId = loveBoardAdminRequest.getUserId();
        String manName = loveBoardAdminRequest.getManName();
        String womanName = loveBoardAdminRequest.getWomanName();
        Integer status = loveBoardAdminRequest.getStatus();
        String sortField = loveBoardAdminRequest.getSortField();
        String sortOrder = loveBoardAdminRequest.getSortOrder();

        QueryWrapper<LoveBoard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(manName), "manName", manName);
        queryWrapper.like(StringUtils.isNotBlank(womanName), "womanName", womanName);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 批量操作恋爱画板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperationLoveBoards(LoveBoardBatchRequest loveBoardBatchRequest) {
        List<Long> ids = loveBoardBatchRequest.getIds();
        String operation = loveBoardBatchRequest.getOperation();

        if (operation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 根据操作类型执行相应的批量操作
        boolean result = false;
        switch (operation) {
            case "delete":
                result = this.removeByIds(ids);
                break;
            case "recover":
                result = this.update()
                        .set("isDelete", 0)
                        .in("id", ids)
                        .update();
                break;
            case "physical":
                result = this.removeBatchByIds(ids);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return result;
    }

    @Override
    public Page<LoveBoard> listPublicLoveBoards(long current, long size, String manName, String womanName, String sortField, String sortOrder) {
        // 限制分页参数
        if (size > 50) {
            size = 50;
        }
        Page<LoveBoard> page = new Page<>(current, size);
        QueryWrapper<LoveBoard> queryWrapper = new QueryWrapper<>();
        // 仅查询状态为开放且未删除的画板
        queryWrapper.eq("status", 1);
        queryWrapper.eq("isDelete", 0);
        // 可选模糊查询
        queryWrapper.like(StringUtils.isNotBlank(manName), "manName", manName);
        queryWrapper.like(StringUtils.isNotBlank(womanName), "womanName", womanName);

        // 处理排序
        if (StringUtils.isNotBlank(sortField)) {
            // 防止SQL注入，只允许特定字段排序
            if ("viewCount".equals(sortField) || "createTime".equals(sortField)) {
                if ("asc".equalsIgnoreCase(sortOrder)) {
                    queryWrapper.orderByAsc(sortField);
                } else {
                    queryWrapper.orderByDesc(sortField);
                }
            } else {
                // 默认按浏览量倒序
                queryWrapper.orderByDesc("viewCount");
            }
        } else {
            // 默认按浏览量倒序
            queryWrapper.orderByDesc("viewCount");
        }

        return this.page(page, queryWrapper);
    }
}
