package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.dto.picture.PictureHotScoreDto;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author 鹿梦
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2024-12-11 20:45:51
 * @Entity com.lumenglover.yuemupicturebackend.model.entity.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    List<Picture> getTop100PictureByYear();

    List<Picture> getTop100PictureByMonth();

    List<Picture> getTop100PictureByWeek();

    /**
     * 获取用户的所有草稿图片
     * @param userId 用户ID
     * @return 草稿图片列表
     */
    List<Picture> getAllDrafts(Long userId);

    /**
     * 获取用户的最新草稿图片
     * @param userId 用户ID
     * @return 最新草稿图片
     */
    Picture getLatestDraft(Long userId);

    /**
     * 分页查询用于计算热榜分数的图片数据
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片热榜分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreData(long offset, long pageSize);

    /**
     * 分页查询用于计算分数的图片数据（带过滤条件：公共空间、审核通过、非草稿）
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片热榜分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreDataWithFilters(long offset, long pageSize);

    /**
     * 统计可用于计算热榜分数的图片总数
     * @return 图片总数
     */
    long countPictureScoreData();

    /**
     * 统计可用于计算热榜分数的公共图片总数（公共空间、审核通过、非草稿）
     * @return 图片总数
     */
    long countPictureScoreDataWithFilters();

    /**
     * 统计可用于计算分数的图片总数（所有空间、审核通过、非草稿）
     * @return 图片总数
     */
    long countPictureScoreDataWithoutSpaceFilter();

    /**
     * 查询最大的图片ID
     * @return 最大图片ID
     */
    Long selectMaxPictureId();

    /**
     * 按ID范围分页查询用于计算热榜分数的图片数据
     * @param minId 最小ID
     * @param maxId 最大ID
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片热榜分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreDataInRange(long minId, long maxId, long offset, long pageSize);

    /**
     * 按ID范围分页查询用于计算分数的图片数据（带过滤条件：公共空间、审核通过、非草稿）
     * @param minId 最小ID
     * @param maxId 最大ID
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片热榜分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreDataInRangeWithFilters(long minId, long maxId, long offset, long pageSize);

    /**
     * 查询已删除的图片（绕过逻辑删除）
     * @param updateTime 更新时间
     * @return 已删除的图片列表
     */
    List<Picture> selectDeletedPicturesByUpdateTime(Date updateTime);

    /**
     * 查询所有已删除的图片（绕过逻辑删除）
     * @return 已删除的图片列表
     */
    List<Picture> selectAllDeletedPictures();

    /**
     * 游标分页查询公共图片
     * @param cursorCreateTime 游标时间
     * @param cursorId 游标ID
     * @param pageSize 页大小
     * @return 图片列表
     */
    List<Picture> cursorPagePublicPictures(@Param("cursorCreateTime") Date cursorCreateTime,
                                           @Param("cursorId") Long cursorId,
                                           @Param("pageSize") int pageSize);

    /**
     * 游标分页查询空间图片
     * @param spaceId 空间ID
     * @param cursorCreateTime 游标时间
     * @param cursorId 游标ID
     * @param pageSize 页大小
     * @return 图片列表
     */
    List<Picture> cursorPageSpacePictures(@Param("spaceId") Long spaceId,
                                          @Param("cursorCreateTime") Date cursorCreateTime,
                                          @Param("cursorId") Long cursorId,
                                          @Param("pageSize") int pageSize);

    /**
     * 游标分页查询用户图片
     * @param userId 用户ID
     * @param cursorCreateTime 游标时间
     * @param cursorId 游标ID
     * @param pageSize 页大小
     * @return 图片列表
     */
    List<Picture> cursorPageUserPictures(@Param("userId") Long userId,
                                         @Param("cursorCreateTime") Date cursorCreateTime,
                                         @Param("cursorId") Long cursorId,
                                         @Param("pageSize") int pageSize);

    /**
     * 搜索图片（优化版）
     * @param spaceId 空间ID
     * @param userId 用户ID
     * @param category 分类
     * @param tags 标签
     * @param searchText 搜索文本
     * @param offset 偏移量
     * @param pageSize 页大小
     * @return 图片列表
     */
    List<Picture> searchPicturesOptimized(@Param("spaceId") Long spaceId,
                                          @Param("userId") Long userId,
                                          @Param("category") String category,
                                          @Param("tags") String tags,
                                          @Param("searchText") String searchText,
                                          @Param("offset") long offset,
                                          @Param("pageSize") int pageSize);
}
