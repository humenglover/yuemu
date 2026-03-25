package com.lumenglover.yuemupicturebackend.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.lumenglover.yuemupicturebackend.manager.auth.StpKit;
import com.lumenglover.yuemupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 多设备登录控制切面
 * 用于在登录后根据用户设置控制多设备登录行为
 */
@Aspect
@Component
@Slf4j
public class MultiDeviceLoginAspect {

    @Resource
    private UserService userService;

    /**
     * 环绕用户登录方法，控制多设备登录行为
     */
    @Around("execution(* com.lumenglover.yuemupicturebackend.service.impl.UserServiceImpl.userLogin(..))")
    public Object handleMultiDeviceLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原始登录逻辑
        Object result = joinPoint.proceed();

        try {
            // 获取当前登录的用户ID
            if (StpKit.SPACE.isLogin()) {
                Long userId = StpKit.SPACE.getLoginIdAsLong();
                String currentToken = StpKit.SPACE.getTokenValue();

                // 查询用户的多设备登录设置
                Integer allowMultiDeviceLogin = userService.getUserMultiDeviceLogin(userId);

                // 如果用户不允许多设备登录，踢掉之前的登录（保留当前登录，移除其他登录）
                if (allowMultiDeviceLogin != null && allowMultiDeviceLogin == 0) {
                    // 获取该用户的所有登录token列表
                    java.util.List<String> allTokenList = StpKit.SPACE.getTokenValueListByLoginId(userId);

                    // 如果有多个token，只保留当前这个，移除其他的
                    if (allTokenList.size() > 1 && currentToken != null) {
                        for (String token : allTokenList) {
                            if (!token.equals(currentToken)) { // 不移除当前登录的token
                                StpKit.SPACE.logoutByTokenValue(token);
                                log.info("用户 {} 不允许多设备登录，已踢出其他登录设备，Token: {}", userId, token);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理多设备登录控制时发生异常", e);
            // 不影响正常登录流程
        }

        return result;
    }
}
