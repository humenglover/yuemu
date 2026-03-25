package com.lumenglover.yuemupicturebackend.controller;


import com.lumenglover.yuemupicturebackend.annotation.RateLimiter;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @RateLimiter(key = "main_health", time = 60, count = 30, message = "健康检查查询过于频繁，请稍后再试")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
