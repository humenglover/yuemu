CREATE TABLE activity
(
    id            bigint AUTO_INCREMENT COMMENT '活动ID'
        PRIMARY KEY,
    userId        bigint                             NOT NULL COMMENT '发布用户ID',
    spaceId       bigint                             NULL COMMENT '所属空间ID',
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

CREATE INDEX idx_spaceId
    ON activity (spaceId);

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
    COMMENT '聊天消息表' COLLATE = utf8mb4_general_ci;

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

CREATE TABLE bug_report
(
    id             bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userId         bigint                             NOT NULL COMMENT '用户id',
    title          varchar(255)                       NOT NULL COMMENT 'bug标题',
    description    text                               NOT NULL COMMENT '详细',
    bugType        tinyint                            NOT NULL COMMENT 'bug类型',
    status         tinyint  DEFAULT 0                 NOT NULL COMMENT '状态',
    screenshotUrls varchar(2048)                      NULL COMMENT '截图URL数组(JSON格式)',
    websiteUrl     varchar(512)                       NULL COMMENT '出现问题的网站URL',
    resolvedTime   datetime                           NULL COMMENT '解决时间',
    resolution     varchar(512)                       NULL COMMENT '解决方案或说明',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT 'bug报告表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_bug_create_time
    ON bug_report (createTime DESC);

CREATE INDEX idx_bug_status
    ON bug_report (status);

CREATE INDEX idx_bug_type
    ON bug_report (bugType);

CREATE INDEX idx_bug_user
    ON bug_report (userId);

CREATE TABLE category
(
    id           bigint AUTO_INCREMENT COMMENT '分类id'
        PRIMARY KEY,
    categoryName varchar(256)                       NOT NULL COMMENT '分类名称',
    type         tinyint  DEFAULT 0                 NOT NULL COMMENT '分类类型：0-图片分类 1-帖子分类',
    createTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '分类编辑时间',
    updateTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '分类更新时间',
    isDelete     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_categoryName
        UNIQUE (categoryName)
)
    COMMENT '分类' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chat_message
(
    id              bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    senderId        bigint                                NOT NULL COMMENT '发送者id',
    receiverId      bigint                                NULL COMMENT '接收者id',
    pictureId       bigint                                NULL COMMENT '图片id',
    content         text                                  NOT NULL COMMENT '消息内容',
    type            tinyint     DEFAULT 1                 NOT NULL COMMENT '消息类型 1-私聊 2-图片聊天室',
    status          tinyint     DEFAULT 0                 NOT NULL COMMENT '状态 0-未读 1-已读',
    replyId         bigint                                NULL COMMENT '回复的消息id',
    rootId          bigint                                NULL COMMENT '会话根消息id',
    createTime      datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        tinyint     DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceId         bigint                                NULL COMMENT '空间id',
    privateChatId   bigint                                NULL COMMENT '私聊ID',
    messageType     varchar(32) DEFAULT 'text'            NOT NULL COMMENT '消息类型：text-文本 image-图片 audio-音频 video-视频',
    messageUrl      varchar(1024)                         NULL COMMENT '消息资源地址',
    messageSize     bigint      DEFAULT 0                 NULL COMMENT '消息资源大小(字节)',
    messageLocation varchar(256)                          NULL COMMENT '消息发送位置'
)
    COMMENT '聊天消息表' COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_messageLocation
    ON chat_message (messageLocation);

CREATE INDEX idx_messageType
    ON chat_message (messageType);

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
    isRead          tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）',
    rootCommentId   bigint                               NULL COMMENT '最顶级评论ID，用于标识评论链的根节点',
    location        varchar(128)                         NULL COMMENT '评论位置（省份）'
)
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_admin_delete_time_type
    ON comments (isDelete ASC, createTime DESC, targetType ASC);

CREATE INDEX idx_parent_delete_time
    ON comments (parentCommentId ASC, isDelete ASC, createTime DESC);

CREATE INDEX idx_root_comment
    ON comments (rootCommentId);

CREATE INDEX idx_root_comment_delete_time
    ON comments (rootCommentId ASC, isDelete ASC, createTime DESC);

CREATE INDEX idx_target
    ON comments (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON comments (targetUserId, isRead);

CREATE INDEX idx_targetUserId_isRead_delete_time
    ON comments (targetUserId ASC, isRead ASC, isDelete ASC, createTime DESC);

CREATE INDEX idx_target_parent_delete_time
    ON comments (targetId ASC, targetType ASC, parentCommentId ASC, isDelete ASC, createTime DESC);

CREATE INDEX idx_user_delete_time
    ON comments (userId ASC, isDelete ASC, createTime DESC);

CREATE TABLE favorite_record
(
    id           bigint AUTO_INCREMENT COMMENT '收藏记录ID'
        PRIMARY KEY,
    userId       bigint                               NOT NULL COMMENT '用户ID',
    targetId     bigint                               NOT NULL COMMENT '被收藏内容的ID',
    targetType   tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId bigint                               NOT NULL COMMENT '被收藏内容所属用户ID',
    isFavorite   tinyint(1)                           NOT NULL COMMENT '是否收藏',
    favoriteTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '收藏时间',
    isRead       tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）',
    createTime   datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     tinyint    DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_user_target_favorite
        UNIQUE (userId, targetId, targetType)
)
    COMMENT '收藏记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_targetUserId_isRead
    ON favorite_record (targetUserId, isRead);

CREATE INDEX idx_target_favorite
    ON favorite_record (targetId, targetType);

CREATE INDEX idx_userId_isRead
    ON favorite_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON favorite_record (userId, targetType);

CREATE TABLE friend_link
(
    id            bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    siteName      varchar(128)                       NOT NULL COMMENT '网站名称',
    siteUrl       varchar(512)                       NOT NULL COMMENT '网站链接',
    siteLogo      varchar(512)                       NULL COMMENT '网站logo',
    siteDesc      varchar(512)                       NULL COMMENT '网站描述',
    ownerName     varchar(64)                        NOT NULL COMMENT '站长名称',
    ownerContact  varchar(128)                       NOT NULL COMMENT '站长联系方式',
    userId        bigint                             NOT NULL COMMENT '申请用户id',
    siteType      varchar(32)                        NULL COMMENT '网站类型',
    status        tinyint  DEFAULT 0                 NOT NULL COMMENT '审核状态 0-待审核 1-通过 2-拒绝',
    reviewMessage varchar(256)                       NULL COMMENT '审核信息',
    viewCount     bigint   DEFAULT 0                 NOT NULL COMMENT '浏览量',
    clickCount    bigint   DEFAULT 0                 NOT NULL COMMENT '点击量',
    weight        int      DEFAULT 0                 NOT NULL COMMENT '排序权重',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '友情链接表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_status
    ON friend_link (status);

CREATE INDEX idx_userId
    ON friend_link (userId);

CREATE INDEX idx_weight
    ON friend_link (weight DESC);

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
    keyword        varchar(512)                       NOT NULL COMMENT '搜索关键词',
    type           varchar(32)                        NOT NULL COMMENT '搜索类型',
    count          bigint   DEFAULT 0                 NOT NULL COMMENT '搜索次数',
    lastUpdateTime datetime                           NOT NULL COMMENT '最后更新时间',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    realTimeCount  bigint   DEFAULT 0                 NOT NULL COMMENT '24h内实时搜索量',
    trend          double   DEFAULT 0                 NOT NULL COMMENT '热度趋势（斜率）',
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
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    viewCount      bigint   DEFAULT 0                 NOT NULL COMMENT '浏览量'
)
    COMMENT '恋爱画板' COLLATE = utf8mb4_unicode_ci;

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
    spaceId        bigint                             NULL COMMENT '空间 id（为空表示公共空间,-1表示其他）',
    picColor       varchar(16)                        NULL COMMENT '图片主色调',
    commentCount   bigint   DEFAULT 0                 NOT NULL COMMENT '评论数',
    likeCount      bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    shareCount     bigint   DEFAULT 0                 NOT NULL COMMENT '分享数',
    viewCount      bigint   DEFAULT 0                 NOT NULL COMMENT '浏览量',
    isFeature      tinyint  DEFAULT 0                 NULL,
    isDraft        tinyint  DEFAULT 1                 NOT NULL COMMENT '是否为草稿：0-非草稿 1-草稿',
    IsDownload     tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许下载：0-禁止下载 1-允许下载',
    recommendScore double   DEFAULT 0                 NOT NULL COMMENT '推荐分数',
    hotScore       double   DEFAULT 0                 NOT NULL COMMENT '热榜分数',
    favoriteCount  bigint   DEFAULT 0                 NULL COMMENT '收藏数',
    allowCollect   tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许收藏：1-允许、0-禁止',
    allowLike      tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许点赞：1-允许、0-禁止',
    allowComment   tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许评论：1-允许、0-禁止',
    allowShare     tinyint  DEFAULT 1                 NOT NULL COMMENT '是否允许分享：1-允许、0-禁止'
)
    COMMENT '图片' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_category
    ON picture (category);

CREATE INDEX idx_introduction
    ON picture (introduction);

CREATE INDEX idx_isFeature_isDraft
    ON picture (isFeature, isDraft);

CREATE INDEX idx_name
    ON picture (name);

CREATE INDEX idx_picture_create_time
    ON picture (createTime DESC);

CREATE INDEX idx_picture_draft_time
    ON picture (isDraft ASC, createTime DESC);

CREATE INDEX idx_picture_hot_score
    ON picture (hotScore DESC, createTime DESC);

CREATE INDEX idx_picture_like_count
    ON picture (likeCount DESC, createTime DESC);

CREATE INDEX idx_picture_public_library
    ON picture (isDraft ASC, reviewStatus ASC, spaceId ASC, createTime DESC);

CREATE INDEX idx_picture_recommend_score
    ON picture (recommendScore);

CREATE INDEX idx_picture_recommend_score_opt
    ON picture (recommendScore DESC, createTime DESC);

CREATE INDEX idx_picture_review_time
    ON picture (reviewStatus ASC, createTime DESC);

CREATE INDEX idx_picture_space_time
    ON picture (spaceId ASC, isDraft ASC, createTime DESC);

CREATE INDEX idx_picture_user_time
    ON picture (userId ASC, isDraft ASC, createTime DESC);

CREATE INDEX idx_picture_view_count
    ON picture (viewCount DESC, createTime DESC);

CREATE INDEX idx_reviewStatus
    ON picture (reviewStatus);

CREATE INDEX idx_reviewStatus_isDraft
    ON picture (reviewStatus, isDraft);

CREATE INDEX idx_spaceId_isDraft
    ON picture (spaceId, isDraft);

CREATE INDEX idx_tags
    ON picture (tags);

CREATE INDEX idx_userId_isDraft
    ON picture (userId, isDraft);

CREATE INDEX idx_viewCount
    ON picture (viewCount);

CREATE TABLE post
(
    id            bigint       DEFAULT 0  NULL,
    userId        bigint       DEFAULT 0  NULL,
    title         varchar(100) DEFAULT '' NULL,
    content       text                    NULL,
    category      varchar(50)  DEFAULT '' NULL,
    tags          varchar(255) DEFAULT '' NULL,
    viewCount     bigint       DEFAULT 0  NULL,
    likeCount     bigint       DEFAULT 0  NULL,
    commentCount  bigint       DEFAULT 0  NULL,
    status        tinyint      DEFAULT 0  NULL,
    reviewMessage varchar(255) DEFAULT '' NULL,
    createTime    datetime                NULL,
    updateTime    datetime                NULL,
    isDelete      tinyint      DEFAULT 0  NULL,
    shareCount    bigint       DEFAULT 0  NULL,
    hotScore      double       DEFAULT 0  NULL,
    favoriteCount bigint       DEFAULT 0  NULL,
    coverUrl      varchar(512) DEFAULT '' NULL,
    isDraft       tinyint      DEFAULT 0  NULL,
    allowCollect  tinyint      DEFAULT 1  NOT NULL COMMENT '是否允许收藏：1-允许、0-禁止',
    allowLike     tinyint      DEFAULT 1  NOT NULL COMMENT '是否允许点赞：1-允许、0-禁止',
    allowComment  tinyint      DEFAULT 1  NOT NULL COMMENT '是否允许评论：1-允许、0-禁止',
    allowShare    tinyint      DEFAULT 1  NOT NULL COMMENT '是否允许分享：1-允许、0-禁止'
);

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

CREATE TABLE rag_session_message
(
    id          bigint AUTO_INCREMENT COMMENT '消息ID'
        PRIMARY KEY,
    sessionId   bigint                             NOT NULL COMMENT '关联会话ID',
    userId      bigint                             NOT NULL COMMENT '发送用户ID',
    messageType tinyint                            NOT NULL COMMENT '消息类型 1-用户提问 2-AI回答',
    content     text                               NOT NULL COMMENT '消息内容',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除 0-未删 1-已删'
)
    COMMENT '智能客服会话消息表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_createTime
    ON rag_session_message (createTime);

CREATE INDEX idx_sessionId_isDelete
    ON rag_session_message (sessionId, isDelete);

CREATE TABLE rag_user_session
(
    id          bigint AUTO_INCREMENT COMMENT '会话ID'
        PRIMARY KEY,
    userId      bigint                             NOT NULL COMMENT '关联用户ID',
    sessionName varchar(100)                       NOT NULL COMMENT '会话名称（默认：新会话+时间戳）',
    createTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后消息时间',
    isActive    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否为当前活跃会话 0-否 1-是',
    isDelete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除 0-未删 1-已删',
    active_flag tinyint AS ((CASE WHEN ((`isActive` = 1) AND (`isDelete` = 0)) THEN 1 ELSE NULL END)) STORED,
    CONSTRAINT uk_user_active_session
        UNIQUE (userId, active_flag)
)
    COMMENT '智能客服用户会话表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_updateTime
    ON rag_user_session (updateTime);

CREATE INDEX idx_userId_isDelete
    ON rag_user_session (userId, isDelete);

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
    COMMENT '提醒事项表' COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_userId
    ON reminder (userId);

CREATE TABLE report
(
    id             bigint AUTO_INCREMENT COMMENT '举报ID'
        PRIMARY KEY,
    userId         bigint                             NOT NULL COMMENT '举报人ID',
    targetId       bigint                             NOT NULL COMMENT '被举报内容ID',
    targetType     tinyint                            NOT NULL COMMENT '举报内容类型：1-图片 2-帖子 3-评论 4-用户 5-其他',
    reportType     tinyint                            NOT NULL COMMENT '举报类型：1-垃圾广告 2-违规内容 3-有害信息 4-人身攻击 5-侵犯隐私 6-版权问题 7-其他',
    reason         varchar(512)                       NOT NULL COMMENT '举报原因',
    screenshotUrls varchar(2048)                      NULL COMMENT '举报截图URL（JSON数组）',
    status         tinyint  DEFAULT 0                 NOT NULL COMMENT '处理状态：0-待处理 1-已处理 2-驳回',
    handlerId      bigint                             NULL COMMENT '处理人ID',
    handleResult   varchar(512)                       NULL COMMENT '处理结果',
    handleTime     datetime                           NULL COMMENT '处理时间',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '举报表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_createTime
    ON report (createTime);

CREATE INDEX idx_status
    ON report (status);

CREATE INDEX idx_targetId_targetType
    ON report (targetId, targetType);

CREATE INDEX idx_userId
    ON report (userId);

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
    id            bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceName     varchar(128)                       NULL COMMENT '空间名称',
    spaceLevel    int      DEFAULT 0                 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize       bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大总大小',
    maxCount      bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大数量',
    totalSize     bigint   DEFAULT 0                 NULL COMMENT '当前空间下图片的总大小',
    totalCount    bigint   DEFAULT 0                 NULL COMMENT '当前空间下的图片数量',
    userId        bigint                             NOT NULL COMMENT '创建用户 id',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime      datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceType     int      DEFAULT 0                 NOT NULL COMMENT '空间类型：0-私有 1-团队',
    spaceDesc     varchar(512)                       NULL COMMENT '空间简介',
    spaceCover    varchar(512)                       NULL COMMENT '空间封面图',
    isRecommended tinyint  DEFAULT 0                 NOT NULL COMMENT '是否推荐 0-否 1-是'
)
    COMMENT '空间' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_spaceDesc
    ON space (spaceDesc);

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
    id            bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceId       bigint                                 NOT NULL COMMENT '空间 id',
    userId        bigint                                 NOT NULL COMMENT '用户 id',
    spaceRole     varchar(128) DEFAULT 'viewer'          NULL COMMENT '空间角色：viewer/editor/admin',
    status        tinyint      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核 1-已通过 2-已拒绝',
    createTime    datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isRecommended tinyint      DEFAULT 0                 NOT NULL COMMENT '是否为推荐成员：0-否 1-是',
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

CREATE TABLE t_system_notify
(
    id             bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    createTime     datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    operatorId     varchar(50)                           NULL COMMENT '操作人ID：管理员操作填管理员ID/系统自动操作填"system"/NULL=无',
    operatorType   varchar(20)                           NULL COMMENT '操作人类型：ADMIN(管理员)/SYSTEM(系统)/NULL=无',
    notifyType     varchar(30)                           NOT NULL COMMENT '通知类型：ADMIN_ANNOUNCE(管理员公告)/POST_SELECTED(帖子精选)/POST_DELETED(帖子删除)/POST_UPDATED(帖子修改)/ACCOUNT_CHANGED(账号变更)/SYSTEM_ALERT(系统告警)',
    senderType     varchar(20)                           NOT NULL COMMENT '发送者类型：ADMIN(管理员)/SYSTEM(系统)',
    senderId       varchar(50)                           NOT NULL COMMENT '发送者ID：ADMIN=管理员用户ID/SYSTEM=固定值"system"',
    receiverType   varchar(20)                           NOT NULL COMMENT '接收者类型：ALL_USER(全体用户)/SPECIFIC_USER(指定用户)/ROLE(按角色)',
    receiverId     varchar(50)                           NULL COMMENT '接收者ID：ALL_USER=NULL/SPECIFIC_USER=用户ID/ROLE=角色编码（如USER/VIP/ADMIN）',
    title          varchar(100)                          NOT NULL COMMENT '通知标题（如：系统公告、您的帖子已精选）',
    content        text                                  NOT NULL COMMENT '通知详情（支持富文本）',
    notifyIcon     varchar(50) DEFAULT 'default'         NOT NULL COMMENT '通知图标标识（用于前端差异化展示，如：announce/selected/alert）',
    relatedBizType varchar(30)                           NULL COMMENT '关联业务类型：POST(帖子)/ACCOUNT(账号)/COMMENT(评论)/NULL(无关联)',
    relatedBizId   varchar(50)                           NULL COMMENT '关联业务ID：帖子ID/账号ID/评论ID（用于前端跳转至对应页面）',
    readStatus     tinyint     DEFAULT 0                 NOT NULL COMMENT '阅读状态[0:未读, 1:已读]',
    readTime       datetime                              NULL COMMENT '阅读时间',
    isGlobal       tinyint     DEFAULT 0                 NOT NULL COMMENT '是否全局通知[0:否, 1:是（全员可见，如系统公告）]',
    expireTime     datetime                              NULL COMMENT '通知过期时间（NULL=永久有效）',
    isEnabled      tinyint     DEFAULT 1                 NOT NULL COMMENT '是否有效[0:无效（如误发通知）, 1:有效]',
    isDelete       tinyint     DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '系统通知表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_create_time
    ON t_system_notify (createTime DESC)
    COMMENT '按创建时间排序查询';

CREATE INDEX idx_notify_type
    ON t_system_notify (notifyType)
    COMMENT '按通知类型筛选';

CREATE INDEX idx_read_status
    ON t_system_notify (receiverId, readStatus)
    COMMENT '查询用户未读通知';

CREATE INDEX idx_receiver
    ON t_system_notify (receiverType, receiverId)
    COMMENT '查询指定用户/角色的通知';

CREATE TABLE tag
(
    id         bigint AUTO_INCREMENT COMMENT '标签id'
        PRIMARY KEY,
    tagName    varchar(256)                       NOT NULL COMMENT '标签名称',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_tagName
        UNIQUE (tagName)
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
    id                    bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userAccount           varchar(256)                           NOT NULL COMMENT '账号',
    email                 varchar(256)                           NULL COMMENT '用户邮箱',
    userPassword          varchar(512)                           NOT NULL COMMENT '密码',
    userName              varchar(256)                           NULL COMMENT '用户昵称',
    userAvatar            varchar(1024)                          NULL COMMENT '用户头像',
    userProfile           varchar(512)                           NULL COMMENT '用户简介',
    userRole              varchar(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user/admin',
    editTime              datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime            datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime            datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete              tinyint      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    gender                varchar(20)                            NULL COMMENT '用户性别',
    region                varchar(128)                           NULL COMMENT '所在地区',
    birthday              date                                   NULL COMMENT '生日',
    userTags              varchar(512)                           NULL COMMENT '个人标签',
    homepageBg            varchar(1024)                          NULL COMMENT '主页背景图URL',
    personalSign          varchar(256)                           NULL COMMENT '个性签名',
    themePreference       varchar(20)  DEFAULT 'light'           NULL COMMENT '主题偏好',
    visibilitySetting     varchar(20)  DEFAULT 'public'          NULL COMMENT '内容可见性设置',
    interestField         varchar(512)                           NULL COMMENT '兴趣领域',
    lastActiveTime        datetime                               NULL COMMENT '最后活跃时间',
    allowPrivateChat      tinyint      DEFAULT 1                 NOT NULL COMMENT '是否允许私聊：1-允许、0-禁止',
    allowFollow           tinyint      DEFAULT 1                 NOT NULL COMMENT '是否允许被关注：1-允许、0-禁止',
    showFollowList        tinyint      DEFAULT 1                 NOT NULL COMMENT '是否展示关注列表：1-展示、0-隐藏',
    showFansList          tinyint      DEFAULT 1                 NOT NULL COMMENT '是否展示粉丝列表：1-展示、0-隐藏',
    allowMultiDeviceLogin tinyint      DEFAULT 1                 NOT NULL COMMENT '是否允许多设备登录：1-允许、0-禁止',
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

CREATE TABLE user_search_record
(
    id         bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId     bigint                             NOT NULL COMMENT '用户ID',
    keyword    varchar(512)                       NOT NULL COMMENT '搜索关键词',
    type       varchar(32)                        NOT NULL COMMENT '搜索类型',
    searchTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '搜索时间',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '用户搜索记录' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_userId_searchTime
    ON user_search_record (userId ASC, searchTime DESC);

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

CREATE TABLE user_system_notify_read
(
    id             bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userId         bigint                             NOT NULL COMMENT '用户ID',
    systemNotifyId bigint                             NOT NULL COMMENT '系统通知ID',
    readStatus     tinyint  DEFAULT 0                 NOT NULL COMMENT '阅读状态[0:未读, 1:已读]',
    readTime       datetime                           NULL COMMENT '阅读时间',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_user_notify
        UNIQUE (userId, systemNotifyId) COMMENT '用户通知唯一索引'
)
    COMMENT '用户系统通知阅读状态表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_create_time
    ON user_system_notify_read (createTime DESC)
    COMMENT '按创建时间排序查询';

CREATE INDEX idx_read_status
    ON user_system_notify_read (userId, readStatus)
    COMMENT '用户阅读状态索引';

CREATE INDEX idx_user_notify
    ON user_system_notify_read (userId, systemNotifyId)
    COMMENT '用户通知索引';

CREATE TABLE userfollows
(
    followId            bigint AUTO_INCREMENT
        PRIMARY KEY,
    followerId          bigint                             NOT NULL COMMENT '关注者的用户 ID',
    followingId         bigint                             NOT NULL COMMENT '被关注者的用户 ID',
    followStatus        tinyint                            NOT NULL COMMENT '关注状态，0 表示取消关注，1 表示关注',
    isMutual            tinyint                            NOT NULL COMMENT '是否为双向关注，0 表示单向，1 表示双向',
    lastInteractionTime datetime                           NULL COMMENT '最后交互时间',
    createTime          datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '关注关系创建时间，默认为当前时间',
    editTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '关注关系编辑时间，默认为当前时间',
    updateTime          datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '关注关系更新时间，更新时自动更新',
    isDelete            tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除，0 表示未删除，1 表示已删除'
)
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_followStatus
    ON userfollows (followStatus);

CREATE INDEX idx_followerId
    ON userfollows (followerId);

CREATE INDEX idx_followingId
    ON userfollows (followingId);

CREATE INDEX idx_isDelete
    ON userfollows (isDelete);

CREATE TABLE view_record
(
    id           bigint AUTO_INCREMENT COMMENT '浏览记录ID'
        PRIMARY KEY,
    userId       bigint                             NOT NULL COMMENT '用户ID',
    targetId     bigint                             NOT NULL COMMENT '被浏览内容的ID',
    targetType   tinyint                            NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间 4-用户',
    viewDuration int      DEFAULT 0                 NULL COMMENT '浏览时长(秒)',
    createTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_user_target_view
        UNIQUE (userId, targetId, targetType)
)
    COMMENT '浏览记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target_view
    ON view_record (targetId, targetType);

CREATE INDEX idx_userId_viewType
    ON view_record (userId, targetType);

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

CREATE DEFINER = root@localhost VIEW v_unfixed_posts AS
select `yuemu_picture`.`post`.`id` AS `id`,`yuemu_picture`.`post`.`userId` AS `userId`,`yuemu_picture`.`post`.`title` AS `title`,((length(`yuemu_picture`.`post`.`content`) - length(replace(`yuemu_picture`.`post`.`content`,'{img-',''))) / 5) AS `unfixed_count` from `yuemu_picture`.`post` where ((`yuemu_picture`.`post`.`content` like '%{img-%') and (`yuemu_picture`.`post`.`isDelete` = 0) and ((`yuemu_picture`.`post`.`isDraft` = 0) or (`yuemu_picture`.`post`.`isDraft` is null)) and (`yuemu_picture`.`post`.`status` = 1));

CREATE
    DEFINER = root@localhost PROCEDURE FinalFixImageTags()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE current_post_id BIGINT;
    DECLARE current_content TEXT;
    DECLARE attachment_cursor_result TEXT DEFAULT '';

    -- 声明游标
    DECLARE post_cursor CURSOR FOR
        SELECT id, content
        FROM post
        WHERE isDelete = 0
          AND (isDraft = 0 OR isDraft IS NULL)
          AND status = 1
          AND content LIKE '%{img-%'; -- 只处理包含图片标记的内容

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 打开游标
    OPEN post_cursor;

    post_loop: LOOP
        FETCH post_cursor INTO current_post_id, current_content;

        IF done THEN
            LEAVE post_loop;
        END IF;

        -- 获取当前帖子的附件列表并按位置排序
        SET @attachment_urls = (
            SELECT GROUP_CONCAT(url ORDER BY COALESCE(position, 999999), createTime SEPARATOR '|')
            FROM post_attachment
            WHERE postId = current_post_id
              AND isDelete = 0
              AND type = 1
        );

        -- 如果找到了附件URLs，则进行替换
        IF @attachment_urls IS NOT NULL AND LENGTH(@attachment_urls) > 0 THEN
            SET @updated_content = current_content;

            -- 分割附件URLs并逐个替换标记
            SET @url_count = CHAR_LENGTH(@attachment_urls) - CHAR_LENGTH(REPLACE(@attachment_urls, '|', '')) + 1;
            SET @counter = 1;

            WHILE @counter <= @url_count DO
                    SET @current_url = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(@attachment_urls, '|', @counter), '|', -1));

                    -- 替换标记为Markdown图片语法
                    SET @pattern = CONCAT('{img-', @counter, '}');
                    SET @replacement = CONCAT('![](', @current_url, ')'); -- Markdown图片语法
                    SET @updated_content = REPLACE(@updated_content, @pattern, @replacement);

                    SET @counter = @counter + 1;
                END WHILE;

            -- 检查是否有剩余的图片标记未被替换
            SET @remaining_count = (CHAR_LENGTH(@updated_content) - CHAR_LENGTH(REPLACE(@updated_content, '{img-', ''))) / 5;

            -- 如果还有剩余的标记，可能是附件数量少于标记数量，尝试处理多余的标记
            IF @remaining_count > 0 THEN
                SET @extra_counter = @url_count + 1;
                WHILE @remaining_count > 0 DO
                        SET @extra_pattern = CONCAT('{img-', @extra_counter, '}');
                        -- 如果找不到对应附件，替换为空字符串或占位符
                        SET @updated_content = REPLACE(@updated_content, @extra_pattern, '');
                        SET @extra_counter = @extra_counter + 1;
                        SET @remaining_count = (CHAR_LENGTH(@updated_content) - CHAR_LENGTH(REPLACE(@updated_content, '{img-', ''))) / 5;
                    END WHILE;
            END IF;

            -- 更新帖子内容
            UPDATE post SET content = @updated_content WHERE id = current_post_id;
            SELECT CONCAT('已更新帖子 ', current_post_id, '，替换图片标记') AS result;
        ELSE
            -- 没有附件但有图片标记，可能是异常数据，替换为占位符或清空标记
            -- 注意：REGEXP_REPLACE 在较老版本的 MySQL 中不可用，所以我们使用嵌套的REPLACE
            -- 逐一移除常见的图片标记
            SET @cleaned_content = current_content;
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-1}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-2}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-3}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-4}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-5}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-6}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-7}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-8}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-9}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-10}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-11}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-12}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-13}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-14}', '');
            SET @cleaned_content = REPLACE(@cleaned_content, '{img-15}', '');

            UPDATE post SET content = @cleaned_content WHERE id = current_post_id;
            SELECT CONCAT('已清理帖子 ', current_post_id, ' 的无效图片标记') AS result;
        END IF;

    END LOOP;

    -- 关闭游标
    CLOSE post_cursor;
END;

CREATE
    DEFINER = root@localhost PROCEDURE UpdatePostContentWithRealImageUrls()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE current_post_id BIGINT;
    DECLARE current_content TEXT;
    DECLARE current_user_id BIGINT;

    -- 声明游标
    DECLARE post_cursor CURSOR FOR
        SELECT id, content, userId FROM post
        WHERE isDelete = 0
          AND (isDraft = 0 OR isDraft IS NULL)
          AND status = 1
          AND content LIKE '%{img-%'; -- 只处理包含图片标记的内容

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 打开游标
    OPEN post_cursor;

    post_loop: LOOP
        FETCH post_cursor INTO current_post_id, current_content, current_user_id;

        IF done THEN
            LEAVE post_loop;
        END IF;

        -- 获取当前帖子的附件列表并按位置排序
        SET @attachment_urls = (SELECT GROUP_CONCAT(url ORDER BY COALESCE(position, 999999), createTime SEPARATOR '|')
                                FROM post_attachment
                                WHERE postId = current_post_id
                                  AND isDelete = 0
                                  AND type = 1); -- 1表示图片类型

        -- 如果找到了附件URLs，则进行替换
        IF @attachment_urls IS NOT NULL AND LENGTH(@attachment_urls) > 0 THEN
            SET @updated_content = current_content;

            -- 分割附件URLs并逐个替换标记
            SET @pos = 1;
            SET @url_count = CHAR_LENGTH(@attachment_urls) - CHAR_LENGTH(REPLACE(@attachment_urls, '|', '')) + 1;
            SET @counter = 1;

            WHILE @counter <= @url_count DO
                    SET @current_url = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(@attachment_urls, '|', @counter), '|', -1));
                    SET @marker = CONCAT('{img-', @counter, '}');

                    -- 替换标记为Markdown图片语法
                    SET @pattern = CONCAT('{img-', @counter, '}');
                    SET @replacement = CONCAT('![](', @current_url, ')'); -- Markdown图片语法
                    SET @updated_content = REPLACE(@updated_content, @pattern, @replacement);

                    SET @counter = @counter + 1;
                END WHILE;

            -- 更新帖子内容
            UPDATE post SET content = @updated_content WHERE id = current_post_id;
        END IF;

    END LOOP;

    -- 关闭游标
    CLOSE post_cursor;
END;

CREATE TABLE knowledge_file
(
    id           bigint AUTO_INCREMENT COMMENT '知识库文件ID'
        PRIMARY KEY,
    originalName varchar(255)                       NOT NULL COMMENT '原始文件名',
    storedName   varchar(255)                       NOT NULL COMMENT '存储文件名',
    fileUrl      varchar(512)                       NOT NULL COMMENT '文件访问URL',
    fileSize     bigint                             NOT NULL COMMENT '文件大小(字节)',
    fileType     varchar(50)                        NOT NULL COMMENT '文件类型(pdf,txt,docx,md等)',
    uploadTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '上传时间',
    userId       bigint                             NOT NULL COMMENT '上传用户ID',
    md5Hash      varchar(32)                        NOT NULL COMMENT '文件MD5哈希值',
    vectorCount  int      DEFAULT 0                 NOT NULL COMMENT '向量数量',
    createTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_originalName (originalName),
    INDEX idx_storedName (storedName),
    INDEX idx_userId (userId),
    INDEX idx_md5Hash (md5Hash),
    INDEX idx_createTime (createTime),
    INDEX idx_isDelete (isDelete)
) COMMENT = '知识库文件表' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE picture ADD COLUMN aiLabels VARCHAR(512) NULL COMMENT 'AI 自动识别标签';

-- 2026-03-05 微信登录补丁：为用户表添加公众号 OpenId 字段
ALTER TABLE user ADD COLUMN mpOpenId VARCHAR(256) NULL COMMENT '公众号 OpenId';
ALTER TABLE user ADD UNIQUE INDEX uk_mpOpenId (mpOpenId);
CREATE INDEX idx_mpOpenId ON user (mpOpenId);


-- 智能客服会话摘要表
CREATE TABLE IF NOT EXISTS rag_session_summary
(
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    sessionId   BIGINT                             NOT NULL COMMENT '会话id',
    userId      BIGINT                             NOT NULL COMMENT '用户id',
    content     TEXT                               NOT NULL COMMENT '摘要内容',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sessionId (sessionId),
    INDEX idx_userId (userId)
) COMMENT '智能客服会话摘要表' COLLATE = utf8mb4_unicode_ci;
