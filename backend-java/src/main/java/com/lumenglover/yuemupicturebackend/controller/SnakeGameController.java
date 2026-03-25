package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.model.dto.snake.GameRankingRequest;
import com.lumenglover.yuemupicturebackend.model.dto.snake.SaveGameRecordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.SnakeGameRecord;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.UserHighestScoreVO;
import com.lumenglover.yuemupicturebackend.service.SnakeGameService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/snake")
public class SnakeGameController {

    @Resource
    private SnakeGameService snakeGameService;

    @Resource
    private UserService userService;

    /**
     * 保存游戏记录
     */
    @PostMapping("/save")
    @RateLimiter(key = "snake_game_save", time = 3600, count = 50, message = "游戏记录保存过于频繁，请稍后再试")
    public BaseResponse<SnakeGameRecord> saveGameRecord(@RequestBody SaveGameRecordRequest request,
                                                        HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        SnakeGameRecord record = snakeGameService.saveGameRecord(request, loginUser);
        return ResultUtils.success(record);
    }

    /**
     * 获取用户所有模式最高分
     */
    @GetMapping("/highest/all")
    @RateLimiter(key = "snake_game_highest", time = 60, count = 30, message = "最高分查询过于频繁，请稍后再试")
    public BaseResponse<UserHighestScoreVO> getUserAllHighestScores(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserHighestScoreVO scores = snakeGameService.getUserAllHighestScores(loginUser.getId());
        return ResultUtils.success(scores);
    }

    /**
     * 获取排行榜
     */
    @PostMapping("/ranking")
    @RateLimiter(key = "snake_game_ranking", time = 60, count = 30, message = "排行榜查询过于频繁，请稍后再试")
    public BaseResponse<List<SnakeGameRecord>> getRankingList(@RequestBody GameRankingRequest request) {
        List<SnakeGameRecord> rankingList = snakeGameService.getRankingList(request);
        return ResultUtils.success(rankingList);
    }
}
