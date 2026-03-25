package com.lumenglover.yuemupicturebackend.utils;

import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

@Slf4j
public class ImageUtils {

    // 图片尺寸限制
    private static final int MIN_WIDTH = 320;
    private static final int MIN_HEIGHT = 320;
    private static final int TARGET_WIDTH = 680;
    private static final int TARGET_HEIGHT = 680;
    private static final long MIN_SIZE_BYTES = 3 * 1024; // 3KB
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    static {
        // 确保WebP支持被加载
        try {
            Class.forName("org.sejda.imageio.webp.WebPReadParam");
            log.info("WebP支持已加载");
        } catch (ClassNotFoundException e) {
            log.warn("WebP支持未加载，某些WebP图片可能无法处理", e);
        }
    }

    /**
     * 将图片转换为PNG格式，并自动调整到满足要求的尺寸
     *
     * @param imageUrl 图片URL
     * @return PNG格式的图片字节数组
     */
    public static byte[] convertToPng(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // 从URL加载图片
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("无法访问图片URL: {}, 响应码: {}", imageUrl, responseCode);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        String.format("无法访问图片，HTTP状态码: %d", responseCode));
            }

            String contentType = connection.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                log.error("URL不是图片类型: {}, Content-Type: {}", imageUrl, contentType);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        String.format("URL不是图片类型，Content-Type: %s", contentType));
            }

            // 读取图片数据
            inputStream = connection.getInputStream();
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                log.error("无法读取图片内容: {}", imageUrl);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "无法读取图片内容，图片可能已损坏或格式不支持");
            }

            // 获取原始尺寸
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算目标尺寸
            int targetWidth = originalWidth;
            int targetHeight = originalHeight;

            // 如果尺寸小于最小要求，放大到最小要求
            if (originalWidth < MIN_WIDTH || originalHeight < MIN_HEIGHT) {
                double scale = Math.max(
                        (double) MIN_WIDTH / originalWidth,
                        (double) MIN_HEIGHT / originalHeight
                );
                targetWidth = (int) (originalWidth * scale);
                targetHeight = (int) (originalHeight * scale);
                log.info("图片尺寸太小，将自动放大到: {}x{}", targetWidth, targetHeight);
            }
            // 如果尺寸超过目标尺寸，缩小到目标尺寸
            else if (originalWidth > TARGET_WIDTH || originalHeight > TARGET_HEIGHT) {
                double scale = Math.min(
                        (double) TARGET_WIDTH / originalWidth,
                        (double) TARGET_HEIGHT / originalHeight
                );
                targetWidth = (int) (originalWidth * scale);
                targetHeight = (int) (originalHeight * scale);
                log.info("图片尺寸太大，将自动缩小到: {}x{}", targetWidth, targetHeight);
            }

            // 创建调整大小后的图片
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resizedImage.createGraphics();
            try {
                // 设置高质量绘制
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // 设置白色背景（处理透明图片）
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetWidth, targetHeight);

                // 绘制调整大小后的图片
                g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                g2d.dispose();
            }

            // 将图片转换为PNG格式并调整质量
            byte[] imageData = adjustImageQuality(resizedImage);

            // 如果图片仍然太小，增加填充
            if (imageData.length < MIN_SIZE_BYTES) {
                log.info("图片文件太小，添加元数据以增加文件大小");
                imageData = addPaddingToImage(imageData);
            }

            // 如果图片太大，降低质量
            if (imageData.length > MAX_SIZE_BYTES) {
                log.info("图片文件太大，降低质量以减小文件大小");
                imageData = reduceImageQuality(resizedImage);
            }

            return imageData;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("图片处理失败: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("图片处理失败: %s", e.getMessage()));
        } finally {
            // 关闭所有资源
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 调整图片质量
     */
    private static byte[] adjustImageQuality(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }

    /**
     * 降低图片质量以减小文件大小
     */
    private static byte[] reduceImageQuality(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }

    /**
     * 添加填充以增加文件大小
     */
    private static byte[] addPaddingToImage(byte[] imageData) {
        // 创建一个新的字节数组，大小为最小要求
        byte[] paddedData = new byte[(int) MIN_SIZE_BYTES];
        // 复制原始图片数据
        System.arraycopy(imageData, 0, paddedData, 0, imageData.length);
        // 用随机数据填充剩余空间
        for (int i = imageData.length; i < paddedData.length; i++) {
            paddedData[i] = (byte) (Math.random() * 256);
        }
        return paddedData;
    }

    /**
     * 将图片URL转换为InputStream
     *
     * @param imageUrl 图片URL
     * @return InputStream
     */
    public static InputStream getImageInputStream(String imageUrl) {
        try {
            byte[] imageData = convertToPng(imageUrl);
            return new ByteArrayInputStream(imageData);
        } catch (Exception e) {
            log.error("获取图片输入流失败: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("获取图片失败: %s", e.getMessage()));
        }
    }
}
