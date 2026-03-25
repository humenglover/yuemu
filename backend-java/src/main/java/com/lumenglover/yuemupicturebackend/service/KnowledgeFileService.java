package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.entity.KnowledgeFile;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.dto.knowledgefile.KnowledgeFileQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.KnowledgeFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 知识库文件服务接口
 */
public interface KnowledgeFileService extends IService<KnowledgeFile> {

    /**
     * 上传知识库文件
     */
    KnowledgeFile uploadKnowledgeFile(MultipartFile file, Long userId) throws IOException;

    /**
     * 批量删除知识库文件
     */
    boolean batchDeleteKnowledgeFiles(List<Long> ids);

    /**
     * 同步知识库文件数据
     * 对比Java和Python两侧的文件MD5，保持数据一致性
     */
    boolean syncKnowledgeFiles();

    /**
     * 分页获取知识库文件列表
     *
     * @param knowledgeFileQueryRequest 查询请求参数
     * @return 分页结果
     */
    Page<KnowledgeFileVO> listKnowledgeFileVOByPage(KnowledgeFileQueryRequest knowledgeFileQueryRequest);
}
