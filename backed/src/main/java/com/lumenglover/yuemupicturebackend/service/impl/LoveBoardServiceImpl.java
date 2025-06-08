package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.LoveBoardMapper;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoard;
import com.lumenglover.yuemupicturebackend.service.LoveBoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 恋爱画板服务实现类
 */
@Service
public class LoveBoardServiceImpl extends ServiceImpl<LoveBoardMapper, LoveBoard>
        implements LoveBoardService {

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
            return loveBoard;
        }

        // 如果不是画板主人，检查是否开放查看权限
        if (loveBoard.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该恋爱画板未开放查看权限");
        }

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
}
