package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.KnowledgeFile;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.dto.knowledgefile.KnowledgeFileQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.KnowledgeFileVO;
import com.lumenglover.yuemupicturebackend.service.KnowledgeFileService;
import com.lumenglover.yuemupicturebackend.annotation.AuthCheck;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import com.lumenglover.yuemupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * 知识库文件控制器
 */
@RestController
@RequestMapping("/knowledgeFile")
public class KnowledgeFileController {

    @Autowired
    private KnowledgeFileService knowledgeFileService;

    @Autowired
    private UserService userService;


    /**
     * 上传知识库文件
     *
     * @param file 上传的文件
     * @return 文件信息
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<KnowledgeFile> uploadKnowledgeFile(@RequestParam("file") MultipartFile file,
                                                           HttpServletRequest httpServletRequest) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId = loginUser.getId();
        KnowledgeFile knowledgeFile = knowledgeFileService.uploadKnowledgeFile(file, userId);
        return ResultUtils.success(knowledgeFile);
    }

    /**
     * 批量删除知识库文件
     *
     * @param ids 文件ID列表
     * @return 删除结果
     */
    @PostMapping("/batch-delete")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> batchDeleteKnowledgeFiles(@RequestBody List<Long> ids, HttpServletRequest httpServletRequest) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件ID列表不能为空");
        }

        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean success = knowledgeFileService.batchDeleteKnowledgeFiles(ids);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量删除知识库文件失败");
        }
        return ResultUtils.success(success);
    }

    /**
     * 同步知识库文件数据
     * 对比Java和Python两侧的文件MD5，保持数据一致性
     *
     * @return 同步结果
     */
    @PostMapping("/sync")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> syncKnowledgeFiles(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean success = knowledgeFileService.syncKnowledgeFiles();
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步知识库文件失败");
        }
        return ResultUtils.success(success);
    }

    /**
     * 分页获取知识库文件列表
     *
     * @param knowledgeFileQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Page<KnowledgeFileVO>> listKnowledgeFileVOByPage(
            @RequestBody KnowledgeFileQueryRequest knowledgeFileQueryRequest,
            HttpServletRequest httpServletRequest) {

        // 参数校验
        if (knowledgeFileQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 限制每页大小
        long size = knowledgeFileQueryRequest.getPageSize();
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页最多显示50条");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 设置用户ID查询条件（普通用户只能查看自己的文件）
        if (!userService.isAdmin(loginUser)) {
            knowledgeFileQueryRequest.setUserId(loginUser.getId());
        }

        // 执行查询
        Page<KnowledgeFileVO> knowledgeFileVOPage = knowledgeFileService.listKnowledgeFileVOByPage(knowledgeFileQueryRequest);
        return ResultUtils.success(knowledgeFileVOPage);
    }

    /**
     * 根据ID获取知识库文件详情
     *
     * @param id 文件ID
     * @return 文件详情
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<KnowledgeFileVO> getKnowledgeFileVOById(Long id, HttpServletRequest httpServletRequest) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件ID不能为空");
        }

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 查询文件
        KnowledgeFile knowledgeFile = knowledgeFileService.getById(id);
        if (knowledgeFile == null || knowledgeFile.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }

        // 权限检查（普通用户只能查看自己的文件）
        if (!userService.isAdmin(loginUser) && !knowledgeFile.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该文件");
        }

        // 转换为VO
        KnowledgeFileVO knowledgeFileVO = KnowledgeFileVO.objToVo(knowledgeFile);
        return ResultUtils.success(knowledgeFileVO);
    }
}
