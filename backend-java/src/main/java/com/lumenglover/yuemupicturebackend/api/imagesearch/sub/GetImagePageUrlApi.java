package com.lumenglover.yuemupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step 1）
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // image: https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        //tn: pc
        //from: pc
        //image_source: PC_UPLOAD_URL
        //sdkParams:
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";
        try {
            // 2. 发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", acsToken)
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            // {"status":0,"msg":"Success","data":{"url":"https://graph.baidu.com/sc","sign":"1262fe97cd54acd88139901734784257"}}
            String body = httpResponse.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            // 对 URL 进行解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }
}
