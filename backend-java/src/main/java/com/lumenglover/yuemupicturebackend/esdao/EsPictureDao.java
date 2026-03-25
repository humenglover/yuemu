package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsPicture;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsPictureDao extends ElasticsearchRepository<EsPicture, Long> {

    /**
     * 根据分类查询图片
     */
    List<EsPicture> findByCategory(String category);

    /**
     * 根据用户ID查询图片
     */
    List<EsPicture> findByUserId(Long userId);

    /**
     * 根据审核状态查询图片
     */
    List<EsPicture> findByReviewStatus(Integer reviewStatus);

    /**
     * 根据名称或简介模糊查询
     */
    List<EsPicture> findByNameContainingOrIntroductionContaining(String name, String introduction);

    /**
     * 根据标签模糊查询
     */
    List<EsPicture> findByTagsContaining(String tag);

    /**
     * 根据图片格式查询
     */
    List<EsPicture> findByPicFormat(String picFormat);
}
