package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsRagMemory;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 超长记忆 ES 操作接口
 */
public interface EsRagMemoryDao extends ElasticsearchRepository<EsRagMemory, String> {
}
