package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.model.entity.Report;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.ReportService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import com.lumenglover.yuemupicturebackend.model.enums.ReportStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ReportReviewJob {

    @Resource
    private ReportService reportService;

    @Resource
    private UserService userService;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Value("${spring.mail.admin}")
    private String adminEmail;

    /**
     * 每2小时检查一次未处理的举报
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void checkUnprocessedReports() {
        log.info("开始执行举报审核检查");

        // 查询待处理的举报（状态为0）
        QueryWrapper<Report> reportWrapper = new QueryWrapper<>();
        reportWrapper.eq("status", 0); // 0表示待处理
        reportWrapper.eq("isDelete", 0); // 未删除的举报

        List<Report> unprocessedReports = reportService.list(reportWrapper);

        if (!unprocessedReports.isEmpty()) {
            log.info("发现 {} 条未处理的举报", unprocessedReports.size());

            // 发送邮件通知
            String emailContent = generateReportEmailContent(unprocessedReports);
            emailSenderUtil.sendReviewEmail(adminEmail, emailContent);

            log.info("已发送举报审核通知邮件，共 {} 条未处理举报", unprocessedReports.size());
        } else {
            log.info("没有发现未处理的举报");
        }
    }

    /**
     * 每天凌晨3点对前一天的举报进行全量检查
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyReportCheck() {
        log.info("开始执行每日举报全量检查");

        // 获取昨天的时间范围
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startTime = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endTime = calendar.getTime();

        // 查询指定时间段内的所有举报
        QueryWrapper<Report> reportWrapper = new QueryWrapper<>();
        reportWrapper.between("createTime", startTime, endTime);
        reportWrapper.eq("isDelete", 0); // 未删除的举报

        List<Report> allReports = reportService.list(reportWrapper);

        if (!allReports.isEmpty()) {
            // 过滤出未处理的举报
            List<Report> unprocessedReports = allReports.stream()
                    .filter(report -> report.getStatus() == 0)
                    .collect(java.util.stream.Collectors.toList());

            if (!unprocessedReports.isEmpty()) {
                log.info("昨日发现 {} 条未处理的举报", unprocessedReports.size());

                // 发送邮件通知
                String emailContent = generateReportEmailContent(unprocessedReports);
                emailSenderUtil.sendReviewEmail(adminEmail, emailContent);

                log.info("已发送举报审核通知邮件，昨日共 {} 条未处理举报", unprocessedReports.size());
            } else {
                log.info("昨日所有举报均已处理完毕");
            }
        } else {
            log.info("昨日没有新的举报");
        }
    }

    private String generateReportEmailContent(List<Report> reports) {
        try {
            // 读取HTML模板
            String template = readReportHtmlTemplate();
            StringBuilder tableContent = new StringBuilder();

            // 生成举报行
            for (Report report : reports) {
                User user = userService.getById(report.getUserId());

                String targetTypeText = getTargetTypeText(report.getTargetType());
                String reportTypeText = getReportTypeText(report.getReportType());

                tableContent.append(String.format(
                    "<tr>" +
                    "<td>%d</td>" +
                    "<td>%s</td>" +
                    "<td>%d</td>" +
                    "<td>%s</td>" +
                    "<td>%s</td>" +
                    "<td>%s</td>" +
                    "<td>%s</td>" +
                    "</tr>",
                    user != null ? user.getId() : -1,
                    user != null ? user.getEmail() : "未知",
                    report.getId(),
                    targetTypeText,
                    reportTypeText,
                    report.getReason(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(report.getCreateTime())
                ));
            }

            // 替换模板中的占位符
            return template
                    .replace(":count", String.valueOf(reports.size()))
                    .replace(":table_content", tableContent.toString());
        } catch (IOException e) {
            log.error("生成举报邮件内容失败", e);
            return "发现未处理举报，请登录系统查看";
        }
    }

    private String readReportHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("html/report_review_notification.html");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private String getTargetTypeText(Integer targetType) {
        if (targetType == null) return "未知";

        switch (targetType) {
            case 1: return "图片";
            case 2: return "帖子";
            case 3: return "评论";
            case 4: return "用户";
            case 5: return "其他";
            default: return "未知";
        }
    }

    private String getReportTypeText(Integer reportType) {
        if (reportType == null) return "未知";

        switch (reportType) {
            case 1: return "侵权";
            case 2: return "色情";
            case 3: return "政治敏感";
            case 4: return "暴力恐怖";
            case 5: return "广告营销";
            case 6: return "谣言";
            case 7: return "其他";
            default: return "未知";
        }
    }
}
