package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.PrivateChatMapper;
import com.lumenglover.yuemupicturebackend.model.entity.PrivateChat;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.Userfollows;
import com.lumenglover.yuemupicturebackend.model.entity.ChatMessage;
import com.lumenglover.yuemupicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import com.lumenglover.yuemupicturebackend.service.PrivateChatService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.UserFollowsService;
import com.lumenglover.yuemupicturebackend.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.Date;

import cn.hutool.core.util.StrUtil;
import javax.servlet.http.HttpServletRequest;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.manager.websocket.ChatWebSocketServer;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import com.lumenglover.yuemupicturebackend.manager.websocket.ChatListWebSocketServer;

@Service
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatMapper, PrivateChat>
        implements PrivateChatService {

    private static final Logger log = LoggerFactory.getLogger(PrivateChatServiceImpl.class);

    @Lazy
    @Resource
    private UserFollowsService userFollowsService;

    @Resource
    private UserService userService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    @Lazy
    private ChatListWebSocketServer chatListWebSocketServer;
    /**
     * 获取用户的私聊列表（分页）
     */
    @Override
    public Page<PrivateChat> getUserPrivateChats(long userId, long current, long size) {
        // 限制每页大小
        if (size > 20) {
            size = 20;
        }

        // 创建分页对象
        Page<PrivateChat> page = new Page<>(current, size);

        // 构建查询条件
        LambdaQueryWrapper<PrivateChat> queryWrapper = new LambdaQueryWrapper<>();
        // 查询条件：用户是发送者或接收者
        queryWrapper.eq(PrivateChat::getUserId, userId)
                .or()
                .eq(PrivateChat::getTargetUserId, userId);
        // 按最后消息时间倒序排序
        queryWrapper.orderByDesc(PrivateChat::getLastMessageTime);

        // 执行分页查询
        Page<PrivateChat> privateChatPage = this.page(page, queryWrapper);

        // 填充用户信息
        this.fillPrivateChatList(privateChatPage.getRecords());

        return privateChatPage;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateChat createOrUpdatePrivateChat(long userId, long targetUserId, String content) {
        // 查找现有私聊（检查两个方向）
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        long finalUserId = userId;
        long finalTargetUserId = targetUserId;
        queryWrapper.and(wrap ->
                wrap.eq("userId", finalUserId).eq("targetUserId", finalTargetUserId)
                        .or()
                        .eq("userId", finalTargetUserId).eq("targetUserId", finalUserId)
        );

        PrivateChat privateChat = this.getOne(queryWrapper);

        if (privateChat == null) {
            // 创建新私聊
            privateChat = new PrivateChat();
            privateChat.setUserId(userId);
            privateChat.setTargetUserId(targetUserId);
            privateChat.setUserUnreadCount(0);
            privateChat.setTargetUserUnreadCount(0);

            // 检查是否互相关注来设置聊天类型
            QueryWrapper<Userfollows> followsQuery = new QueryWrapper<>();
            followsQuery.eq("followerId", userId)
                    .eq("followingId", targetUserId)
                    .eq("followStatus", 1)
                    .eq("isMutual", 1)  // 使用 isMutual 字段，该字段已经表示了双向关注状态
                    .eq("isDelete", 0);

            boolean isFriend = userFollowsService.count(followsQuery) > 0;
            privateChat.setChatType(isFriend ? 1 : 0);
        } else {
            // 如果找到的是反向记录，需要交换用户ID
            if (privateChat.getTargetUserId().equals(userId)) {
                // 交换用户ID
                long temp = userId;
                userId = targetUserId;
                targetUserId = temp;
            }
        }

        // 更新最后消息
        privateChat.setLastMessage(content);
        privateChat.setLastMessageTime(new Date());
        // 增加目标用户的未读消息数
        privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);

        this.saveOrUpdate(privateChat);

        // 创建聊天消息记录
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(userId);
        chatMessage.setReceiverId(targetUserId);
        chatMessage.setContent(content);
        chatMessage.setType(1);  // 私聊类型
        chatMessage.setStatus(0);  // 未读状态
        chatMessageService.save(chatMessage);

        return privateChat;
    }


    @Override
    public void incrementUnreadCount(long userId, long targetUserId, boolean isUser) {
        // 查找或创建私聊记录
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetUserId", targetUserId);

        PrivateChat privateChat = this.getOne(queryWrapper);
        if (privateChat == null) {
            // 如果不存在，创建新的私聊记录
            privateChat = new PrivateChat();
            privateChat.setUserId(userId);
            privateChat.setTargetUserId(targetUserId);
            if (isUser) {
                privateChat.setUserUnreadCount(1);
                privateChat.setTargetUserUnreadCount(0);
            } else {
                privateChat.setUserUnreadCount(0);
                privateChat.setTargetUserUnreadCount(1);
            }
            this.save(privateChat);
        } else {
            // 如果存在，增加对应的未读计数
            this.update()
                    .setSql(isUser ?
                            "userUnreadCount = userUnreadCount + 1" :
                            "targetUserUnreadCount = targetUserUnreadCount + 1")
                    .eq("id", privateChat.getId())
                    .update();
        }
    }

    @Override
    public void clearUnreadCount(long userId, long targetUserId, boolean isSender) {
        // 当用户进入私聊时，需要清除当前用户看到的未读消息数
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrap -> wrap
                .eq("userId", userId).eq("targetUserId", targetUserId)
                .or()
                .eq("userId", targetUserId).eq("targetUserId", userId)
        );
        PrivateChat privateChat = this.getOne(queryWrapper);

        if (privateChat != null) {
            // 根据当前用户是userId还是targetUserId来确定如何清除未读消息
            // 当用户进入私聊时，应该清除用户自己看到的未读消息数
            if (privateChat.getUserId().equals(userId)) {
                // 当前用户是userId，清除userUnreadCount（用户自己看到的未读消息）
                this.update()
                        .set("userUnreadCount", 0)
                        .eq("id", privateChat.getId())
                        .update();

                // 通知当前用户更新未读消息数
                chatListWebSocketServer.notifyUpdateUnreadCount(userId, privateChat.getId(), 0);
            } else {
                // 当前用户是targetUserId，清除targetUserUnreadCount（用户自己看到的未读消息）
                this.update()
                        .set("targetUserUnreadCount", 0)
                        .eq("id", privateChat.getId())
                        .update();

                // 通知当前用户更新未读消息数
                chatListWebSocketServer.notifyUpdateUnreadCount(userId, privateChat.getId(), 0);
            }
        }
    }

    @Override
    public boolean checkIsFriend(long userId, long targetUserId) {
        // 检查是否互相关注
        QueryWrapper<Userfollows> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", userId)
                .eq("followingId", targetUserId)
                .eq("followStatus", 1)
                .eq("isMutual", 1)
                .eq("isDelete", 0);  // 添加未删除条件

        return userFollowsService.count(queryWrapper) > 0;
    }

    @Override
    public void updateChatType(long userId, long targetUserId, boolean isFriend) {
        // 准备更新的聊天类型
        int chatType = isFriend ? 1 : 0;

        // 同时更新两个方向的记录
        this.update()
                .set("chatType", chatType)
                .and(wrap -> wrap
                        .eq("userId", userId).eq("targetUserId", targetUserId)
                        .or()
                        .eq("userId", targetUserId).eq("targetUserId", userId)
                )
                .update();
    }

    @Override
    public int getTotalUnreadCount(Long userId) {
        // 查询用户作为接收者的所有未读消息总数
        LambdaQueryWrapper<PrivateChat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrap -> wrap
                .eq(PrivateChat::getUserId, userId).gt(PrivateChat::getUserUnreadCount, 0)
                .or()
                .eq(PrivateChat::getTargetUserId, userId).gt(PrivateChat::getTargetUserUnreadCount, 0)
        );
        queryWrapper.eq(PrivateChat::getIsDelete, 0);

        // 获取所有相关的私聊记录
        List<PrivateChat> chats = this.list(queryWrapper);

        // 计算总未读数
        return chats.stream().mapToInt(chat -> {
            if (chat.getUserId().equals(userId)) {
                return chat.getUserUnreadCount();
            } else {
                return chat.getTargetUserUnreadCount();
            }
        }).sum();
    }

    @Override
    public int getFriendUnreadCount(Long userId) {
        // 查询用户作为接收者的好友未读消息总数
        LambdaQueryWrapper<PrivateChat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrap -> wrap
                .eq(PrivateChat::getUserId, userId).gt(PrivateChat::getUserUnreadCount, 0)
                .or()
                .eq(PrivateChat::getTargetUserId, userId).gt(PrivateChat::getTargetUserUnreadCount, 0)
        );
        queryWrapper.eq(PrivateChat::getChatType, 1); // 好友聊天类型
        queryWrapper.eq(PrivateChat::getIsDelete, 0);

        // 获取所有相关的私聊记录
        List<PrivateChat> chats = this.list(queryWrapper);

        // 计算好友未读数
        return chats.stream().mapToInt(chat -> {
            if (chat.getUserId().equals(userId)) {
                return chat.getUserUnreadCount();
            } else {
                return chat.getTargetUserUnreadCount();
            }
        }).sum();
    }

    @Override
    public QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser) {
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        if (privateChatQueryRequest == null) {
            return queryWrapper;
        }

        // 使用 final 修饰需要在 lambda 中使用的变量
        final Long userId = loginUser.getId();
        final Long targetUserId = privateChatQueryRequest.getTargetUserId();
        final Integer chatType = privateChatQueryRequest.getChatType();
        final String searchText = privateChatQueryRequest.getSearchText();

        // 查询与当前用户相关的聊天记录，并且排除自己和自己的对话
        queryWrapper.and(wrap ->
                wrap.eq("userId", userId).ne("targetUserId", userId)
                        .or()
                        .eq("targetUserId", userId).ne("userId", userId)
        );

        // 如果指定了目标用户，则只查询与该用户的对话
        if (targetUserId != null && targetUserId > 0) {
            queryWrapper.and(wrap ->
                    wrap.eq("targetUserId", targetUserId).eq("userId", userId)
                            .or()
                            .eq("userId", targetUserId).eq("targetUserId", userId)
            );
        }

        // 如果指定了聊天类型（私聊/好友），则按类型筛选
        if (chatType != null) {
            queryWrapper.eq("chatType", chatType);
        }

        // 搜索关键字（模糊匹配：最后一条消息, 聊天别名, 或目标用户的名称）
        if (StrUtil.isNotBlank(searchText)) {
            // 1. 通过 searchText 在 User 表查询相关的系统原有用户名对应的 userIds
            List<Long> matchedUserIds = userService.listObjs(
                    new QueryWrapper<User>().select("id").like("userName", searchText),
                    obj -> (Long) obj
            );

            // 2. 通过 searchText 在 ChatMessage 表查询相关的历史消息对应的 privateChatIds
            // 只查询属于当前用户的消息记录（发送或接收）
            List<Long> matchedChatIds = chatMessageService.listObjs(
                    new QueryWrapper<ChatMessage>().select("distinct privateChatId")
                            .like("content", searchText)
                            .and(w -> w.eq("senderId", userId).or().eq("receiverId", userId)),
                    obj -> (Long) obj
            );

            queryWrapper.and(qw -> {
                // 1. 匹配最后一条消息内容
                qw.like("lastMessage", searchText)
                        // 2. 匹配自定义聊天名称（根据身份区分字段）
                        .or(w -> w.eq("userId", userId).like("userChatName", searchText))
                        .or(w -> w.eq("targetUserId", userId).like("targetUserChatName", searchText));

                // 3. 匹配对方的系统用户名
                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(matchedUserIds)) {
                    qw.or(w -> w.eq("userId", userId).in("targetUserId", matchedUserIds))
                            .or(w -> w.eq("targetUserId", userId).in("userId", matchedUserIds));
                }

                // 4. 匹配历史消息中出现的会话 ID
                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(matchedChatIds)) {
                    qw.or().in("id", matchedChatIds);
                }
            });
        }

        // 未删除
        queryWrapper.eq("isDelete", 0);
        // 按最后消息时间倒序
        queryWrapper.orderByDesc("lastMessageTime");

        return queryWrapper;
    }

    @Override
    public Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, HttpServletRequest request) {
        Page<PrivateChat> privateChatPage = super.page(page, queryWrapper);
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            log.error("无法获取当前用户信息");
            return privateChatPage;
        }
        return processPrivateChatPage(privateChatPage, loginUser);
    }

    /**
     * WebSocket专用的分页方法，直接接收User对象
     */
    public Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<PrivateChat> privateChatPage = super.page(page, queryWrapper);
        return processPrivateChatPage(privateChatPage, loginUser);
    }

    /**
     * 处理私聊分页结果
     */
    private Page<PrivateChat> processPrivateChatPage(Page<PrivateChat> privateChatPage, User loginUser) {
        final Long currentUserId = loginUser.getId();

        // 填充目标用户信息并处理未读消息数和聊天名称
        if (privateChatPage.getRecords() != null) {
            privateChatPage.getRecords().forEach(chat -> {
                Long targetId;
                String chatName;
                // 如果当前用户是接收者，需要交换未读消息数和使用对应的聊天名称
                if (chat.getTargetUserId().equals(currentUserId)) {
                    targetId = chat.getUserId();
                    // 交换未读消息数
                    Integer temp = chat.getUserUnreadCount();
                    chat.setUserUnreadCount(chat.getTargetUserUnreadCount());
                    chat.setTargetUserUnreadCount(temp);
                    chat.setIsSender(false);  // 设置当前用户是接收者
                    // 使用目标用户的自定义聊天名称
                    chatName = chat.getTargetUserChatName();
                } else {
                    targetId = chat.getTargetUserId();
                    chat.setIsSender(true);   // 设置当前用户是发送者
                    // 使用用户的自定义聊天名称
                    chatName = chat.getUserChatName();
                }

                // 获取目标用户信息
                User targetUser = userService.getById(targetId);
                if (targetUser != null) {
                    UserVO userVO = userService.getUserVO(targetUser);
                    chat.setTargetUser(userVO);
                    // 如果没有自定义聊天名称，使用目标用户的用户名
                    if (StrUtil.isBlank(chatName)) {
                        chatName = userVO.getUserName();
                    }
                    log.info("User info found for targetId: {}", targetId);
                } else {
                    log.warn("User not found for targetId: {}", targetId);
                }

                // 设置显示的聊天名称
                if (chat.getIsSender()) {
                    chat.setUserChatName(chatName);
                } else {
                    chat.setTargetUserChatName(chatName);
                }
            });
        }

        return privateChatPage;
    }

    @Override
    public Page<ChatMessage> getPrivateChatHistory(Long userId, Long targetUserId, Long page, Long size) {
        // 查询消息记录
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrap ->
                        wrap.eq("senderId", userId).eq("receiverId", targetUserId)
                                .or()
                                .eq("senderId", targetUserId).eq("receiverId", userId)
                )
                .eq("type", 1)  // 私聊类型
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        Page<ChatMessage> messagePage = chatMessageService.page(new Page<>(page, size), queryWrapper);

        // 填充发送者信息
        messagePage.getRecords().forEach(message -> {
            // 填充发送者信息
            User sender = userService.getById(message.getSenderId());
            sender.setUserPassword(null);
            message.setSender(sender);
        });

        return messagePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePrivateChatMessage(ChatMessage chatMessage, Long privateChatId, User sender) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前用户ID确定接收者ID
        Long receiverId;
        if (privateChat.getUserId().equals(sender.getId())) {
            receiverId = privateChat.getTargetUserId();
            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketServer.getPrivateChatSessions(privateChatId);
            // 检查接收者是否在当前私聊中
            boolean isReceiverInChat = sessions != null && sessions.stream()
                    .map(session -> (User) session.getAttributes().get("user"))
                    .filter(user -> user != null)
                    .anyMatch(user -> user.getId().equals(receiverId));

            // 如果接收者不在当前私聊中，增加未读消息数并通知更新
            if (!isReceiverInChat) {
                privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);
                // 通知接收者更新未读消息数和聊天列表
                chatListWebSocketServer.notifyUpdateUnreadCount(receiverId, privateChatId, privateChat.getTargetUserUnreadCount());
                chatListWebSocketServer.notifyUpdateChatList(receiverId);
            }
        } else if (privateChat.getTargetUserId().equals(sender.getId())) {
            receiverId = privateChat.getUserId();
            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketServer.getPrivateChatSessions(privateChatId);
            // 检查接收者是否在当前私聊中
            boolean isReceiverInChat = sessions != null && sessions.stream()
                    .map(session -> (User) session.getAttributes().get("user"))
                    .filter(user -> user != null)
                    .anyMatch(user -> user.getId().equals(receiverId));

            // 如果接收者不在当前私聊中，增加未读消息数并通知更新
            if (!isReceiverInChat) {
                privateChat.setUserUnreadCount(privateChat.getUserUnreadCount() + 1);
                // 通知接收者更新未读消息数和聊天列表
                chatListWebSocketServer.notifyUpdateUnreadCount(receiverId, privateChatId, privateChat.getUserUnreadCount());
                chatListWebSocketServer.notifyUpdateChatList(receiverId);
            }
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }
        chatMessage.setReceiverId(receiverId);
        chatMessage.setSenderId(sender.getId());

        // 更新私聊记录的最后一句内容
        privateChat.setLastMessage(chatMessage.getContent());
        privateChat.setLastMessageTime(new Date());

        // 保存更新
        this.updateById(privateChat);

        // 通知双方更新聊天列表
        chatListWebSocketServer.notifyUpdateChatList(sender.getId());
        chatListWebSocketServer.notifyUpdateChatList(receiverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePrivateChat(Long privateChatId, User loginUser) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 校验权限，只有私聊参与者才能删除
        if (!privateChat.getUserId().equals(loginUser.getId())
                && !privateChat.getTargetUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        // 1. 删除私聊记录
        boolean success = this.removeById(privateChatId);

        if (success) {
            // 2. 删除相关的聊天消息
            QueryWrapper<ChatMessage> messageQueryWrapper = new QueryWrapper<>();
            messageQueryWrapper.eq("privateChatId", privateChatId)
                    .eq("type", 1);  // 私聊类型
            chatMessageService.remove(messageQueryWrapper);

            // 3. 通知双方删除聊天
            chatListWebSocketServer.notifyDeleteChat(privateChat.getUserId(), privateChatId);
            chatListWebSocketServer.notifyDeleteChat(privateChat.getTargetUserId(), privateChatId);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChatName(Long privateChatId, String chatName, User loginUser) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前用户身份更新对应的聊天名称
        if (privateChat.getUserId().equals(loginUser.getId())) {
            privateChat.setUserChatName(chatName);
            // 通知用户更新聊天名称
            chatListWebSocketServer.notifyUpdateChatName(loginUser.getId(), privateChatId, chatName);
        } else if (privateChat.getTargetUserId().equals(loginUser.getId())) {
            privateChat.setTargetUserChatName(chatName);
            // 通知用户更新聊天名称
            chatListWebSocketServer.notifyUpdateChatName(loginUser.getId(), privateChatId, chatName);
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        this.updateById(privateChat);
    }

    /**
     * 填充私聊列表的用户信息
     */
    private void fillPrivateChatList(List<PrivateChat> chatList) {
        if (CollectionUtils.isEmpty(chatList)) {
            return;
        }

        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        chatList.forEach(chat -> {
            userIds.add(chat.getUserId());
            userIds.add(chat.getTargetUserId());
        });

        // 批量查询用户信息
        Map<Long, User> userMap = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 填充用户信息
        chatList.forEach(chat -> {
            // 获取目标用户
            User targetUser = userMap.get(chat.getTargetUserId());
            if (targetUser != null) {
                UserVO userVO = userService.getUserVO(targetUser);
                chat.setTargetUser(userVO);
            }

            // 设置发送者标志和处理未读消息数
            if (chat.getTargetUserId().equals(chat.getUserId())) {
                // 交换未读消息数
                Integer temp = chat.getUserUnreadCount();
                chat.setUserUnreadCount(chat.getTargetUserUnreadCount());
                chat.setTargetUserUnreadCount(temp);
                chat.setIsSender(false);  // 设置当前用户是接收者
            } else {
                chat.setIsSender(true);   // 设置当前用户是发送者
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadCount(Long userId) {
        // 1. 查询所有相关的聊天记录
        LambdaQueryWrapper<PrivateChat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PrivateChat::getIsDelete, 0)
                .and(w -> w.eq(PrivateChat::getUserId, userId)
                        .or()
                        .eq(PrivateChat::getTargetUserId, userId));
        List<PrivateChat> chats = this.list(queryWrapper);

        // 2. 遍历每个聊天记录，清除未读消息并通知相关用户
        for (PrivateChat chat : chats) {
            // 保存原始未读消息数，用于判断是否需要通知
            int originalUserUnread = chat.getUserUnreadCount();
            int originalTargetUnread = chat.getTargetUserUnreadCount();

            // 根据当前用户是发送者还是接收者来清除对应的未读消息数
            if (chat.getUserId().equals(userId)) {
                // 当前用户是发送者，清除userUnreadCount
                chat.setUserUnreadCount(0);
                if (originalUserUnread > 0) {
                    // 通知发送者更新未读消息数
                    chatListWebSocketServer.notifyUpdateUnreadCount(userId, chat.getId(), 0);
                }
            } else {
                // 当前用户是接收者，清除targetUserUnreadCount
                chat.setTargetUserUnreadCount(0);
                if (originalTargetUnread > 0) {
                    // 通知接收者更新未读消息数
                    chatListWebSocketServer.notifyUpdateUnreadCount(userId, chat.getId(), 0);
                }
            }

            // 更新聊天记录
            this.updateById(chat);
        }

        // 3. 通知用户更新聊天列表
        chatListWebSocketServer.notifyUpdateChatList(userId);

        // 4. 记录日志
        log.info("用户 {} 清除了所有未读消息，影响的聊天记录数：{}", userId, chats.size());
    }
}
