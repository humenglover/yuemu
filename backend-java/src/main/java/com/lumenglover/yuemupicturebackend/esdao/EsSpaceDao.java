package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsSpace;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSpaceDao extends ElasticsearchRepository<EsSpace, Long> {
}
