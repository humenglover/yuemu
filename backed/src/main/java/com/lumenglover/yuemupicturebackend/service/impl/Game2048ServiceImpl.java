package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.Game2048Mapper;
import com.lumenglover.yuemupicturebackend.model.dto.game2048.SaveGameRecordRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Game2048Record;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.Game2048RecordVO;
import com.lumenglover.yuemupicturebackend.service.Game2048Service;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Game2048ServiceImpl extends ServiceImpl<Game2048Mapper, Game2048Record>
        implements Game2048Service {

    @Resource
    private Game2048Mapper game2048Mapper;

    @Override
    public Game2048Record saveGameRecord(SaveGameRecordRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 校验参数
        Integer score = request.getScore();
        Integer maxTile = request.getMaxTile();
        Integer gameTime = request.getGameTime();
        Integer moveCount = request.getMoveCount();

        ThrowUtils.throwIf(score == null || score < 0, ErrorCode.PARAMS_ERROR, "分数无效");
        ThrowUtils.throwIf(maxTile == null || maxTile < 2 || (maxTile & (maxTile - 1)) != 0,
                ErrorCode.PARAMS_ERROR, "最大数字无效");
        ThrowUtils.throwIf(gameTime == null || gameTime < 0, ErrorCode.PARAMS_ERROR, "游戏时长无效");
        ThrowUtils.throwIf(moveCount == null || moveCount < 0, ErrorCode.PARAMS_ERROR, "移动次数无效");

        // 检查是否存在相同记录
        Game2048Record existingRecord = lambdaQuery()
                .eq(Game2048Record::getUserId, loginUser.getId())
                .eq(Game2048Record::getScore, score)
                .eq(Game2048Record::getMaxTile, maxTile)
                .eq(Game2048Record::getGameTime, gameTime)
                .eq(Game2048Record::getMoveCount, moveCount)
                .orderByDesc(Game2048Record::getCreateTime)
                .last("LIMIT 1")
                .one();

        // 如果存在相同记录且创建时间在1分钟内，则不保存
        if (existingRecord != null &&
                System.currentTimeMillis() - existingRecord.getCreateTime().getTime() < 60000) {
            return existingRecord;
        }

        // 构建记录
        Game2048Record record = new Game2048Record();
        record.setUserId(loginUser.getId());
        record.setScore(score);
        record.setMaxTile(maxTile);
        record.setGameTime(gameTime);
        record.setMoveCount(moveCount);

        // 保存记录
        boolean success = this.save(record);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "保存游戏记录失败");

        return record;
    }

    @Override
    public Integer getUserHighestScore(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        return game2048Mapper.getUserHighestScore(userId);
    }

    @Override
    public List<Game2048RecordVO> getRankingList(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        // 限制最大查询数量
        limit = Math.min(limit, 100);
        // 修改为获取每个用户的最高分记录
        return game2048Mapper.getRankingListByHighestScore(limit);
    }

    @Override
    public Map<String, Object> getUserGameHistory(Long userId, Integer current, Integer pageSize) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);

        // 默认值处理
        if (current == null || current < 1) {
            current = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制每页最大条数
        pageSize = Math.min(pageSize, 100);

        // 计算偏移量
        int offset = (current - 1) * pageSize;

        // 获取数据
        List<Game2048Record> records = game2048Mapper.getUserGameHistory(userId, offset, pageSize);
        Long total = game2048Mapper.getUserGameHistoryTotal(userId);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", current);
        result.put("pageSize", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);

        return result;
    }
}
