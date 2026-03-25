package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.SnakeGameMapper;
import com.lumenglover.yuemupicturebackend.model.dto.snake.GameRankingRequest;
import com.lumenglover.yuemupicturebackend.model.dto.snake.SaveGameRecordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.SnakeGameRecord;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.UserHighestScoreVO;
import com.lumenglover.yuemupicturebackend.service.SnakeGameService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SnakeGameServiceImpl extends ServiceImpl<SnakeGameMapper, SnakeGameRecord>
        implements SnakeGameService {

    @Resource
    private SnakeGameMapper snakeGameMapper;

    @Override
    public SnakeGameRecord saveGameRecord(SaveGameRecordRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 校验游戏模式
        Integer gameMode = request.getGameMode();
        ThrowUtils.throwIf(gameMode == null || gameMode < 1 || gameMode > 3,
                ErrorCode.PARAMS_ERROR, "无效的游戏模式");

        // 构建记录
        SnakeGameRecord record = new SnakeGameRecord();
        record.setUserId(loginUser.getId());
        record.setScore(request.getScore());
        record.setFoodCount(request.getFoodCount());
        record.setGameTime(request.getGameTime());
        record.setSnakeLength(request.getSnakeLength());
        record.setGameMode(gameMode);

        // 保存记录
        boolean success = this.save(record);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "保存游戏记录失败");

        return record;
    }

    @Override
    public UserHighestScoreVO getUserAllHighestScores(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);

        UserHighestScoreVO vo = new UserHighestScoreVO();
        vo.setClassicModeScore(snakeGameMapper.getUserHighestScore(userId, 1));
        vo.setNoWallModeScore(snakeGameMapper.getUserHighestScore(userId, 2));
        vo.setSpeedModeScore(snakeGameMapper.getUserHighestScore(userId, 3));

        return vo;
    }

    @Override
    public List<SnakeGameRecord> getRankingList(GameRankingRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        Integer gameMode = request.getGameMode();
        ThrowUtils.throwIf(gameMode == null || gameMode < 1 || gameMode > 3,
                ErrorCode.PARAMS_ERROR, "无效的游戏模式");

        Integer limit = request.getLimit();
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        // 限制最大查询数量
        limit = Math.min(limit, 100);

        return snakeGameMapper.getRankingList(gameMode, limit);
    }
}
