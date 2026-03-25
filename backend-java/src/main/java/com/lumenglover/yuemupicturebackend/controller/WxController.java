package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.lumenglover.yuemupicturebackend.manager.WxLoginManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/wx")
@Slf4j
public class WxController {

    @Value("${wx.mp.token}")
    private String token;

    @Resource
    private WxLoginManager wxLoginManager;

    /**
     * 微信服务器验证接口
     *
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return 验证通过后原样返回 echostr
     */
    @GetMapping("/check")
    public String check(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {

        log.info("微信服务器验证请求: signature={}, timestamp={}, nonce={}, echostr={}", signature, timestamp, nonce, echostr);

        // 1. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);

        // 2. 将三个参数字符串拼接成一个字符串进行sha1加密
        StringBuilder content = new StringBuilder();
        for (String str : arr) {
            content.append(str);
        }

        // 使用 Hutool 的 DigestUtil 进行 sha1 加密
        String sha1Hex = DigestUtil.sha1Hex(content.toString());

        // 3. 开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
        if (sha1Hex.equals(signature)) {
            log.info("微信验证成功！");
            return echostr;
        } else {
            log.error("微信验证失败: 预期签名={}, 实际签名={}", sha1Hex, signature);
            return null;
        }
    }

    @PostMapping(value = "/check", produces = "application/xml;charset=UTF-8")
    @com.lumenglover.yuemupicturebackend.annotation.RateLimiter(key = "wx_msg", time = 60, count = 10, message = "消息处理过于频繁")
    public String handleMessage(HttpServletRequest request) {
        try {
            // 1. 获取请求体字符串
            String xmlStr = IoUtil.read(request.getReader());
            log.info("收到微信原始消息: \n{}", xmlStr);

            // 2. 解析 XML
            Document document = XmlUtil.parseXml(xmlStr);
            String fromUserName = XmlUtil.elementText(document.getDocumentElement(), "FromUserName");
            String toUserName = XmlUtil.elementText(document.getDocumentElement(), "ToUserName");
            String msgType = XmlUtil.elementText(document.getDocumentElement(), "MsgType");
            String content = XmlUtil.elementText(document.getDocumentElement(), "Content");
            if (content != null) {
                content = content.trim();
            }

            // 内容清洗
            if (content != null) {
                content = content.trim();
            }

            log.info("解析微信消息结果: from={}, type={}, content={}", fromUserName, msgType, content);

            // 3. 处理逻辑
            if ("text".equals(msgType) && content != null) {
                // 判断是否为前端主动生成的6位请求验证码
                if (content.matches("^\\d{6}$")) {
                    log.info("收到6位纯数字验证码 [{}], 准备核对更新场景状态...", content);
                    String sceneId = wxLoginManager.getSceneIdByReqCode(content);
                    if (sceneId != null) {
                        String type = wxLoginManager.getSceneType(sceneId);

                        // 将场景状态更新为用户的 openId
                        wxLoginManager.updateSceneStatus(sceneId, fromUserName);

                        String replyContent;
                        if ("BIND".equals(type)) {
                            replyContent = "微信绑定验证成功！网页即将自动完成绑定。";
                        } else if ("UNBIND".equals(type)) {
                            replyContent = "微信解绑验证成功！网页即将自动查验解绑凭证。";
                        } else {
                            replyContent = "登录验证成功！网页即将自动跳转，请返回网页端操作。\n\n" +
                                    "温馨提示：若是首创账号，默认密码为12345678。";
                        }

                        String replyXml = buildTextReplyXml(toUserName, fromUserName, replyContent);
                        log.info("验证成功并回复 XML: \n{}", replyXml);
                        // 清除 reqCode 防止复用
                        wxLoginManager.removeReqCode(content, null);
                        return replyXml;
                    }
                }

                // 宽松匹配：包含“登录”、“验证码”或“绑定”任一关键词即可
                if (content.contains("登录") || content.contains("验证码") || content.contains("绑定")) {
                    log.info("关键词匹配成功 [{}], 准备生成验证码...", content);
                    String code = wxLoginManager.generateLoginCode(fromUserName);
                    String replyContent = "您的验证码为：" + code + " (5分钟内有效)\n\n" +
                            "温馨提示：若您是首次通过微信快捷入驻，初始密码默认为 12345678。为了账户安全，请登录后尽快前往设置页面修改。";
                    String replyXml = buildTextReplyXml(toUserName, fromUserName, replyContent);
                    log.info("成功生成回复 XML: \n{}", replyXml);
                    return replyXml;
                }
            }
            log.info("消息类型或内容未匹配回复逻辑, msgType={}, content={}", msgType, content);
        } catch (Exception e) {
            log.error("处理微信消息失败", e);
        }
        return "success";
    }

    /**
     * 构建文本消息回复 XML
     */
    private String buildTextReplyXml(String from, String to, String content) {
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%d</CreateTime>" +
                        "<MsgType><![CDATA[text]]></MsgType>" +
                        "<Content><![CDATA[%s]]></Content>" +
                        "</xml>",
                to, from, System.currentTimeMillis() / 1000, content
        );
    }
}
