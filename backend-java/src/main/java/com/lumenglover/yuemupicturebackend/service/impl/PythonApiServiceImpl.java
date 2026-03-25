package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.lumenglover.yuemupicturebackend.config.RagConfig;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.service.PythonApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Service
public class PythonApiServiceImpl implements PythonApiService {

    @Resource
    private RagConfig ragConfig;

    @Override
    public byte[] removeBackground(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传图片");
        }

        RagConfig.PythonService pythonServiceConfig = ragConfig.getPythonService();
        String baseUrl = pythonServiceConfig.getBaseUrl();
        // 此处的 ai.removeBgEndpoint 我们在 application.yml 里配置过
        String removeBgEndpoint = pythonServiceConfig.getAi().getRemoveBgEndpoint();
        Integer timeout = pythonServiceConfig.getTimeout().intValue();

        String url = baseUrl + removeBgEndpoint;
        log.info("向 Python RAG 服务发起去除背景请求，URL: {}", url);

        try {
            // 使用 Hutool 转发文件到 Python 服务
            byte[] fileBytes = file.getBytes();
            HttpResponse response = HttpRequest.post(url)
                    .form("file", fileBytes, file.getOriginalFilename())
                    .timeout(timeout)
                    .execute();

            if (!response.isOk()) {
                log.error("调用 Python 端去除背景失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Python 端服务处理失败，请稍后重试");
            }

            // 获取抠图后的图片二进制返回
            return response.bodyBytes();
        } catch (IOException e) {
            log.error("读取上传图片字节流时发生错误：", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理异常");
        } catch (Exception e) {
            log.error("调用 Python 端去除背景服务异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Python 端服务调用异常，请重试");
        }
    }

    @Override
    public byte[] faceBlur(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传图片");
        }

        RagConfig.PythonService pythonServiceConfig = ragConfig.getPythonService();
        String baseUrl = pythonServiceConfig.getBaseUrl();
        String faceBlurEndpoint = pythonServiceConfig.getAi().getFaceBlurEndpoint();
        Integer timeout = pythonServiceConfig.getTimeout().intValue();

        String url = baseUrl + faceBlurEndpoint;
        log.info("向 Python RAG 服务发起人脸打码请求，URL: {}", url);

        try {
            byte[] fileBytes = file.getBytes();
            HttpResponse response = HttpRequest.post(url)
                    .form("file", fileBytes, file.getOriginalFilename())
                    .timeout(timeout)
                    .execute();

            if (!response.isOk()) {
                log.error("调用 Python 端人脸打码失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Python 端服务处理失败，请稍后重试");
            }

            return response.bodyBytes();
        } catch (IOException e) {
            log.error("读取上传图片字节流时发生错误：", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理异常");
        } catch (Exception e) {
            log.error("调用 Python 端人脸打码服务异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Python 端服务调用异常，请重试");
        }
    }

    @Override
    public byte[] changeBackground(MultipartFile file, String color, MultipartFile backgroundImage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传图片");
        }

        RagConfig.PythonService pythonServiceConfig = ragConfig.getPythonService();
        String baseUrl = pythonServiceConfig.getBaseUrl();
        String endpoint = pythonServiceConfig.getAi().getChangeBackgroundEndpoint();
        Integer timeout = pythonServiceConfig.getTimeout().intValue();

        String url = baseUrl + endpoint;
        log.info("向 Python RAG 服务发起换背景请求，URL: {}", url);

        try {
            HttpRequest request = HttpRequest.post(url)
                    .form("file", file.getBytes(), file.getOriginalFilename())
                    .timeout(timeout);

            if (color != null && !color.isEmpty()) {
                request.form("color", color);
            }

            if (backgroundImage != null && !backgroundImage.isEmpty()) {
                request.form("background_image", backgroundImage.getBytes(), backgroundImage.getOriginalFilename());
            }

            HttpResponse response = request.execute();

            if (!response.isOk()) {
                log.error("调用 Python 端换背景失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Python 端服务处理失败，请稍后重试");
            }

            return response.bodyBytes();
        } catch (IOException e) {
            log.error("读取图片字节流时发生错误：", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理异常");
        } catch (Exception e) {
            log.error("调用 Python 端换背景服务异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Python 端服务调用异常，请重试");
        }
    }
}
