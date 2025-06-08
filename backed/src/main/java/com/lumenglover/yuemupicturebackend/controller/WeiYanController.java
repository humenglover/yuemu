package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.WeiYan;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.WeiYanService;
import com.lumenglover.yuemupicturebackend.service.LoveBoardService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 微言接口
 */
@RestController
@RequestMapping("/weiyan")
public class WeiYanController {

    @Resource
    private WeiYanService weiYanService;

    @Resource
    private UserService userService;

    @Resource
    private LoveBoardService loveBoardService;

    // region 增删改查

    /**
     * 创建微言
     *
     * @param weiYan
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addWeiYan(@RequestBody WeiYan weiYan, HttpServletRequest request) {
        if (weiYan == null || weiYan.getLoveBoardId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 验证用户是否是恋爱板的所有者
        if (!loveBoardService.isLoveBoardOwner(weiYan.getLoveBoardId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        long weiYanId = weiYanService.addWeiYan(weiYan, weiYan.getLoveBoardId(), loginUser.getId());
        return ResultUtils.success(weiYanId);
    }

    /**
     * 删除微言
     *
     * @param id
     * @param loveBoardId
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteWeiYan(@RequestParam("id") long id, @RequestParam("loveBoardId") long loveBoardId, HttpServletRequest request) {
        if (id <= 0 || loveBoardId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean b = weiYanService.deleteWeiYan(id, loveBoardId, loginUser.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新微言
     *
     * @param weiYan
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateWeiYan(@RequestBody WeiYan weiYan, HttpServletRequest request) {
        if (weiYan == null || weiYan.getId() <= 0 || weiYan.getLoveBoardId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean b = weiYanService.updateWeiYan(weiYan, weiYan.getLoveBoardId(), loginUser.getId());
        return ResultUtils.success(b);
    }

    /**
     * 获取微言列表
     *
     * @param loveBoardId 恋爱板ID
     * @param userId 用户ID（可选）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 分页后的微言列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<WeiYan>> listWeiYan(
            @RequestParam("loveBoardId") Long loveBoardId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        // 参数校验
        if (current <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }

        // 如果提供了恋爱板ID，检查是否有效
        boolean canViewAll = false;
        if (loveBoardId != null && loveBoardId > 0) {
            // 如果提供了用户ID，检查是否是恋爱板的主人
            if (userId != null && userId > 0) {
                canViewAll = loveBoardService.isLoveBoardOwner(loveBoardId, userId);
            }
        }

        // 获取微言列表
        Page<WeiYan> weiYanPage = weiYanService.listWeiYan(loveBoardId, canViewAll ? userId : null, current, pageSize);
        return ResultUtils.success(weiYanPage);
    }

    /**
     * 点赞微言
     *
     * @param id 微言id
     * @return 点赞结果
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> likeWeiYan(@RequestParam("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = weiYanService.likeWeiYan(id);
        return ResultUtils.success(result);
    }

    // endregion
}
