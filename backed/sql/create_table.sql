CREATE TABLE activity
(
    id            bigint AUTO_INCREMENT COMMENT '活动ID'
        PRIMARY KEY,
    userId        bigint                             NOT NULL COMMENT '发布用户ID',
    title         varchar(100)                       NOT NULL COMMENT '标题',
    content       text                               NOT NULL COMMENT '内容',
    coverUrl      varchar(255)                       NOT NULL COMMENT '封面图片URL',
    viewCount     bigint   DEFAULT 0                 NULL COMMENT '浏览量',
    likeCount     bigint   DEFAULT 0                 NULL COMMENT '点赞数',
    commentCount  bigint   DEFAULT 0                 NULL COMMENT '评论数',
    status        tinyint  DEFAULT 0                 NULL COMMENT '状态 0-待审核 1-已发布 2-已拒绝',
    reviewMessage varchar(255)                       NULL COMMENT '审核信息',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NULL COMMENT '是否删除',
    shareCount    bigint   DEFAULT 0                 NULL COMMENT '分享数',
    expireTime    datetime                           NOT NULL COMMENT '活动过期时间',
    isExpired     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否过期 0-未过期 1-已过期'
)
    COMMENT '活动表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_expireTime
    ON activity (expireTime);

CREATE INDEX idx_isExpired
    ON activity (isExpired);

CREATE INDEX idx_status
    ON activity (status);

CREATE INDEX idx_userId
    ON activity (userId);

CREATE TABLE aichat
(
    id         bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId     bigint                                NOT NULL COMMENT '用户ID',
    content    text                                  NOT NULL COMMENT '消息内容',
    role       varchar(10) DEFAULT 'user'            NOT NULL COMMENT '角色类型（user-用户, assistant-AI助手）',
    createTime datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    isDeleted  tinyint     DEFAULT 0                 NOT NULL COMMENT '是否删除（0-未删除，1-已删除）',
    sessionId  bigint                                NOT NULL COMMENT '会话ID'
)
    COMMENT '聊天消息表';

CREATE TABLE app_version
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    version     varchar(32)                        NOT NULL COMMENT '版本号',
    versionCode int                                NOT NULL COMMENT '版本码',
    apkPath     varchar(256)                       NOT NULL COMMENT 'APK文件路径',
    apkSize     bigint                             NOT NULL COMMENT 'APK文件大小',
    description text                               NULL COMMENT '版本描述',
    isForce     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否强制更新 0-否 1-是',
    status      tinyint  DEFAULT 1                 NOT NULL COMMENT '状态 0-禁用 1-启用',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT 'APP版本管理表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_version_code
    ON app_version (versionCode DESC);

CREATE TABLE audio_file
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '上传用户id',
    fileName    varchar(255)                       NOT NULL COMMENT '文件名',
    fileUrl     varchar(1024)                      NOT NULL COMMENT '文件访问地址',
    filePath    varchar(1024)                      NOT NULL COMMENT '文件存储路径',
    fileSize    bigint                             NOT NULL COMMENT '文件大小(字节)',
    duration    int                                NULL COMMENT '音频时长(秒)',
    mimeType    varchar(128)                       NOT NULL COMMENT '文件MIME类型',
    md5         varchar(32)                        NOT NULL COMMENT '文件MD5值',
    coverUrl    varchar(1024)                      NULL COMMENT '封面图片URL',
    title       varchar(255)                       NULL COMMENT '音频标题',
    description text                               NULL COMMENT '音频描述',
    artist      varchar(255)                       NULL COMMENT '艺术家/作者',
    album       varchar(255)                       NULL COMMENT '专辑名称',
    genre       varchar(64)                        NULL COMMENT '音乐类型/风格',
    spaceId     bigint                             NULL COMMENT '所属空间ID',
    viewCount   bigint   DEFAULT 0                 NOT NULL COMMENT '播放次数',
    likeCount   bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '音频文件表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_createTime
    ON audio_file (createTime);

CREATE INDEX idx_spaceId
    ON audio_file (spaceId);

CREATE INDEX idx_userId
    ON audio_file (userId);

CREATE INDEX idx_viewCount
    ON audio_file (viewCount);

CREATE TABLE category
(
    id           bigint AUTO_INCREMENT COMMENT '分类id'
        PRIMARY KEY,
    categoryName varchar(256)                       NOT NULL COMMENT '分类名称',
    type         tinyint  DEFAULT 0                 NOT NULL COMMENT '分类类型：0-图片分类 1-帖子分类',
    createTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '分类编辑时间',
    updateTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '分类更新时间',
    isDelete     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '分类' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chat_message
(
    id            bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    senderId      bigint                             NOT NULL COMMENT '发送者id',
    receiverId    bigint                             NULL COMMENT '接收者id',
    pictureId     bigint                             NULL COMMENT '图片id',
    content       text                               NOT NULL COMMENT '消息内容',
    type          tinyint  DEFAULT 1                 NOT NULL COMMENT '消息类型 1-私聊 2-图片聊天室',
    status        tinyint  DEFAULT 0                 NOT NULL COMMENT '状态 0-未读 1-已读',
    replyId       bigint                             NULL COMMENT '回复的消息id',
    rootId        bigint                             NULL COMMENT '会话根消息id',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceId       bigint                             NULL COMMENT '空间id',
    privateChatId bigint                             NULL COMMENT '私聊ID'
)
    COMMENT '聊天消息表';

CREATE INDEX idx_picture
    ON chat_message (pictureId);

CREATE INDEX idx_private_chat
    ON chat_message (privateChatId);

CREATE INDEX idx_reply
    ON chat_message (replyId);

CREATE INDEX idx_root
    ON chat_message (rootId);

CREATE INDEX idx_sender_receiver
    ON chat_message (senderId, receiverId);

CREATE INDEX idx_space
    ON chat_message (spaceId);

CREATE TABLE comments
(
    commentId       bigint AUTO_INCREMENT
        PRIMARY KEY,
    userId          bigint                               NOT NULL,
    targetId        bigint                               NOT NULL COMMENT '评论目标ID',
    targetType      tinyint    DEFAULT 1                 NOT NULL COMMENT '评论目标类型：1-图片 2-帖子',
    targetUserId    bigint                               NOT NULL COMMENT '评论目标所属用户ID',
    content         text                                 NOT NULL,
    createTime      datetime   DEFAULT CURRENT_TIMESTAMP NULL,
    parentCommentId bigint     DEFAULT 0                 NULL COMMENT '0表示顶级',
    isDelete        tinyint(1) DEFAULT 0                 NULL,
    likeCount       bigint     DEFAULT 0                 NULL,
    dislikeCount    bigint     DEFAULT 0                 NULL,
    isRead          tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）'
);

CREATE INDEX idx_target
    ON comments (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON comments (targetUserId, isRead);

CREATE TABLE game_2048_record
(
    id         bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId     bigint                             NOT NULL COMMENT '用户ID',
    score      int      DEFAULT 0                 NOT NULL COMMENT '得分',
    maxTile    int      DEFAULT 2                 NOT NULL COMMENT '最大数字',
    gameTime   int      DEFAULT 0                 NOT NULL COMMENT '游戏时长(秒)',
    moveCount  int      DEFAULT 0                 NOT NULL COMMENT '移动次数',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '2048游戏记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_score
    ON game_2048_record (score DESC);

CREATE INDEX idx_userId_score
    ON game_2048_record (userId ASC, score DESC);

CREATE TABLE hot_search
(
    id             bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    keyword        varchar(128)                       NOT NULL COMMENT '搜索关键词',
    type           varchar(32)                        NOT NULL COMMENT '搜索类型',
    count          bigint   DEFAULT 0                 NOT NULL COMMENT '搜索次数',
    lastUpdateTime datetime                           NOT NULL COMMENT '最后更新时间',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_type_keyword
        UNIQUE (type, keyword)
)
    COMMENT '热门搜索记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_lastUpdateTime
    ON hot_search (lastUpdateTime);

CREATE INDEX idx_type_count
    ON hot_search (type ASC, count DESC);

CREATE TABLE like_record
(
    id            bigint AUTO_INCREMENT COMMENT '主键 ID'
        PRIMARY KEY,
    userId        bigint                               NOT NULL COMMENT '用户 ID',
    targetId      bigint                               NOT NULL COMMENT '被点赞内容的ID',
    targetType    tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId  bigint                               NOT NULL COMMENT '被点赞内容所属用户ID',
    isLiked       tinyint(1)                           NOT NULL COMMENT '是否点赞',
    firstLikeTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '第一次点赞时间',
    lastLikeTime  datetime                             NOT NULL COMMENT '最近一次点赞时间',
    isRead        tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）',
    CONSTRAINT uk_user_target
        UNIQUE (userId, targetId, targetType)
)
    COMMENT '通用点赞表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target
    ON like_record (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON like_record (targetUserId, isRead);

CREATE INDEX idx_userId_isRead
    ON like_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON like_record (userId, targetType);

CREATE TABLE love_board
(
    id             bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userId         bigint                             NOT NULL COMMENT '用户ID',
    bgCover        varchar(256)                       NOT NULL COMMENT '背景封面',
    manCover       varchar(256)                       NOT NULL COMMENT '男生头像',
    womanCover     varchar(256)                       NOT NULL COMMENT '女生头像',
    manName        varchar(32)                        NOT NULL COMMENT '男生昵称',
    womanName      varchar(32)                        NOT NULL COMMENT '女生昵称',
    timing         varchar(32)                        NOT NULL COMMENT '计时',
    countdownTitle varchar(32)                        NULL COMMENT '倒计时标题',
    countdownTime  varchar(32)                        NULL COMMENT '倒计时时间',
    status         tinyint  DEFAULT 1                 NOT NULL COMMENT '是否启用[0:否，1:是]',
    familyInfo     varchar(1024)                      NULL COMMENT '额外信息',
    likeCount      bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '恋爱画板' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_status
    ON love_board (status);

CREATE INDEX idx_userId
    ON love_board (userId);

CREATE TABLE love_board_music_album
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '用户ID',
    loveBoardId bigint                             NOT NULL COMMENT '恋爱板ID',
    albumName   varchar(128)                       NOT NULL COMMENT '专栏名称',
    coverUrl    varchar(512)                       NULL COMMENT '专栏封面URL',
    description varchar(512)                       NULL COMMENT '专栏描述',
    isPublic    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否公开[0-私密，1-公开]',
    password    varchar(32)                        NULL COMMENT '专栏访问密码',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '恋爱板音乐专栏表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_albumName
    ON love_board_music_album (albumName);

CREATE INDEX idx_isPublic
    ON love_board_music_album (isPublic);

CREATE INDEX idx_loveBoardId
    ON love_board_music_album (loveBoardId);

CREATE INDEX idx_userId
    ON love_board_music_album (userId);

CREATE TABLE message
(
    id         bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    content    text                                 NOT NULL COMMENT '留言内容',
    createTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint(1) DEFAULT 0                 NULL COMMENT '是否删除(0-未删除 1-已删除)',
    ip         varchar(50)                          NULL COMMENT 'IP地址'
)
    COMMENT '留言板表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_createTime
    ON message (createTime);

CREATE TABLE message_board
(
    id         bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    ownerId    bigint                             NOT NULL COMMENT '祝福板主人ID',
    userId     bigint                             NULL COMMENT '留言用户ID',
    nickname   varchar(50)                        NULL COMMENT '昵称',
    content    text                               NOT NULL COMMENT '留言内容',
    qq         varchar(20)                        NULL COMMENT 'QQ号',
    location   varchar(100)                       NULL COMMENT '地理位置',
    browser    varchar(255)                       NULL,
    os         varchar(50)                        NULL COMMENT '操作系统信息',
    ipAddress  varchar(50)                        NULL COMMENT 'IP地址',
    likeCount  int      DEFAULT 0                 NULL COMMENT '点赞数',
    status     tinyint  DEFAULT 1                 NULL COMMENT '状态 0-隐藏 1-显示',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '祝福板表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_createTime
    ON message_board (createTime);

CREATE INDEX idx_ownerId
    ON message_board (ownerId);

CREATE INDEX idx_userId
    ON message_board (userId);

CREATE TABLE picture
(
    id             bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    url            varchar(512)                       NOT NULL COMMENT '图片 url',
    name           varchar(128)                       NOT NULL COMMENT '图片名称',
    introduction   varchar(512)                       NULL COMMENT '简介',
    category       varchar(64)                        NULL COMMENT '分类',
    tags           varchar(512)                       NULL COMMENT '标签（JSON 数组）',
    picSize        bigint                             NULL COMMENT '图片体积',
    picWidth       int                                NULL COMMENT '图片宽度',
    picHeight      int                                NULL COMMENT '图片高度',
    picScale       double                             NULL COMMENT '图片宽高比例',
    picFormat      varchar(32)                        NULL COMMENT '图片格式',
    userId         bigint                             NOT NULL COMMENT '创建用户 id',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime       datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    reviewStatus   int      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage  varchar(512)                       NULL COMMENT '审核信息',
    reviewerId     bigint                             NULL COMMENT '审核人 ID',
    reviewTime     datetime                           NULL COMMENT '审核时间',
    thumbnailUrl   varchar(512)                       NULL COMMENT '缩略图 url',
    spaceId        bigint                             NULL COMMENT '空间 id（为空表示公共空间）',
    picColor       varchar(16)                        NULL COMMENT '图片主色调',
    commentCount   bigint   DEFAULT 0                 NOT NULL COMMENT '评论数',
    likeCount      bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    shareCount     bigint   DEFAULT 0                 NOT NULL COMMENT '分享数',
    viewCount      bigint   DEFAULT 0                 NOT NULL COMMENT '浏览量',
    isFeature      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否精选 0-非精选 1-精选',
    IsDownload     tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许下载：0-禁止下载 1-允许下载',
    recommendScore double   DEFAULT 0                 NOT NULL COMMENT '推荐分数'
)
    COMMENT '图片' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_category
    ON picture (category);

CREATE INDEX idx_introduction
    ON picture (introduction);

CREATE INDEX idx_name
    ON picture (name);

CREATE INDEX idx_picture_recommend_score
    ON picture (recommendScore);

CREATE INDEX idx_reviewStatus
    ON picture (reviewStatus);

CREATE INDEX idx_spaceId
    ON picture (spaceId);

CREATE INDEX idx_tags
    ON picture (tags);

CREATE INDEX idx_userId
    ON picture (userId);

CREATE INDEX idx_viewCount
    ON picture (viewCount);

CREATE TABLE post
(
    id            bigint AUTO_INCREMENT COMMENT '帖子ID'
        PRIMARY KEY,
    userId        bigint                             NOT NULL COMMENT '发帖用户ID',
    title         varchar(100)                       NOT NULL COMMENT '标题',
    content       text                               NOT NULL COMMENT '内容',
    category      varchar(50)                        NULL COMMENT '分类',
    tags          varchar(255)                       NULL COMMENT '标签JSON数组',
    viewCount     bigint   DEFAULT 0                 NULL COMMENT '浏览量',
    likeCount     bigint   DEFAULT 0                 NULL COMMENT '点赞数',
    commentCount  bigint   DEFAULT 0                 NULL COMMENT '评论数',
    status        tinyint  DEFAULT 0                 NULL COMMENT '状态 0-待审核 1-已发布 2-已拒绝',
    reviewMessage varchar(255)                       NULL COMMENT '审核信息',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NULL COMMENT '是否删除',
    shareCount    bigint   DEFAULT 0                 NULL COMMENT '分享数'
)
    COMMENT '论坛帖子表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_category
    ON post (category);

CREATE INDEX idx_shareCount
    ON post (shareCount);

CREATE INDEX idx_status
    ON post (status);

CREATE INDEX idx_userId
    ON post (userId);

CREATE TABLE post_attachment
(
    id         bigint AUTO_INCREMENT COMMENT '附件ID'
        PRIMARY KEY,
    postId     bigint                             NOT NULL COMMENT '帖子ID',
    type       tinyint                            NOT NULL COMMENT '类型 1-图片 2-文件',
    url        varchar(255)                       NOT NULL COMMENT '资源URL',
    name       varchar(100)                       NULL COMMENT '原始文件名',
    size       bigint                             NULL COMMENT '文件大小(字节)',
    position   int                                NULL COMMENT '在文章中的位置',
    marker     varchar(50)                        NULL COMMENT '在文章中的标识符',
    sort       int      DEFAULT 0                 NULL COMMENT '排序号',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NULL COMMENT '是否删除'
)
    COMMENT '帖子附件表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_position
    ON post_attachment (position);

CREATE INDEX idx_postId
    ON post_attachment (postId);

CREATE INDEX idx_type
    ON post_attachment (type);

CREATE TABLE private_chat
(
    id                    bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId                bigint                             NOT NULL COMMENT '用户id',
    targetUserId          bigint                             NOT NULL COMMENT '目标用户id',
    lastMessage           text                               NULL COMMENT '最后一条消息内容',
    lastMessageTime       datetime                           NULL COMMENT '最后一条消息时间',
    userUnreadCount       int      DEFAULT 0                 NULL COMMENT '用户未读消息数',
    targetUserUnreadCount int      DEFAULT 0                 NULL COMMENT '目标用户未读消息数',
    userChatName          varchar(50)                        NULL COMMENT '用户自定义的私聊名称',
    targetUserChatName    varchar(50)                        NULL COMMENT '目标用户自定义的私聊名称',
    chatType              tinyint  DEFAULT 0                 NOT NULL COMMENT '聊天类型：0-私信 1-好友(双向关注)',
    createTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete              tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '私聊表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_type
    ON private_chat (chatType);

CREATE INDEX idx_user_target
    ON private_chat (userId, targetUserId);

CREATE TABLE reminder
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '用户id',
    content     varchar(2048)                      NOT NULL COMMENT '提醒内容',
    remindTime  time                               NOT NULL COMMENT '提醒时间',
    completed   tinyint  DEFAULT 0                 NULL COMMENT '是否完成 0-未完成 1-已完成',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NULL COMMENT '是否删除',
    isStarred   tinyint  DEFAULT 0                 NULL COMMENT '是否收藏 0-未收藏 1-已收藏',
    isImportant tinyint  DEFAULT 0                 NULL COMMENT '是否重要 0-普通 1-重要'
)
    COMMENT '提醒事项表';

CREATE INDEX idx_userId
    ON reminder (userId);

CREATE TABLE share_record
(
    id           bigint AUTO_INCREMENT COMMENT '分享ID'
        PRIMARY KEY,
    userId       bigint                               NOT NULL COMMENT '用户ID',
    targetId     bigint                               NOT NULL COMMENT '被分享内容的ID',
    targetType   tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子',
    targetUserId bigint                               NOT NULL COMMENT '被分享内容所属用户ID',
    isShared     tinyint(1) DEFAULT 1                 NOT NULL COMMENT '是否分享',
    shareTime    datetime   DEFAULT CURRENT_TIMESTAMP NULL COMMENT '分享时间',
    isRead       tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）'
)
    COMMENT '分享记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target
    ON share_record (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON share_record (targetUserId, isRead);

CREATE INDEX idx_userId_isRead
    ON share_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON share_record (userId, targetType);

CREATE TABLE snake_game_record
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '用户ID',
    score       int      DEFAULT 0                 NOT NULL COMMENT '得分',
    foodCount   int      DEFAULT 0                 NOT NULL COMMENT '吃到的食物数量',
    gameTime    int      DEFAULT 0                 NOT NULL COMMENT '游戏时长(秒)',
    snakeLength int      DEFAULT 3                 NOT NULL COMMENT '蛇的长度',
    gameMode    tinyint  DEFAULT 1                 NOT NULL COMMENT '游戏模式：1-经典 2-无墙 3-竞速',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '贪吃蛇游戏记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_gameMode_score
    ON snake_game_record (gameMode ASC, score DESC);

CREATE INDEX idx_userId_score
    ON snake_game_record (userId ASC, score DESC);

CREATE TABLE space
(
    id         bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceName  varchar(128)                       NULL COMMENT '空间名称',
    spaceLevel int      DEFAULT 0                 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大总大小',
    maxCount   bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大数量',
    totalSize  bigint   DEFAULT 0                 NULL COMMENT '当前空间下图片的总大小',
    totalCount bigint   DEFAULT 0                 NULL COMMENT '当前空间下的图片数量',
    userId     bigint                             NOT NULL COMMENT '创建用户 id',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceType  int      DEFAULT 0                 NOT NULL COMMENT '空间类型：0-私有 1-团队'
)
    COMMENT '空间' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_spaceLevel
    ON space (spaceLevel);

CREATE INDEX idx_spaceName
    ON space (spaceName);

CREATE INDEX idx_spaceType
    ON space (spaceType);

CREATE INDEX idx_userId
    ON space (userId);

CREATE TABLE space_user
(
    id         bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceId    bigint                                 NOT NULL COMMENT '空间 id',
    userId     bigint                                 NOT NULL COMMENT '用户 id',
    spaceRole  varchar(128) DEFAULT 'viewer'          NULL COMMENT '空间角色：viewer/editor/admin',
    status     tinyint      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核 1-已通过 2-已拒绝',
    createTime datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_spaceId_userId
        UNIQUE (spaceId, userId)
)
    COMMENT '空间用户关联' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_spaceId
    ON space_user (spaceId);

CREATE INDEX idx_status
    ON space_user (status);

CREATE INDEX idx_userId
    ON space_user (userId);

CREATE TABLE tag
(
    id         bigint AUTO_INCREMENT COMMENT '标签id'
        PRIMARY KEY,
    tagName    varchar(256)                       NOT NULL COMMENT '标签名称',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '标签' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE time_album
(
    id          bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '用户ID',
    loveBoardId bigint                             NOT NULL COMMENT '恋爱板ID',
    albumName   varchar(128)                       NOT NULL COMMENT '相册名称',
    coverUrl    varchar(512)                       NULL COMMENT '相册封面URL',
    description varchar(512)                       NULL COMMENT '相册描述',
    isPublic    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否公开[0-私密，1-公开]',
    password    varchar(32)                        NULL COMMENT '相册访问密码',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '时光相册表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_albumName
    ON time_album (albumName);

CREATE INDEX idx_isPublic
    ON time_album (isPublic);

CREATE INDEX idx_loveBoardId
    ON time_album (loveBoardId);

CREATE INDEX idx_userId
    ON time_album (userId);

CREATE TABLE user
(
    id           bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userAccount  varchar(256)                           NOT NULL COMMENT '账号',
    email        varchar(256)                           NULL COMMENT '用户邮箱',
    userPassword varchar(512)                           NOT NULL COMMENT '密码',
    userName     varchar(256)                           NULL COMMENT '用户昵称',
    userAvatar   varchar(1024)                          NULL COMMENT '用户头像',
    userProfile  varchar(512)                           NULL COMMENT '用户简介',
    userRole     varchar(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user/admin/ban',
    editTime     datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime   datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     tinyint      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_email
        UNIQUE (email),
    CONSTRAINT uk_userAccount
        UNIQUE (userAccount)
)
    COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_email
    ON user (email);

CREATE INDEX idx_userName
    ON user (userName);

CREATE TABLE user_sign_in_record
(
    id         bigint                             NOT NULL COMMENT 'id'
        PRIMARY KEY,
    userId     bigint                             NOT NULL COMMENT '用户id',
    year       int                                NOT NULL COMMENT '年份',
    signInData binary(46)                         NOT NULL COMMENT '签到数据位图(366天/8≈46字节)',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_userId_year
        UNIQUE (userId, year) COMMENT '用户id和年份唯一索引'
)
    COMMENT '用户签到记录表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE userfollows
(
    followId            bigint AUTO_INCREMENT
        PRIMARY KEY,
    followerId          bigint                             NOT NULL COMMENT '关注者的用户 ID',
    followingId         bigint                             NOT NULL COMMENT '被关注者的用户 ID',
    followStatus        tinyint                            NULL COMMENT '关注状态，0 表示取消关注，1 表示关注',
    isMutual            tinyint                            NULL COMMENT '是否为双向关注，0 表示单向，1 表示双向',
    lastInteractionTime datetime                           NULL COMMENT '最后交互时间',
    createTime          datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系创建时间，默认为当前时间',
    editTime            datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系编辑时间，默认为当前时间',
    updateTime          datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '关注关系更新时间，更新时自动更新',
    isDelete            tinyint  DEFAULT 0                 NULL COMMENT '是否删除，0 表示未删除，1 表示已删除'
);

CREATE INDEX idx_followStatus
    ON userfollows (followStatus);

CREATE INDEX idx_followerId
    ON userfollows (followerId);

CREATE INDEX idx_followingId
    ON userfollows (followingId);

CREATE INDEX idx_isDelete
    ON userfollows (isDelete);

CREATE TABLE wei_yan
(
    id          bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    loveBoardId bigint                             NOT NULL COMMENT '恋爱板ID',
    userId      bigint                             NOT NULL COMMENT '发布用户ID',
    likeCount   bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    content     varchar(1024)                      NOT NULL COMMENT '内容',
    type        varchar(32)                        NOT NULL COMMENT '类型',
    source      bigint                             NULL COMMENT '来源标识',
    isPublic    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否公开[0:仅自己可见，1:所有人可见]',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '微言表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_isPublic
    ON wei_yan (isPublic);

CREATE INDEX idx_loveBoardId
    ON wei_yan (loveBoardId);

CREATE INDEX idx_type
    ON wei_yan (type);

CREATE INDEX idx_userId
    ON wei_yan (userId);

