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
    public BaseResponse<UserHighestScoreVO> getUserAllHighestScores(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserHighestScoreVO scores = snakeGameService.getUserAllHighestScores(loginUser.getId());
        return ResultUtils.success(scores);
    }

    /**
     * 获取排行榜
     */
    @PostMapping("/ranking")
    public BaseResponse<List<SnakeGameRecord>> getRankingList(@RequestBody GameRankingRequest request) {
        List<SnakeGameRecord> rankingList = snakeGameService.getRankingList(request);
        return ResultUtils.success(rankingList);
    }
}
