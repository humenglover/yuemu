package com.lumenglover.yuemupicturebackend.manager;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信登录管理器
 */
@Component
public class WxLoginManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 验证码有效期：5 分钟
    private static final long CODE_EXPIRATION_TIME = 5 * 60;

    private static final String CODE_KEY_PREFIX = "wx:login:code:";
    private static final String OPENID_KEY_PREFIX = "wx:login:openid:";

    private static final String REQ_CODE_KEY_PREFIX = "wx:login:req:code:";
    private static final String REQ_SCENE_KEY_PREFIX = "wx:login:req:scene:";
    private static final String REQ_TYPE_KEY_PREFIX = "wx:login:req:type:";

    /**
     * 前端主动请求：生成登录验证码及场景ID
     * @param type 场景类别 (如 LOGIN, BIND, UNBIND)
     * @return 包含 sceneId 和 code 的 map
     */
    public Map<String, String> generateFrontendReqCode(String type) {
        // 生成 6 位随机数字验证码
        String code = RandomUtil.randomNumbers(6);
        // 生成 UUID 作为本次请求的场景ID，隔离不同页面的请求
        String sceneId = IdUtil.fastSimpleUUID();

        // 存入 Redis，有效期 5 分钟
        // 验证码映射到场景ID，用于微信端发送验证码后查找对应场景
        stringRedisTemplate.opsForValue().set(REQ_CODE_KEY_PREFIX + code, sceneId, CODE_EXPIRATION_TIME, TimeUnit.SECONDS);
        // 场景ID映射到状态（初始状态为 WAITING）
        stringRedisTemplate.opsForValue().set(REQ_SCENE_KEY_PREFIX + sceneId, "WAITING", CODE_EXPIRATION_TIME, TimeUnit.SECONDS);
        // 场景ID映射到操作类别
        if (type != null) {
            stringRedisTemplate.opsForValue().set(REQ_TYPE_KEY_PREFIX + sceneId, type, CODE_EXPIRATION_TIME, TimeUnit.SECONDS);
        }

        Map<String, String> result = new HashMap<>();
        result.put("sceneId", sceneId);
        result.put("code", code);
        return result;
    }

    /**
     * 根据前端验证码获取对应的场景ID
     * @param code 6位验证码
     * @return 场景ID
     */
    public String getSceneIdByReqCode(String code) {
        return stringRedisTemplate.opsForValue().get(REQ_CODE_KEY_PREFIX + code);
    }

    /**
     * 根据场景ID获取对应的场景类型
     * @param sceneId 场景ID
     * @return 场景类型
     */
    public String getSceneType(String sceneId) {
        return stringRedisTemplate.opsForValue().get(REQ_TYPE_KEY_PREFIX + sceneId);
    }

    /**
     * 更新场景ID的状态（例如存入 openId 表示扫码验证成功）
     * @param sceneId 场景ID
     * @param status 要更新的状态或 openId
     */
    public void updateSceneStatus(String sceneId, String status) {
        Long expire = stringRedisTemplate.getExpire(REQ_SCENE_KEY_PREFIX + sceneId, TimeUnit.SECONDS);
        if (expire != null && expire > 0) {
            stringRedisTemplate.opsForValue().set(REQ_SCENE_KEY_PREFIX + sceneId, status, expire, TimeUnit.SECONDS);
        }
    }

    /**
     * 根据场景ID获取当前状态
     * @param sceneId 场景ID
     * @return 状态字串
     */
    public String getSceneStatus(String sceneId) {
        return stringRedisTemplate.opsForValue().get(REQ_SCENE_KEY_PREFIX + sceneId);
    }

    /**
     * 为用户生成登录验证码 (用于旧版逻辑或绑定流程)
     * @param openId 微信用户标识
     * @return 6 位验证码
     */
    public String generateLoginCode(String openId) {
        // 检查用户是否已有活跃的验证码，避免短时间内重复生成
        String existingCode = stringRedisTemplate.opsForValue().get(OPENID_KEY_PREFIX + openId);
        if (existingCode != null) {
            return existingCode;
        }

        // 生成 6 位随机数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 存入 Redis，双向绑定以便查询
        stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + code, openId, CODE_EXPIRATION_TIME, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(OPENID_KEY_PREFIX + openId, code, CODE_EXPIRATION_TIME, TimeUnit.SECONDS);

        return code;
    }

    /**
     * 根据验证码获取 openId
     * @param code 验证码
     * @return openId
     */
    public String getOpenIdByCode(String code) {
        return stringRedisTemplate.opsForValue().get(CODE_KEY_PREFIX + code);
    }

    /**
     * 验证码使用后删除相关键
     * @param code 验证码
     * @param sceneId 场景ID
     */
    public void removeReqCode(String code, String sceneId) {
        if (code != null) {
            stringRedisTemplate.delete(REQ_CODE_KEY_PREFIX + code);
        }
        if (sceneId != null) {
            stringRedisTemplate.delete(REQ_SCENE_KEY_PREFIX + sceneId);
            stringRedisTemplate.delete(REQ_TYPE_KEY_PREFIX + sceneId);
        }
    }

    /**
     * 验证码使用后删除相关键 (用于旧逻辑)
     * @param code 验证码
     */
    public void removeCode(String code) {
        String openId = getOpenIdByCode(code);
        if (openId != null) {
            stringRedisTemplate.delete(CODE_KEY_PREFIX + code);
            stringRedisTemplate.delete(OPENID_KEY_PREFIX + openId);
        }
    }
}
