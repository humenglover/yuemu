package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lumenglover.yuemupicturebackend.model.dto.picture.*;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.PictureTagCategory;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 鹿梦
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-11 20:45:51
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                 User loginUser);

    boolean batchOperationPicture(PictureOperation pictureOperation);

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    void crawlerDetect(HttpServletRequest request);

    List<PictureVO> getTop100Picture(Long id);

    Page<PictureVO> getFollowPicture(HttpServletRequest request, PictureQueryRequest pictureQueryRequest);

    PictureVO uploadPostPicture(Object inputSource,
                                PictureUploadRequest pictureUploadRequest,
                                User loginUser);

    long getViewCount(Long pictureId);


    /**
     * 更新图片信息
     * @param picture 图片信息
     * @return 更新结果
     */
    boolean updatePicture(Picture picture);

    /**
     * 获取图片详情(带权限校验)
     * @param id 图片ID
     * @param request HTTP请求
     * @return 图片详情VO
     */
    PictureVO getPictureVOById(long id, HttpServletRequest request);

    /**
     * 分页获取图片列表(带缓存)
     * @param pictureQueryRequest 查询请求
     * @param request HTTP请求
     * @return 分页图片列表
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 获取Top100图片列表(带缓存)
     * @param id 榜单类型ID
     * @return Top100图片列表
     */
    List<PictureVO> getTop100PictureWithCache(Long id);

    /**
     * 分页获取图片列表（封装类）
     * @param pictureQueryRequest 查询请求
     * @param request HTTP请求
     * @return 分页图片列表
     */
    Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 设置图片精选状态
     * @param pictureFeatureRequest 请求参数
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean setPictureFeature(PictureFeatureRequest pictureFeatureRequest, User loginUser);

    /**
     * 分页获取精选图片列表
     * @param pictureQueryRequest 查询参数
     * @param request HTTP请求
     * @return 分页图片列表
     */
    Page<PictureVO> getFeaturePicture(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 获取所有草稿图片
     * @param userId 用户ID
     * @return 草稿图片列表
     */
    List<Picture> getAllDrafts(Long userId);

    /**
     * 获取最近的一条草稿
     * @param userId 用户ID
     * @return 最近的草稿图片
     */
    Picture getLatestDraft(Long userId);

    /**
     * 修改图片草稿状态
     * @param pictureId 图片ID
     * @param isDraft 是否为草稿状态
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean updatePictureDraftStatus(Long pictureId, Integer isDraft, User loginUser);

    /**
     * 分页查询用于计算分数的图片数据
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreData(long offset, long pageSize);

    /**
     * 统计可用于计算分数的图片总数
     * @return 图片总数
     */
    long countPictureScoreData();

    /**
     * 批量更新图片热榜分数
     * @param pictures 包含ID和热榜分数的图片列表
     * @return 更新结果
     */
    boolean updateBatchHotScore(List<Picture> pictures);

    /**
     * 批量更新图片推荐分数
     * @param pictures 包含ID和推荐分数的图片列表
     * @return 更新结果
     */
    boolean updateBatchRecommendScore(List<Picture> pictures);

    /**
     * 查询最大的图片ID
     * @return 最大图片ID
     */
    Long selectMaxPictureId();

    /**
     * 按ID范围分页查询用于计算分数的图片数据
     * @param minId 最小ID
     * @param maxId 最大ID
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 图片分数DTO列表
     */
    List<PictureHotScoreDto> selectPictureScoreDataInRange(long minId, long maxId, long offset, long pageSize);

    /**
     * 添加图片浏览记录
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @param request HTTP请求
     */
    void addPictureViewRecord(long pictureId, long userId, HttpServletRequest request);

    PictureTagCategory listPictureTagCategory(User loginUser);


    /**
     * 按是否为新图片分组
     * @param ids 图片ID列表
     * @param newPictureHours 新图片的时间阈值（小时）
     * @return Map，key为是否为新图片，value为对应的ID列表
     */
    Map<Boolean, List<Long>> groupPictureByNew(List<Long> ids, long newPictureHours);

    /**
     * 根据权限过滤图片查询条件
     * @param queryWrapper 查询条件
     * @param loginUser 当前登录用户
     * @return 过滤后的查询条件
     */
    QueryWrapper<Picture> filterPictureQueryByPermission(QueryWrapper<Picture> queryWrapper, User loginUser);

    /**
     * 设置图片权限
     *
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @param allowCollect 是否允许收藏
     * @param allowLike 是否允许点赞
     * @param allowComment 是否允许评论
     * @param allowShare 是否允许分享
     * @return 操作结果
     */
    boolean setPicturePermission(Long pictureId, Long userId, Integer allowCollect, Integer allowLike, Integer allowComment, Integer allowShare);

    /**
     * 分页获取用户的所有非草稿非删除图片
     *
     * @param userId 用户ID
     * @param current 页码
     * @param size 页面大小
     * @return 分页的用户非草稿非删除图片列表
     */
    Page<Picture> getUserAllPictures(Long userId, long current, long size);

    /**
     * 分页获取用户指定空间外的所有非草稿非删除图片
     *
     * @param userId 用户ID
     * @param excludeSpaceId 要排除的空间ID
     * @param current 页码
     * @param size 页面大小
     * @return 分页的用户在其他空间的非草稿非删除图片列表
     */
    Page<Picture> getUserPicturesFromOtherSpaces(Long userId, Long excludeSpaceId, long current, long size);

    /**
     * 对图片进行 AI 标签识别
     *
     * @param file 图片文件
     * @param pictureId 图片ID
     * @return 识别到的标签列表
     */
    List<String> aiTag(MultipartFile file, Long pictureId);

    /**
     * 从现有图片复制到新空间
     *
     * @param pictureId 原图片ID
     * @param newSpaceId 新空间ID，可以为null表示公共空间
     * @param loginUser 当前登录用户
     * @return 复制后的新图片对象
     */
    Picture copyPictureToNewSpace(Long pictureId, Long newSpaceId, User loginUser);
}
