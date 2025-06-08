package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.WeiYanMapper;
import com.lumenglover.yuemupicturebackend.model.entity.WeiYan;
import com.lumenglover.yuemupicturebackend.service.WeiYanService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 微言服务实现类
 */
@Service
public class WeiYanServiceImpl extends ServiceImpl<WeiYanMapper, WeiYan>
        implements WeiYanService {

    @Override
    public long addWeiYan(WeiYan weiYan, long loveBoardId, long userId) {
        if (weiYan == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 设置恋爱板ID和用户ID
        weiYan.setLoveBoardId(loveBoardId);
        weiYan.setUserId(userId);
        boolean result = this.save(weiYan);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return weiYan.getId();
    }

    @Override
    public boolean deleteWeiYan(long id, long loveBoardId, long userId) {
        // 判断是否存在
        WeiYan oldWeiYan = this.getById(id);
        if (oldWeiYan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅恋爱板所有者或发布者可删除
        if (!oldWeiYan.getLoveBoardId().equals(loveBoardId) && !oldWeiYan.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return this.removeById(id);
    }

    @Override
    public boolean updateWeiYan(WeiYan weiYan, long loveBoardId, long userId) {
        if (weiYan == null || weiYan.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        WeiYan oldWeiYan = this.getById(weiYan.getId());
        if (oldWeiYan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅恋爱板所有者或发布者可修改
        if (!oldWeiYan.getLoveBoardId().equals(loveBoardId) && !oldWeiYan.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 不允许修改的字段
        weiYan.setLoveBoardId(oldWeiYan.getLoveBoardId());
        weiYan.setUserId(oldWeiYan.getUserId());
        return this.updateById(weiYan);
    }

    @Override
    public WeiYan getWeiYanById(long id, long loveBoardId, Long userId) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WeiYan weiYan = this.getById(id);
        if (weiYan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 如果是恋爱板所有者或发布者，可以查看所有内容
        if (weiYan.getLoveBoardId().equals(loveBoardId) || (userId != null && userId.equals(weiYan.getUserId()))) {
            return weiYan;
        }
        // 其他用户只能查看公开内容
        if (weiYan.getIsPublic() == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return weiYan;
    }

    @Override
    public List<WeiYan> listLoveBoardWeiYan(long loveBoardId, Long userId) {
        QueryWrapper<WeiYan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("loveBoardId", loveBoardId);
        // 如果不是恋爱板所有者且不是发布者，只能看到公开内容
        if (userId != null) {
            queryWrapper.and(wrapper ->
                    wrapper.eq("isPublic", 1)
                            .or()
                            .eq("userId", userId)
            );
        } else {
            queryWrapper.eq("isPublic", 1);
        }
        queryWrapper.orderByDesc("createTime");
        return this.list(queryWrapper);
    }

    @Override
    public List<WeiYan> listPublicWeiYan() {
        QueryWrapper<WeiYan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isPublic", 1);
        queryWrapper.orderByDesc("createTime");
        return this.list(queryWrapper);
    }

    @Override
    public Page<WeiYan> listWeiYan(Long loveBoardId, Long userId, long current, long pageSize) {
        QueryWrapper<WeiYan> queryWrapper = new QueryWrapper<>();

        // 如果提供了恋爱板ID，添加恋爱板筛选条件
        if (loveBoardId != null && loveBoardId > 0) {
            queryWrapper.eq("loveBoardId", loveBoardId);

            // 如果提供了用户ID且是恋爱板主人，不添加公开条件（可以看到所有内容）
            if (userId == null) {
                // 未提供用户ID，只能看到公开内容
                queryWrapper.eq("isPublic", 1);
            }
        } else {
            // 没有提供恋爱板ID，只显示公开内容
            queryWrapper.eq("isPublic", 1);
        }

        // 按创建时间倒序排序
        queryWrapper.orderByDesc("createTime");

        // 执行分页查询
        return this.page(new Page<>(current, pageSize), queryWrapper);
    }

    @Override
    public boolean likeWeiYan(long id) {
        // 判断是否存在
        WeiYan weiYan = this.getById(id);
        if (weiYan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 更新点赞数
        weiYan.setLikeCount(weiYan.getLikeCount() + 1);
        return this.updateById(weiYan);
    }
}
