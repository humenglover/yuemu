package com.lumenglover.yuemupicturebackend.esdao;

import com.lumenglover.yuemupicturebackend.model.entity.es.EsUser;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsUserDao extends ElasticsearchRepository<EsUser, Long> {

    /**
     * 根据用户账号查询
     */
    Optional<EsUser> findByUserAccount(String userAccount);

    /**
     * 根据用户名模糊查询
     */
    List<EsUser> findByUserNameContaining(String userName);

    /**
     * 根据用户简介模糊查询
     */
    List<EsUser> findByUserProfileContaining(String userProfile);

    /**
     * 根据用户角色查询
     */
    List<EsUser> findByUserRole(String userRole);

    /**
     * 根据用户名或简介模糊查询
     */
    List<EsUser> findByUserNameContainingOrUserProfileContaining(String userName, String userProfile);

    /**
     * 根据用户账号模糊查询
     */
    List<EsUser> findByUserAccountContaining(String userAccount);
}
