package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.Game2048Record;
import com.lumenglover.yuemupicturebackend.model.vo.Game2048RecordVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface Game2048Mapper extends BaseMapper<Game2048Record> {

    /**
     * 获取用户最高分
     * @param userId 用户ID
     * @return 最高分
     */
    Integer getUserHighestScore(@Param("userId") Long userId);

    /**
     * 获取排行榜（每个用户只返回最高分记录）
     * @param limit 限制数量
     * @return 排行榜记录
     */
    @Select("SELECT r.*, u.userAccount, u.userName, u.userAvatar " +
            "FROM game_2048_record r " +
            "JOIN user u ON r.userId = u.id " +
            "WHERE r.isDelete = 0 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM game_2048_record r2 " +
            "    WHERE r2.userId = r.userId " +
            "    AND r2.isDelete = 0 " +
            "    AND (r2.score > r.score OR (r2.score = r.score AND r2.gameTime < r.gameTime))" +
            ") " +
            "ORDER BY r.score DESC, r.gameTime ASC " +
            "LIMIT #{limit}")
    List<Game2048RecordVO> getRankingListByHighestScore(@Param("limit") Integer limit);

    /**
     * 获取排行榜
     */
    List<Game2048RecordVO> getRankingList(@Param("limit") Integer limit);

    /**
     * 保存游戏记录
     */
    int saveGameRecord(Game2048Record record);

    /**
     * 获取用户游戏历史记录
     * @param userId 用户ID
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 游戏记录列表
     */
    @Select("SELECT * FROM game_2048_record " +
            "WHERE userId = #{userId} AND isDelete = 0 " +
            "ORDER BY createTime DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<Game2048Record> getUserGameHistory(@Param("userId") Long userId,
                                            @Param("offset") Integer offset,
                                            @Param("pageSize") Integer pageSize);

    /**
     * 获取用户游戏历史记录总数
     * @param userId 用户ID
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM game_2048_record " +
            "WHERE userId = #{userId} AND isDelete = 0")
    Long getUserGameHistoryTotal(@Param("userId") Long userId);
}
