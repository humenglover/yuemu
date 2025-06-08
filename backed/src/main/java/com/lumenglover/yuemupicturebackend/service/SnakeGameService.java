package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.dto.snake.GameRankingRequest;
import com.lumenglover.yuemupicturebackend.model.dto.snake.SaveGameRecordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.SnakeGameRecord;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.UserHighestScoreVO;

import java.util.List;

public interface SnakeGameService {

    /**
     * 保存游戏记录
     * @param request 游戏记录请求
     * @param loginUser 当前登录用户
     * @return 保存的记录
     */
    SnakeGameRecord saveGameRecord(SaveGameRecordRequest request, User loginUser);

    /**
     * 获取用户所有模式最高分
     * @param userId 用户ID
     * @return 用户所有模式最高分
     */
    UserHighestScoreVO getUserAllHighestScores(Long userId);

    /**
     * 获取排行榜
     * @param request 排行榜请求
     * @return 排行榜记录列表
     */
    List<SnakeGameRecord> getRankingList(GameRankingRequest request);
}
