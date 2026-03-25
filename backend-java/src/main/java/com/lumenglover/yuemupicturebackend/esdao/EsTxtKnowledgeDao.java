package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsTxtKnowledge;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * TXT知识库ES操作接口
 */
public interface EsTxtKnowledgeDao extends ElasticsearchRepository<EsTxtKnowledge, String> {
}
