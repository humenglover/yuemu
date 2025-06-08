package com.lumenglover.yuemupicturebackend.controller;

import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.model.dto.game2048.SaveGameRecordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Game2048Record;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.Game2048RecordVO;
import com.lumenglover.yuemupicturebackend.service.Game2048Service;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/game2048")
public class Game2048Controller {

    @Resource
    private Game2048Service game2048Service;

    @Resource
    private UserService userService;

    /**
     * 保存游戏记录
     */
    @PostMapping("/save")
    public BaseResponse<Game2048Record> saveGame2048Record(@RequestBody SaveGameRecordRequest request,
                                                           HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Game2048Record record = game2048Service.saveGameRecord(request, loginUser);
        return ResultUtils.success(record);
    }

    /**
     * 获取用户最高分
     */
    @GetMapping("/highest")
    public BaseResponse<Integer> getUserHighestScore(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Integer highestScore = game2048Service.getUserHighestScore(loginUser.getId());
        return ResultUtils.success(highestScore);
    }

    /**
     * 获取排行榜
     */
    @GetMapping("/ranking")
    public BaseResponse<List<Game2048RecordVO>> getRankingList(@RequestParam(required = false) Integer limit) {
        List<Game2048RecordVO> rankingList = game2048Service.getRankingList(limit);
        return ResultUtils.success(rankingList);
    }

    /**
     * 获取用户游戏历史记录
     */
    @GetMapping("/history")
    public BaseResponse<Map<String, Object>> getUserGameHistory(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Map<String, Object> result = game2048Service.getUserGameHistory(loginUser.getId(), current, pageSize);
        return ResultUtils.success(result);
    }
}
