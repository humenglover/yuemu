package com.lumenglover.yuemupicturebackend.utils;

import com.lumenglover.yuemupicturebackend.model.entity.SystemNotify;
import com.lumenglover.yuemupicturebackend.service.SystemNotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;

/**
 * 系统通知工具类
 */
@Component
public class SystemNotifyUtil {

    @Resource
    private SystemNotifyService systemNotifyService;

    private static SystemNotifyUtil systemNotifyUtil;

    @PostConstruct
    public void init() {
        systemNotifyUtil = this;
        systemNotifyUtil.systemNotifyService = this.systemNotifyService;
    }

    /**
     * 发送图片审核通过通知
     *
     * @param userId  用户ID
     * @param pictureId 图片ID
     * @param pictureName 图片名称
     */
    public static void sendPictureApprovedNotify(Long userId, Long pictureId, String pictureName) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setCreateTime(new Date());
        systemNotify.setUpdateTime(new Date());
        systemNotify.setOperatorId("system");
        systemNotify.setOperatorType("SYSTEM");
        systemNotify.setNotifyType("PICTURE_APPROVED");
        systemNotify.setSenderType("SYSTEM");
        systemNotify.setSenderId("system");
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setTitle("图片审核通过");
        systemNotify.setContent(String.format("您的图片《%s》已通过审核", pictureName));
        systemNotify.setNotifyIcon("approve");
        systemNotify.setRelatedBizType("PICTURE");
        systemNotify.setRelatedBizId(String.valueOf(pictureId));
        systemNotify.setReadStatus(0);
        systemNotify.setIsGlobal(0);
        systemNotify.setIsEnabled(1);
        systemNotify.setIsDelete(0);

        systemNotifyUtil.systemNotifyService.addSystemNotify(systemNotify);
    }

    /**
     * 发送图片审核不通过通知
     *
     * @param userId  用户ID
     * @param pictureId 图片ID
     * @param pictureName 图片名称
     * @param reason  拒绝原因
     */
    public static void sendPictureRejectedNotify(Long userId, Long pictureId, String pictureName, String reason) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setCreateTime(new Date());
        systemNotify.setUpdateTime(new Date());
        systemNotify.setOperatorId("system");
        systemNotify.setOperatorType("SYSTEM");
        systemNotify.setNotifyType("PICTURE_REJECTED");
        systemNotify.setSenderType("SYSTEM");
        systemNotify.setSenderId("system");
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setTitle("图片审核未通过");
        systemNotify.setContent(String.format("您的图片《%s》未通过审核，原因：%s", pictureName, reason));
        systemNotify.setNotifyIcon("reject");
        systemNotify.setRelatedBizType("PICTURE");
        systemNotify.setRelatedBizId(String.valueOf(pictureId));
        systemNotify.setReadStatus(0);
        systemNotify.setIsGlobal(0);
        systemNotify.setIsEnabled(1);
        systemNotify.setIsDelete(0);

        systemNotifyUtil.systemNotifyService.addSystemNotify(systemNotify);
    }

    /**
     * 发送帖子审核通过通知
     *
     * @param userId  用户ID
     * @param postId  帖子ID
     * @param postTitle 帖子标题
     */
    public static void sendPostApprovedNotify(Long userId, Long postId, String postTitle) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setCreateTime(new Date());
        systemNotify.setUpdateTime(new Date());
        systemNotify.setOperatorId("system");
        systemNotify.setOperatorType("SYSTEM");
        systemNotify.setNotifyType("POST_APPROVED");
        systemNotify.setSenderType("SYSTEM");
        systemNotify.setSenderId("system");
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setTitle("帖子审核通过");
        systemNotify.setContent(String.format("您的帖子《%s》已通过审核", postTitle));
        systemNotify.setNotifyIcon("approve");
        systemNotify.setRelatedBizType("POST");
        systemNotify.setRelatedBizId(String.valueOf(postId));
        systemNotify.setReadStatus(0);
        systemNotify.setIsGlobal(0);
        systemNotify.setIsEnabled(1);
        systemNotify.setIsDelete(0);

        systemNotifyUtil.systemNotifyService.addSystemNotify(systemNotify);
    }

    /**
     * 发送帖子审核不通过通知
     *
     * @param userId  用户ID
     * @param postId  帖子ID
     * @param postTitle 帖子标题
     * @param reason  拒绝原因
     */
    public static void sendPostRejectedNotify(Long userId, Long postId, String postTitle, String reason) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setCreateTime(new Date());
        systemNotify.setUpdateTime(new Date());
        systemNotify.setOperatorId("system");
        systemNotify.setOperatorType("SYSTEM");
        systemNotify.setNotifyType("POST_REJECTED");
        systemNotify.setSenderType("SYSTEM");
        systemNotify.setSenderId("system");
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setTitle("帖子审核未通过");
        systemNotify.setContent(String.format("您的帖子《%s》未通过审核，原因：%s", postTitle, reason));
        systemNotify.setNotifyIcon("reject");
        systemNotify.setRelatedBizType("POST");
        systemNotify.setRelatedBizId(String.valueOf(postId));
        systemNotify.setReadStatus(0);
        systemNotify.setIsGlobal(0);
        systemNotify.setIsEnabled(1);
        systemNotify.setIsDelete(0);

        systemNotifyUtil.systemNotifyService.addSystemNotify(systemNotify);
    }

    /**
     * 发送图片设为精选通知
     *
     * @param userId  用户ID
     * @param pictureId 图片ID
     * @param pictureName 图片名称
     */
    public static void sendPictureFeaturedNotify(Long userId, Long pictureId, String pictureName) {
        SystemNotify systemNotify = new SystemNotify();
        systemNotify.setCreateTime(new Date());
        systemNotify.setUpdateTime(new Date());
        systemNotify.setOperatorId("system");
        systemNotify.setOperatorType("SYSTEM");
        systemNotify.setNotifyType("PICTURE_FEATURED");
        systemNotify.setSenderType("SYSTEM");
        systemNotify.setSenderId("system");
        systemNotify.setReceiverType("SPECIFIC_USER");
        systemNotify.setReceiverId(String.valueOf(userId));
        systemNotify.setTitle("图片被设为精选");
        systemNotify.setContent(String.format("恭喜！您的图片《%s》被设为精选", pictureName));
        systemNotify.setNotifyIcon("featured");
        systemNotify.setRelatedBizType("PICTURE");
        systemNotify.setRelatedBizId(String.valueOf(pictureId));
        systemNotify.setReadStatus(0);
        systemNotify.setIsGlobal(0);
        systemNotify.setIsEnabled(1);
        systemNotify.setIsDelete(0);

        systemNotifyUtil.systemNotifyService.addSystemNotify(systemNotify);
    }
}
