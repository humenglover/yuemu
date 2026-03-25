package com.lumenglover.yuemupicturebackend.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

@Component
public class EmailSenderUtil {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderUtil.class);

    @Value("${spring.mail.from}")
    private String from;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    public void sendEmail(String toEmail, String generatedCode) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "悦木", "UTF-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            // 编码邮件主题
            String encodedSubject = MimeUtility.encodeText("悦木邮箱验证码", "UTF-8", "B");
            message.setSubject(encodedSubject, "UTF-8");

            String htmlContent = readHTMLFromFile();
            htmlContent = htmlContent.replace(":data=\"verify\"", ":data=\"" + generatedCode + "\"").replace("000000", generatedCode);

            // 设置邮件内容编码
            message.setContent(htmlContent, "text/html;charset=UTF-8");

            Transport.send(message);
            logger.info("Sent message successfully to {}", toEmail);
        } catch (MessagingException | IOException e) {
            logger.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String readHTMLFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("html/vericode_email.html");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine())!= null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    public void sendReviewEmail(String toEmail, String htmlContent) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "悦木", "UTF-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            // 编码邮件主题
            String encodedSubject = MimeUtility.encodeText("悦木内容审核通知", "UTF-8", "B");
            message.setSubject(encodedSubject, "UTF-8");

            // 设置邮件内容
            message.setContent(htmlContent, "text/html;charset=UTF-8");

            Transport.send(message);
            logger.info("审核通知邮件发送成功");
        } catch (MessagingException | IOException e) {
            logger.error("审核通知邮件发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送ES同步失败通知邮件给管理员
     * 为了避免一次性发送过多邮件，这里只发送摘要信息
     *
     * @param adminEmail 管理员邮箱
     * @param dataType 同步失败的数据类型
     * @param errorMessage 错误信息摘要
     */
    public void sendEsFailureAlert(String adminEmail, String dataType, String errorMessage) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSFSocketFactory");
        properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "悦木系统", "UTF-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));

            // 编码邮件主题
            String encodedSubject = MimeUtility.encodeText("【重要】ES同步失败告警 - " + dataType, "UTF-8", "B");
            message.setSubject(encodedSubject, "UTF-8");

            // 创建简单的HTML内容，避免发送过多数据
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset='UTF-8'>\n" +
                    "    <title>ES同步失败告警</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h2>悦木系统 ES 同步失败告警</h2>\n" +
                    "    <p><strong>数据类型:</strong> " + dataType + "</p>\n" +
                    "    <p><strong>错误时间:</strong> " + new java.util.Date() + "</p>\n" +
                    "    <p><strong>错误摘要:</strong> " + errorMessage + "</p>\n" +
                    "    <p>请及时检查ES服务状态。</p>\n" +
                    "</body>\n" +
                    "</html>";

            // 设置邮件内容编码
            message.setContent(htmlContent, "text/html;charset=UTF-8");

            Transport.send(message);
            logger.info("ES同步失败告警邮件发送成功至 {}", adminEmail);
        } catch (MessagingException | IOException e) {
            logger.error("发送ES同步失败告警邮件失败: {}", e.getMessage(), e);
        }
    }
}
