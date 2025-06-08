package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lumenglover.yuemupicturebackend.model.dto.picture.*;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

}
