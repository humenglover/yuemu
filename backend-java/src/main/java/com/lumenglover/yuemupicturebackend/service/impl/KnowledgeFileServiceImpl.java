package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.config.CosClientConfig;
import com.lumenglover.yuemupicturebackend.config.PythonRagConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.manager.upload.KnowledgeFileUpload;
import com.lumenglover.yuemupicturebackend.mapper.KnowledgeFileMapper;
import com.lumenglover.yuemupicturebackend.model.entity.KnowledgeFile;
import com.lumenglover.yuemupicturebackend.service.KnowledgeFileService;
import com.lumenglover.yuemupicturebackend.service.RagService;
import com.lumenglover.yuemupicturebackend.model.dto.knowledgefile.KnowledgeFileQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.KnowledgeFileVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeFileServiceImpl extends ServiceImpl<KnowledgeFileMapper, KnowledgeFile> implements KnowledgeFileService {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private KnowledgeFileUpload knowledgeFileUpload;

    @Resource
    private RagService ragService;

    @Resource
    private PythonRagConfig pythonRagConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>();

    // Python知识库文件信息DTO
    private static class PythonKnowledgeFile {
        public String filename;
        public String filepath;
        public Long size;
        public String extension;
        public String md5;
        public String created_at;
        public String updated_at;

        // getter/setter省略，使用公共字段
    }

    // Python文件列表响应DTO
    private static class PythonFileListResponse {
        public Integer code;
        public boolean success;
        public String msg;
        public PythonFileListData data;
    }

    private static class PythonFileListData {
        public List<PythonKnowledgeFile> files;
        public Integer total;
    }

    static {
        ALLOWED_FILE_TYPES.add(".pdf");
        ALLOWED_FILE_TYPES.add(".txt");
        ALLOWED_FILE_TYPES.add(".docx");
        ALLOWED_FILE_TYPES.add(".md");
    }

    @Override
    @Transactional
    public KnowledgeFile uploadKnowledgeFile(MultipartFile file, Long userId) {
        validateKnowledgeFile(file);

        // 检查知识库文件数量限制
        checkKnowledgeFileLimit();

        String md5Hash;
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            md5Hash = DigestUtil.md5Hex(fileBytes);
        } catch (IOException e) {
            log.error("计算文件MD5失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败");
        }

        QueryWrapper<KnowledgeFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5Hash", md5Hash).eq("isDelete", 0);
        KnowledgeFile existingFile = this.getOne(queryWrapper);
        if (existingFile != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件已存在");
        }

        String originalFileName = file.getOriginalFilename();
        File tempFile = null;
        try {
            String nameWithoutExt = FileUtil.mainName(originalFileName);
            String ext = FileUtil.extName(originalFileName);
            String suffix = "_" + RandomUtil.randomString(8);
            String tempFileName = nameWithoutExt + suffix + (ext.isEmpty() ? "" : "." + ext);
            String tempDir = System.getProperty("java.io.tmpdir");
            tempFile = new File(tempDir, tempFileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }
        } catch (IOException e) {
            log.error("创建临时文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "临时文件创建失败");
        }

        String tempFileName = tempFile.getName();
        String fileExtension = FileUtil.extName(originalFileName);
        String storedFileName = tempFileName;

        String uploadPath = String.format("/knowledge/%s", storedFileName);
        try {
            knowledgeFileUpload.uploadKnowledgeFile(file, uploadPath);
        } catch (Exception e) {
            log.error("上传文件到对象存储失败", e);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        String fileUrl = cosClientConfig.getHost() + uploadPath;

        KnowledgeFile knowledgeFile = new KnowledgeFile();
        knowledgeFile.setOriginalName(tempFileName);
        knowledgeFile.setStoredName(tempFileName);
        knowledgeFile.setFileUrl(fileUrl);
        knowledgeFile.setFileSize(file.getSize());
        knowledgeFile.setFileType(fileExtension);
        knowledgeFile.setUploadTime(new Date());
        knowledgeFile.setUserId(userId);
        knowledgeFile.setMd5Hash(md5Hash);
        knowledgeFile.setVectorCount(0);

        this.save(knowledgeFile);

        uploadToPythonKnowledgeBaseWithBytes(knowledgeFile.getId(), tempFileName, fileBytes, tempFile);

        return knowledgeFile;
    }

    @Override
    @Transactional
    public boolean batchDeleteKnowledgeFiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        List<KnowledgeFile> knowledgeFiles = this.list(new QueryWrapper<KnowledgeFile>()
                .in("id", ids)
                .eq("isDelete", 0));

        if (knowledgeFiles.isEmpty()) {
            return true;
        }

        List<String> md5List = knowledgeFiles.stream()
                .map(KnowledgeFile::getMd5Hash)
                .distinct()
                .collect(Collectors.toList());

        boolean pythonDeleteSuccess = sendPythonPostRequest(pythonRagConfig.getFullKnowledgeDeleteUrl(), md5List);

        if (pythonDeleteSuccess) {
            List<Long> fileIds = knowledgeFiles.stream().map(KnowledgeFile::getId).collect(Collectors.toList());
            this.removeByIds(fileIds);
            log.info("批量删除知识库文件成功，删除文件数量: {}", fileIds.size());
            return true;
        } else {
            log.error("批量删除知识库文件失败，Python端删除失败");
            return false;
        }
    }

    private void uploadToPythonKnowledgeBaseWithBytes(Long fileId, String tempFileName, byte[] fileBytes, File tempFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            FileSystemResource resource = new FileSystemResource(tempFile);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("original_filename", tempFileName);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonRagConfig.getFullKnowledgeUploadUrl(),
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().value() == 200) {
                log.info("文件上传到Python知识库成功，文件ID: {}，文件名: {}", fileId, tempFileName);
            } else {
                log.error("文件上传到Python知识库失败，HTTP状态码: {}，文件ID: {}", response.getStatusCode().value(), fileId);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传到Python知识库失败");
            }
        } catch (Exception e) {
            log.error("上传文件到Python知识库异常，文件ID: {}，文件名: {}", fileId, tempFileName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传到Python知识库失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private boolean sendPythonPostRequest(String url, List<String> md5List) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("md5_hashes", md5List);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            return response.getStatusCode().value() == 200;
        } catch (Exception e) {
            log.error("调用Python接口异常，URL: {}，MD5数量: {}", url, md5List.size(), e);
            return false;
        }
    }

    @Override
    public Page<KnowledgeFileVO> listKnowledgeFileVOByPage(KnowledgeFileQueryRequest knowledgeFileQueryRequest) {
        long current = knowledgeFileQueryRequest.getCurrent();
        long size = knowledgeFileQueryRequest.getPageSize();

        // 限制每页大小
        size = Math.min(size, 50);

        // 构建查询条件
        QueryWrapper<KnowledgeFile> queryWrapper = this.getQueryWrapper(knowledgeFileQueryRequest);

        // 执行分页查询
        Page<KnowledgeFile> knowledgeFilePage = this.page(new Page<>(current, size), queryWrapper);

        // 转换为VO
        Page<KnowledgeFileVO> knowledgeFileVOPage = new Page<>(knowledgeFilePage.getCurrent(), knowledgeFilePage.getSize(), knowledgeFilePage.getTotal());
        List<KnowledgeFileVO> knowledgeFileVOList = knowledgeFilePage.getRecords().stream()
                .map(KnowledgeFileVO::objToVo)
                .collect(Collectors.toList());
        knowledgeFileVOPage.setRecords(knowledgeFileVOList);

        return knowledgeFileVOPage;
    }

    @Override
    @Transactional
    public boolean syncKnowledgeFiles() {
        try {
            log.info("开始同步知识库文件数据");

            // 1. 获取Java侧文件列表
            List<KnowledgeFile> javaFiles = this.list(new QueryWrapper<KnowledgeFile>().eq("isDelete", 0));
            Set<String> javaMd5Set = javaFiles.stream()
                    .map(KnowledgeFile::getMd5Hash)
                    .collect(Collectors.toSet());

            log.info("Java侧文件数量: {}, MD5集合大小: {}", javaFiles.size(), javaMd5Set.size());

            // 2. 获取Python侧文件列表
            List<PythonKnowledgeFile> pythonFiles = getPythonKnowledgeFiles();
            Set<String> pythonMd5Set = pythonFiles.stream()
                    .map(file -> file.md5)
                    .collect(Collectors.toSet());

            log.info("Python侧文件数量: {}, MD5集合大小: {}", pythonFiles.size(), pythonMd5Set.size());

            // 3. 计算需要上传到Python的文件（Java有但Python没有）
            Set<String> toUploadMd5s = new HashSet<>(javaMd5Set);
            toUploadMd5s.removeAll(pythonMd5Set);

            // 4. 计算需要从Python删除的文件（Python有但Java没有）
            Set<String> toDeleteMd5s = new HashSet<>(pythonMd5Set);
            toDeleteMd5s.removeAll(javaMd5Set);

            log.info("需要上传到Python的文件数量: {}", toUploadMd5s.size());
            log.info("需要从Python删除的文件数量: {}", toDeleteMd5s.size());

            // 5. 执行上传操作
            int uploadSuccessCount = 0;
            if (!toUploadMd5s.isEmpty()) {
                uploadSuccessCount = uploadFilesToPython(toUploadMd5s, javaFiles);
            }

            // 6. 执行删除操作
            int deleteSuccessCount = 0;
            if (!toDeleteMd5s.isEmpty()) {
                deleteSuccessCount = deleteFilesFromPython(new ArrayList<>(toDeleteMd5s));
            }

            return true;

        } catch (Exception e) {
            log.error("同步知识库文件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步知识库文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取Python侧知识库文件列表
     */
    private List<PythonKnowledgeFile> getPythonKnowledgeFiles() {
        try {
            String url = pythonRagConfig.getFullKnowledgeListUrl();
            log.info("获取Python知识库文件列表，URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().value() == 200) {
                String responseBody = response.getBody();
                log.debug("Python响应内容: {}", responseBody);

                PythonFileListResponse pythonResponse = objectMapper.readValue(responseBody, PythonFileListResponse.class);

                // 支持多种成功判断方式：success字段为true，或者code为200
                boolean isSuccess = (pythonResponse.success && pythonResponse.data != null && pythonResponse.data.files != null) ||
                        (pythonResponse.code != null && pythonResponse.code == 200 && pythonResponse.data != null && pythonResponse.data.files != null);

                if (isSuccess) {
                    log.info("成功获取Python文件列表，文件数量: {}", pythonResponse.data.files.size());
                    return pythonResponse.data.files;
                } else {
                    log.warn("Python返回失败或数据为空: {}", pythonResponse.msg);
                    return new ArrayList<>();
                }
            } else {
                log.error("获取Python文件列表失败，HTTP状态码: {}", response.getStatusCode().value());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("调用Python获取文件列表接口异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 上传文件到Python知识库
     */
    private int uploadFilesToPython(Set<String> md5s, List<KnowledgeFile> javaFiles) {
        int successCount = 0;

        for (String md5 : md5s) {
            try {
                // 查找对应的Java文件
                KnowledgeFile javaFile = javaFiles.stream()
                        .filter(file -> file.getMd5Hash().equals(md5))
                        .findFirst()
                        .orElse(null);

                if (javaFile == null) {
                    log.warn("找不到MD5对应的Java文件: {}", md5);
                    continue;
                }

                // 下载文件内容
                byte[] fileBytes = downloadFileContent(javaFile.getFileUrl());
                if (fileBytes == null || fileBytes.length == 0) {
                    log.warn("下载文件内容失败: {}", javaFile.getFileUrl());
                    continue;
                }

                // 上传到Python
                boolean uploadSuccess = uploadToPythonWithBytes(javaFile, fileBytes);
                if (uploadSuccess) {
                    successCount++;
                    log.info("文件上传到Python成功: {} (MD5: {})", javaFile.getOriginalName(), md5);
                } else {
                    log.error("文件上传到Python失败: {} (MD5: {})", javaFile.getOriginalName(), md5);
                }

            } catch (Exception e) {
                log.error("上传文件到Python异常，MD5: {}", md5, e);
            }
        }

        return successCount;
    }

    /**
     * 从Python删除文件
     */
    private int deleteFilesFromPython(List<String> md5s) {
        if (md5s.isEmpty()) {
            return 0;
        }

        try {
            String url = pythonRagConfig.getFullKnowledgeDeleteUrl();
            log.info("删除Python知识库文件，URL: {}, MD5数量: {}", url, md5s.size());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("md5_hashes", md5s);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            log.debug("Python删除接口响应状态码: {}", response.getStatusCode().value());
            log.debug("Python删除接口响应体: {}", response.getBody());

            if (response.getStatusCode().value() == 200) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    Boolean success = (Boolean) responseBody.get("success");
                    if (success != null && success) {
                        // 获取data对象，然后从中获取success_count
                        Object dataObj = responseBody.get("data");
                        if (dataObj instanceof Map) {
                            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                            Integer successCount = (Integer) dataMap.get("success_count");
                            log.info("Python删除文件成功，成功数量: {}", successCount != null ? successCount : 0);
                            return successCount != null ? successCount : 0;
                        }
                    }
                }
            }

            log.error("Python删除文件失败，HTTP状态码: {}", response.getStatusCode().value());
            return 0;

        } catch (Exception e) {
            log.error("调用Python删除文件接口异常", e);
            return 0;
        }
    }

    /**
     * 下载文件内容
     */
    private byte[] downloadFileContent(String fileUrl) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
            if (response.getStatusCode().value() == 200) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("下载文件失败: {}", fileUrl, e);
        }
        return null;
    }

    /**
     * 使用字节数组上传文件到Python
     */
    private boolean uploadToPythonWithBytes(KnowledgeFile knowledgeFile, byte[] fileBytes) {
        File tempFile = null;
        try {
            // 创建临时文件
            String tempDir = System.getProperty("java.io.tmpdir");
            tempFile = new File(tempDir, knowledgeFile.getOriginalName());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            // 上传到Python
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            FileSystemResource resource = new FileSystemResource(tempFile);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("original_filename", knowledgeFile.getOriginalName());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonRagConfig.getFullKnowledgeUploadUrl(),
                    requestEntity,
                    Map.class
            );

            return response.getStatusCode().value() == 200;

        } catch (Exception e) {
            log.error("上传文件到Python异常，文件ID: {}", knowledgeFile.getId(), e);
            return false;
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<KnowledgeFile> getQueryWrapper(KnowledgeFileQueryRequest knowledgeFileQueryRequest) {
        QueryWrapper<KnowledgeFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);

        if (knowledgeFileQueryRequest == null) {
            return queryWrapper;
        }

        // 精确查询条件
        if (knowledgeFileQueryRequest.getId() != null && knowledgeFileQueryRequest.getId() > 0) {
            queryWrapper.eq("id", knowledgeFileQueryRequest.getId());
        }
        if (knowledgeFileQueryRequest.getUserId() != null && knowledgeFileQueryRequest.getUserId() > 0) {
            queryWrapper.eq("userId", knowledgeFileQueryRequest.getUserId());
        }
        if (knowledgeFileQueryRequest.getMd5Hash() != null) {
            queryWrapper.eq("md5Hash", knowledgeFileQueryRequest.getMd5Hash());
        }

        // 模糊查询条件
        if (knowledgeFileQueryRequest.getOriginalName() != null) {
            queryWrapper.like("originalName", knowledgeFileQueryRequest.getOriginalName());
        }
        if (knowledgeFileQueryRequest.getStoredName() != null) {
            queryWrapper.like("storedName", knowledgeFileQueryRequest.getStoredName());
        }
        if (knowledgeFileQueryRequest.getFileType() != null) {
            queryWrapper.eq("fileType", knowledgeFileQueryRequest.getFileType());
        }

        // 向量数量范围查询
        if (knowledgeFileQueryRequest.getMinVectorCount() != null) {
            queryWrapper.ge("vectorCount", knowledgeFileQueryRequest.getMinVectorCount());
        }
        if (knowledgeFileQueryRequest.getMaxVectorCount() != null) {
            queryWrapper.le("vectorCount", knowledgeFileQueryRequest.getMaxVectorCount());
        }

        // 时间范围查询
        if (knowledgeFileQueryRequest.getStartUploadTime() != null) {
            queryWrapper.ge("uploadTime", knowledgeFileQueryRequest.getStartUploadTime());
        }
        if (knowledgeFileQueryRequest.getEndUploadTime() != null) {
            queryWrapper.le("uploadTime", knowledgeFileQueryRequest.getEndUploadTime());
        }

        // 全文搜索
        if (knowledgeFileQueryRequest.getSearchText() != null) {
            queryWrapper.and(wrapper -> wrapper
                    .like("originalName", knowledgeFileQueryRequest.getSearchText())
                    .or()
                    .like("storedName", knowledgeFileQueryRequest.getSearchText())
            );
        }

        // 按上传时间降序排列
        queryWrapper.orderByDesc("uploadTime");

        return queryWrapper;
    }

    /**
     * 检查知识库文件数量限制
     */
    private void checkKnowledgeFileLimit() {
        // 查询当前未删除的知识库文件数量
        long currentCount = this.count(new QueryWrapper<KnowledgeFile>().eq("isDelete", 0));

        // 获取配置的最大文件数量
        int maxFiles = pythonRagConfig.getKnowledge().getMaxFiles();

        if (currentCount >= maxFiles) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("知识库文件数量已达上限：%d个，当前已有%d个文件", maxFiles, currentCount));
        }

        log.info("知识库文件数量检查通过，当前数量：{}，最大限制：{}", currentCount, maxFiles);
    }

    private void validateKnowledgeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        String originalFileName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String fileExtension = "." + FileUtil.extName(originalFileName).toLowerCase();
        if (!ALLOWED_FILE_TYPES.contains(fileExtension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("不支持的文件类型，仅支持：%s", String.join(", ", ALLOWED_FILE_TYPES)));
        }

        final long MAX_FILE_SIZE = 100 * 1024 * 1024;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过100MB");
        }
    }
}
