package com.lumenglover.yuemupicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.model.entity.FriendLink;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.service.FriendLinkService;
import com.lumenglover.yuemupicturebackend.service.PictureService;
import com.lumenglover.yuemupicturebackend.service.PostService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
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
import java.util.*;

@Component
@Slf4j
public class ContentReviewJob {

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @Resource
    private FriendLinkService friendLinkService;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    @Value("${spring.mail.admin}")
    private String adminEmail;

    private Date lastCheckTime = null;

    /**
     * 每天凌晨1点检查前一天的数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyCheck() {
        log.info("开始执行每日审核检查");

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

        // 检查并发送邮件
        checkAndSendEmail(startTime, endTime, true);

        // 更新最后检查时间
        lastCheckTime = new Date();
    }

    private void checkAndSendEmail(Date startTime, Date endTime, boolean isDaily) {
        try {
            // 查询未审核的公共图库图片（spaceId为null或0）且非草稿状态
            QueryWrapper<Picture> pictureWrapper = new QueryWrapper<Picture>()
                    .eq("reviewStatus", 0)
                    .eq("isDelete", 0)
                    .eq("isDraft", 0)
                    .between("createTime", startTime, endTime)
                    .and(wrapper -> wrapper
                            .isNull("spaceId")
                            .or()
                            .eq("spaceId", 0)
                    );

            // 查询未审核的帖子（非草稿）
            QueryWrapper<Post> postWrapper = new QueryWrapper<Post>()
                    .eq("status", 0)
                    .eq("isDelete", 0)
                    .eq("isDraft", 0)  // 非草稿状态
                    .between("createTime", startTime, endTime);

            // 查询未审核的友链
            QueryWrapper<FriendLink> friendLinkWrapper = new QueryWrapper<FriendLink>()
                    .eq("status", 0)
                    .eq("isDelete", 0)
                    .between("createTime", startTime, endTime);

            List<Picture> pictures = pictureService.list(pictureWrapper);
            List<Post> posts = postService.list(postWrapper);
            List<FriendLink> friendLinks = friendLinkService.list(friendLinkWrapper);

            // 如果有未审核的内容，发送邮件
            if (!pictures.isEmpty() || !posts.isEmpty() || !friendLinks.isEmpty()) {
                String emailContent = generateEmailContent(pictures, posts, friendLinks, isDaily);
                emailSenderUtil.sendReviewEmail(adminEmail, emailContent);
                log.info("已发送审核通知邮件，未审核公共图片：{}张，未审核帖子：{}条，未审核友链：{}条",
                        pictures.size(), posts.size(), friendLinks.size());
            }
        } catch (Exception e) {
            log.error("审核检查任务执行失败", e);
        }
    }

    private String generateEmailContent(List<Picture> pictures, List<Post> posts, List<FriendLink> friendLinks, boolean isDaily)
            throws IOException {
        // 读取HTML模板
        String template = readHtmlTemplate();
        StringBuilder tableContent = new StringBuilder();
        int totalCount = pictures.size() + posts.size() + friendLinks.size();

        // 生成图片行
        for (Picture picture : pictures) {
            User user = userService.getById(picture.getUserId());
            tableContent.append(String.format("<tr>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td><span class='type picture'>图片</span></td>" +
                            "<td>%s</td>" +
                            "</tr>",
                    user.getId(),
                    user.getEmail(),
                    picture.getId(),
                    picture.getName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(picture.getCreateTime())
            ));
        }

        // 生成帖子行
        for (Post post : posts) {
            User user = userService.getById(post.getUserId());
            tableContent.append(String.format("<tr>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td><span class='type post'>帖子</span></td>" +
                            "<td>%s</td>" +
                            "</tr>",
                    user.getId(),
                    user.getEmail(),
                    post.getId(),
                    post.getTitle(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(post.getCreateTime())
            ));
        }

        // 生成友链行
        for (FriendLink friendLink : friendLinks) {
            User user = userService.getById(friendLink.getUserId());
            tableContent.append(String.format("<tr>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td>%d</td>" +
                            "<td>%s</td>" +
                            "<td><span class='type friend-link'>友链</span></td>" +
                            "<td>%s</td>" +
                            "</tr>",
                    user.getId(),
                    user.getEmail(),
                    friendLink.getId(),
                    friendLink.getSiteName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(friendLink.getCreateTime())
            ));
        }

        // 替换模板中的占位符
        return template
                .replace(":count", String.valueOf(totalCount))
                .replace(":table_content", tableContent.toString());
    }

    private String readHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("html/review_notification.html");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}
