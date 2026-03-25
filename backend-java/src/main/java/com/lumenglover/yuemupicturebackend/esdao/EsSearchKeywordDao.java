package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsSearchKeyword;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;

@Repository
public interface EsSearchKeywordDao extends ElasticsearchRepository<EsSearchKeyword, String> {

    /**
     * 根据类型和关键词查询
     */
    EsSearchKeyword findByTypeAndKeyword(String type, String keyword);

    /**
     * 根据类型查询热门关键词
     */
    List<EsSearchKeyword> findByTypeAndUpdateTimeAfterOrderByCountDesc(String type, Date startTime);
}
