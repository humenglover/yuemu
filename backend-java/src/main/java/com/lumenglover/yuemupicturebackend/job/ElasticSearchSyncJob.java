package com.lumenglover.yuemupicturebackend.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.mapper.PictureMapper;
import com.lumenglover.yuemupicturebackend.mapper.PostMapper;
import com.lumenglover.yuemupicturebackend.mapper.SpaceMapper;
import com.lumenglover.yuemupicturebackend.mapper.UserMapper;
import com.lumenglover.yuemupicturebackend.model.entity.Picture;
import com.lumenglover.yuemupicturebackend.model.entity.Post;
import com.lumenglover.yuemupicturebackend.model.entity.Space;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsPicture;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsSpace;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsUser;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsPost;
import com.lumenglover.yuemupicturebackend.utils.EmailSenderUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.PreDestroy;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * ElasticSearch 数据同步任务
 * 负责MySQL与ES之间的全量/增量数据同步、数据一致性检查、异常告警等功能
 *
 * @author 开发者
 * @date 2026-01
 */
@Component
@Slf4j
public class ElasticSearchSyncJob implements CommandLineRunner {

    // ===================== 常量定义区 =====================
    /**
     * 增量同步时间窗口（分钟）
     */
    private static final long INCREMENTAL_SYNC_INTERVAL_MINUTES = 5L;

    /**
     * 增量同步固定延迟（毫秒）= 5分钟
     */
    private static final long INCREMENTAL_SYNC_DELAY_MILLIS = INCREMENTAL_SYNC_INTERVAL_MINUTES * 60 * 1000;

    /**
     * ES索引名称常量
     */
    private static final String INDEX_NAME_PICTURE = "picture";
    private static final String INDEX_NAME_USER = "user";
    private static final String INDEX_NAME_POST = "post";
    private static final String INDEX_NAME_SPACE = "space";

    /**
     * 数据一致性检查阈值：数量差异超过该值触发全量同步
     */
    private static final int CONSISTENCY_CHECK_THRESHOLD = 10;

    /**
     * ES批量操作最大批次大小
     */
    private static final int MAX_BATCH_SIZE = 500;

    /**
     * 批次间休眠时间（毫秒）
     */
    private static final long BATCH_SLEEP_TIME = 100L;

    /**
     * 重试最大等待时间（毫秒）
     */
    private static final long MAX_RETRY_WAIT_TIME = 30000L;

    // ===================== 配置项（可通过配置文件动态调整） =====================
    @Value("${spring.mail.admin:admin@example.com}")
    private String adminEmail;

    @Value("${elasticsearch.sync.batch.size:1000}")
    private int batchSize;

    @Value("${elasticsearch.sync.alert.interval:1800000}") // 30分钟（毫秒）
    private long alertInterval;

    @Value("${elasticsearch.sync.retry.times:3}")
    private int retryTimes;

    @Value("${elasticsearch.sync.retry.interval:5000}") // 5秒（毫秒）
    private long retryInterval;

    @Value("${elasticsearch.sync.picture.enable:true}")
    private boolean pictureSyncEnabled;

    @Value("${elasticsearch.sync.user.enable:true}")
    private boolean userSyncEnabled;

    @Value("${elasticsearch.sync.post.enable:true}")
    private boolean postSyncEnabled;

    @Value("${elasticsearch.sync.space.enable:true}")
    private boolean spaceSyncEnabled;

    @Value("${elasticsearch.sync.incremental.random.delay.max:30}") // 增量同步最大随机延迟（秒）
    private int incrementalRandomDelayMax;

    // ===================== 资源注入区 =====================
    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private EmailSenderUtil emailSenderUtil;

    // ===================== 状态与统计区 =====================
    /**
     * 记录最近一次发送告警邮件的时间，避免短时间内发送过多邮件
     */
    private volatile long lastAlertTime = 0L;

    /**
     * 同步成功数统计（按数据类型分类）
     */
    private final Map<String, AtomicInteger> syncSuccessCount = new ConcurrentHashMap<>();

    /**
     * 同步失败数统计（按数据类型分类）
     */
    private final Map<String, AtomicInteger> syncFailureCount = new ConcurrentHashMap<>();

    /**
     * 告警失败计数器（按告警类型分类）
     */
    private final Map<String, AtomicInteger> alertFailureCount = new ConcurrentHashMap<>();

    /**
     * 告警失败阈值
     */
    private static final int ALERT_FAILURE_THRESHOLD = 3;

    /**
     * 同步任务线程池
     */
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(4);

    // ===================== 定时任务入口 =====================

    /**
     * 每日数据一致性检查（凌晨2点执行）
     * 功能：清理已删除数据 + 检查并修复ES与MySQL数据一致性
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyConsistencyCheck() {
        try {
            log.info("===== 开始ES与MySQL数据一致性检查 =====");

            // 清理各类已删除/无效数据
            cleanDeletedPictures();
            cleanDraftPictures();
            cleanDeletedUsers();
            cleanDeletedSpaces();

            // 检查并修复数据一致性
            checkAndFixDataConsistency();

            log.info("===== ES与MySQL数据一致性检查完成 =====");
        } catch (Exception e) {
            String errorMsg = "ES数据一致性检查失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES数据一致性检查异常", errorMsg);
        }
    }

    /**
     * 图片增量同步（每5分钟执行，随机延迟0-30秒）
     */
    @Scheduled(fixedDelay = INCREMENTAL_SYNC_DELAY_MILLIS)
    public void incrementalSyncPictures() {
        executeIncrementalSync(this::incrementalSyncPicturesInternal, "picture");
    }

    /**
     * 用户增量同步（每5分钟执行，初始延迟20秒 + 随机延迟0-30秒）
     */
    @Scheduled(fixedDelay = INCREMENTAL_SYNC_DELAY_MILLIS, initialDelay = 20000)
    public void incrementalSyncUsers() {
        executeIncrementalSync(this::incrementalSyncUsersInternal, "user");
    }

    /**
     * 帖子增量同步（每5分钟执行，初始延迟40秒 + 随机延迟0-30秒）
     */
    @Scheduled(fixedDelay = INCREMENTAL_SYNC_DELAY_MILLIS, initialDelay = 40000)
    public void incrementalSyncPosts() {
        executeIncrementalSync(this::incrementalSyncPostsInternal, "post");
    }

    /**
     * 空间增量同步（每5分钟执行，初始延迟40秒 + 随机延迟0-30秒）
     */
    @Scheduled(fixedDelay = INCREMENTAL_SYNC_DELAY_MILLIS, initialDelay = 40000)
    public void incrementalSyncSpaces() {
        executeIncrementalSync(this::incrementalSyncSpacesInternal, "space");
    }

    // ===================== 应用启动执行逻辑 =====================

    /**
     * 应用启动时删除旧索引并执行全量同步
     */
    @Override
    public void run(String... args) {
        try {
            log.info("===== 应用启动，开始删除并重建ES索引 =====");
            // 删除并重建ES索引以确保最新字段映射
            deleteAndRecreateIndices();
            log.info("===== ES索引删除并重建完成 =====");

            log.info("===== 开始ES全量同步 =====");
            fullSync();
            log.info("===== 应用启动，ES全量同步完成 =====");
        } catch (Exception e) {
            String errorMsg = "应用启动ES索引重建或全量同步失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("应用启动ES同步异常", errorMsg);
        }
    }

    // ===================== 核心同步逻辑 =====================

    /**
     * 全量同步入口（按配置启用不同类型数据同步）
     */
    public void fullSync() {
        try {
            log.info("===== 开始全量同步数据到ES =====");

            if (pictureSyncEnabled) {
                fullSyncPictures();
            }
            if (userSyncEnabled) {
                fullSyncUsers();
            }
            if (postSyncEnabled) {
                fullSyncPosts();
            }
            if (spaceSyncEnabled) {
                fullSyncSpaces();
            }

            log.info("===== 全量同步数据到ES完成 =====");
        } catch (Exception e) {
            String errorMsg = "全量同步ES失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES全量同步异常", errorMsg);
            throw e;
        }
    }

    /**
     * 全量同步图片数据
     * 同步范围：未删除、非草稿、公共空间（spaceId为空）的图片
     */
    private void fullSyncPictures() {
        try {
            // 1. 查询MySQL中符合条件的总记录数
            long totalCount = pictureMapper.selectCount(
                    new QueryWrapper<Picture>()
                            .eq("isDelete", 0)
                            .eq("isDraft", 0)
                            .isNull("spaceId")
            );

            log.info("开始全量同步图片数据，符合条件总数：{}", totalCount);

            if (totalCount == 0) {
                log.info("图片全量同步：无符合条件数据，直接返回");
                return;
            }

            // 2. 计算总页数
            long totalPages = calculateTotalPages(totalCount, batchSize);

            // 3. 分页并发同步
            syncByPage(
                    totalPages,
                    batchSize,
                    INDEX_NAME_PICTURE,
                    this::buildPictureQueryWrapper,
                    pictureMapper::selectPage,
                    this::buildPictureIndexQuery
            );

            // 4. 清理无效数据
            cleanDeletedPictures();
            cleanDraftPictures();

            log.info("图片全量同步完成");
        } catch (Exception e) {
            String errorMsg = "全量同步图片数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES图片全量同步异常", errorMsg);
            throw e;
        }
    }

    /**
     * 全量同步用户数据
     * 同步范围：未删除、未被封禁的用户
     */
    private void fullSyncUsers() {
        try {
            // 1. 查询MySQL中符合条件的总记录数
            long totalCount = userMapper.selectCount(
                    new QueryWrapper<User>()
                            .eq("isDelete", 0)
                            .ne("userRole", "ban")
            );

            log.info("开始全量同步用户数据，符合条件总数：{}", totalCount);

            if (totalCount == 0) {
                log.info("用户全量同步：无符合条件数据，直接返回");
                return;
            }

            // 2. 计算总页数
            long totalPages = calculateTotalPages(totalCount, batchSize);

            // 3. 分页并发同步
            syncByPage(
                    totalPages,
                    batchSize,
                    INDEX_NAME_USER,
                    this::buildUserQueryWrapper,
                    userMapper::selectPage,
                    this::buildUserIndexQuery
            );

            // 4. 清理无效数据
            cleanDeletedUsers();

            log.info("用户全量同步完成");
        } catch (Exception e) {
            String errorMsg = "全量同步用户数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES用户全量同步异常", errorMsg);
            throw e;
        }
    }

    /**
     * 全量同步帖子数据
     * 同步范围：未删除、审核通过的帖子
     */
    private void fullSyncPosts() {
        try {
            // 1. 查询MySQL中符合条件的总记录数
            long totalCount = postMapper.selectCount(
                    new QueryWrapper<Post>()
                            .eq("isDelete", 0)
                            .eq("status", 1)
                            .eq("isDraft", 0) // 只同步非草稿的帖子
            );

            log.info("开始全量同步帖子数据，符合条件总数：{}", totalCount);

            if (totalCount == 0) {
                log.info("帖子全量同步：无符合条件数据，直接返回");
                return;
            }

            // 2. 计算总页数
            long totalPages = calculateTotalPages(totalCount, batchSize);

            // 3. 分页并发同步
            syncByPage(
                    totalPages,
                    batchSize,
                    INDEX_NAME_POST,
                    this::buildPostQueryWrapper,
                    postMapper::selectPage,
                    this::buildPostIndexQuery
            );

            log.info("帖子全量同步完成");
        } catch (Exception e) {
            String errorMsg = "全量同步帖子数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES帖子全量同步异常", errorMsg);
            throw e;
        }
    }

    /**
     * 全量同步空间数据
     * 同步范围：未删除、团队空间（spaceType=1）
     */
    private void fullSyncSpaces() {
        try {
            // 1. 查询MySQL中符合条件的总记录数
            long totalCount = spaceMapper.selectCount(
                    new QueryWrapper<Space>()
                            .eq("isDelete", 0)
                            .eq("spaceType", 1)
            );

            log.info("开始全量同步空间数据，符合条件总数：{}", totalCount);

            if (totalCount == 0) {
                log.info("空间全量同步：无符合条件数据，直接返回");
                return;
            }

            // 2. 计算总页数
            long totalPages = calculateTotalPages(totalCount, batchSize);

            // 3. 分页并发同步
            syncByPage(
                    totalPages,
                    batchSize,
                    INDEX_NAME_SPACE,
                    this::buildSpaceQueryWrapper,
                    spaceMapper::selectPage,
                    this::buildSpaceIndexQuery
            );

            // 4. 清理无效数据
            cleanDeletedSpaces();

            log.info("空间全量同步完成");
        } catch (Exception e) {
            String errorMsg = "全量同步空间数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES空间全量同步异常", errorMsg);
            throw e;
        }
    }

    // ===================== 增量同步内部逻辑 =====================

    /**
     * 图片增量同步内部逻辑
     * 处理范围：
     * 1. 最近5分钟更新的有效图片（未删除、审核通过、非草稿、公共空间）
     * 2. 最近5分钟删除的图片
     * 3. 最近5分钟审核未通过的图片（从ES删除）
     * 4. 最近5分钟审核通过的图片（添加到ES）
     */
    private void incrementalSyncPicturesInternal() {
        Date syncTime = calculateSyncTime();

        try {
            // 1. 同步新增/更新的有效图片
            syncIncrementalValidData(
                    syncTime,
                    this::buildPictureIncrementalQueryWrapper,
                    pictureMapper::selectList,
                    INDEX_NAME_PICTURE,
                    this::buildPictureIndexQuery,
                    "picture"
            );

            // 2. 删除已删除的图片
            cleanIncrementalDeletedData(
                    syncTime,
                    pictureMapper::selectDeletedPicturesByUpdateTime,
                    INDEX_NAME_PICTURE,
                    "picture"
            );

            // 3. 删除审核未通过的图片
            cleanIncrementalUnreviewedData(
                    syncTime,
                    this::buildPictureUnreviewedQueryWrapper,
                    pictureMapper::selectList,
                    INDEX_NAME_PICTURE,
                    "picture"
            );

            // 4. 同步审核通过的图片
            syncIncrementalReviewedData(
                    syncTime,
                    this::buildPictureReviewedQueryWrapper,
                    pictureMapper::selectList,
                    INDEX_NAME_PICTURE,
                    this::buildPictureIndexQuery,
                    "picture"
            );
        } catch (Exception e) {
            String errorMsg = "增量同步图片数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES图片增量同步异常", errorMsg);
        }
    }

    /**
     * 用户增量同步内部逻辑
     * 处理范围：
     * 1. 最近5分钟更新的有效用户（未删除、未封禁）
     * 2. 最近5分钟删除的用户
     * 3. 最近5分钟被封禁的用户（从ES删除）
     */
    private void incrementalSyncUsersInternal() {
        Date syncTime = calculateSyncTime();

        try {
            // 1. 同步新增/更新的有效用户
            syncIncrementalValidData(
                    syncTime,
                    this::buildUserIncrementalQueryWrapper,
                    userMapper::selectList,
                    INDEX_NAME_USER,
                    this::buildUserIndexQuery,
                    "user"
            );

            // 2. 删除已删除的用户
            cleanIncrementalDeletedData(
                    syncTime,
                    userMapper::selectDeletedUsersByUpdateTime,
                    INDEX_NAME_USER,
                    "user"
            );

            // 3. 删除被封禁的用户
            cleanIncrementalBannedData(
                    syncTime,
                    this::buildUserBannedQueryWrapper,
                    userMapper::selectList,
                    INDEX_NAME_USER,
                    "user"
            );
        } catch (Exception e) {
            String errorMsg = "增量同步用户数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES用户增量同步异常", errorMsg);
        }
    }

    /**
     * 帖子增量同步内部逻辑
     * 处理范围：
     * 1. 最近5分钟更新的有效帖子（未删除、审核通过）
     * 2. 最近5分钟删除的帖子
     * 3. 最近5分钟审核未通过的帖子（从ES删除）
     */
    private void incrementalSyncPostsInternal() {
        Date syncTime = calculateSyncTime();

        try {
            // 1. 同步新增/更新的有效帖子
            syncIncrementalValidData(
                    syncTime,
                    this::buildPostIncrementalQueryWrapper,
                    postMapper::selectList,
                    INDEX_NAME_POST,
                    this::buildPostIndexQuery,
                    "post"
            );

            // 2. 删除已删除的帖子
            cleanIncrementalDeletedData(
                    syncTime,
                    postMapper::selectDeletedPostsByUpdateTime,
                    INDEX_NAME_POST,
                    "post"
            );

            // 3. 删除审核未通过的帖子
            cleanIncrementalUnreviewedData(
                    syncTime,
                    this::buildPostUnreviewedQueryWrapper,
                    postMapper::selectList,
                    INDEX_NAME_POST,
                    "post"
            );
        } catch (Exception e) {
            String errorMsg = "增量同步帖子数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES帖子增量同步异常", errorMsg);
        }
    }

    /**
     * 空间增量同步内部逻辑
     * 处理范围：
     * 1. 最近5分钟更新的有效空间（未删除、团队空间）
     * 2. 最近5分钟删除的空间
     */
    private void incrementalSyncSpacesInternal() {
        Date syncTime = calculateSyncTime();

        try {
            // 1. 同步新增/更新的有效空间
            syncIncrementalValidData(
                    syncTime,
                    this::buildSpaceIncrementalQueryWrapper,
                    spaceMapper::selectList,
                    INDEX_NAME_SPACE,
                    this::buildSpaceIndexQuery,
                    "space"
            );

            // 2. 删除已删除的空间
            cleanIncrementalDeletedData(
                    syncTime,
                    spaceMapper::selectDeletedSpacesByUpdateTime,
                    INDEX_NAME_SPACE,
                    "space"
            );
        } catch (Exception e) {
            String errorMsg = "增量同步空间数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES空间增量同步异常", errorMsg);
        }
    }

    // ===================== 数据清理逻辑 =====================

    /**
     * 清理ES中已删除的图片数据
     */
    private void cleanDeletedPictures() {
        cleanDeletedData(
                pictureMapper::selectAllDeletedPictures,
                INDEX_NAME_PICTURE,
                "picture"
        );
    }

    /**
     * 清理ES中草稿状态的图片数据
     */
    private void cleanDraftPictures() {
        try {
            // 查询所有草稿状态的图片
            List<Picture> draftPictures = pictureMapper.selectList(
                    new QueryWrapper<Picture>().eq("isDraft", 1)
            );

            if (draftPictures.isEmpty()) {
                log.info("清理ES草稿图片：无草稿图片数据");
                return;
            }

            log.info("准备清理ES中草稿状态的图片，数量：{}", draftPictures.size());

            // 执行批量删除
            batchDeleteFromEs(
                    draftPictures.stream().map(Picture::getId).map(String::valueOf).collect(Collectors.toList()),
                    INDEX_NAME_PICTURE,
                    "draft_picture"
            );

        } catch (Exception e) {
            String errorMsg = "清理ES中草稿状态的图片数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES草稿图片清理异常", errorMsg);
        }
    }

    /**
     * 清理ES中已删除的用户数据
     */
    private void cleanDeletedUsers() {
        cleanDeletedData(
                userMapper::selectAllDeletedUsers,
                INDEX_NAME_USER,
                "user"
        );
    }

    /**
     * 清理ES中已删除的空间数据
     */
    private void cleanDeletedSpaces() {
        cleanDeletedData(
                spaceMapper::selectAllDeletedSpaces,
                INDEX_NAME_SPACE,
                "space"
        );
    }

    // ===================== 数据一致性检查逻辑 =====================

    /**
     * 数据一致性检查与修复入口
     */
    private void checkAndFixDataConsistency() {
        try {
            checkAndFixPictureConsistency();
            checkAndFixUserConsistency();
            checkAndFixPostConsistency();
            checkAndFixSpaceConsistency();
        } catch (Exception e) {
            String errorMsg = "数据一致性检查与修复失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES数据一致性检查异常", errorMsg);
        }
    }

    /**
     * 检查并修复图片数据一致性
     */
    private void checkAndFixPictureConsistency() {
        checkAndFixDataConsistency(
                () -> pictureMapper.selectCount(buildPictureQueryWrapper()),
                INDEX_NAME_PICTURE,
                this::fullSyncPictures,
                "picture"
        );
    }

    /**
     * 检查并修复用户数据一致性
     */
    private void checkAndFixUserConsistency() {
        checkAndFixDataConsistency(
                () -> userMapper.selectCount(buildUserQueryWrapper()),
                INDEX_NAME_USER,
                this::fullSyncUsers,
                "user"
        );
    }

    /**
     * 检查并修复帖子数据一致性
     */
    private void checkAndFixPostConsistency() {
        checkAndFixDataConsistency(
                () -> postMapper.selectCount(buildPostQueryWrapper()),
                INDEX_NAME_POST,
                this::fullSyncPosts,
                "post"
        );
    }

    /**
     * 检查并修复空间数据一致性
     */
    private void checkAndFixSpaceConsistency() {
        checkAndFixDataConsistency(
                () -> spaceMapper.selectCount(buildSpaceQueryWrapper()),
                INDEX_NAME_SPACE,
                this::fullSyncSpaces,
                "space"
        );
    }

    // ===================== 通用工具方法 =====================

    /**
     * 执行增量同步（封装通用逻辑：随机延迟 + 日志 + 异常处理）
     *
     * @param syncLogic 增量同步具体逻辑
     * @param dataTypeName 数据类型名称（用于日志）
     */
    private void executeIncrementalSync(Runnable syncLogic, String dataTypeName) {
        try {
            // 随机延迟，避免任务集中执行
            long randomDelay = RandomUtil.randomInt(0, incrementalRandomDelayMax) * 1000L;
            Thread.sleep(randomDelay);

            log.info("===== 开始增量同步{}数据到ES（延迟{}秒） =====", dataTypeName, randomDelay / 1000);
            syncLogic.run();
            log.info("===== 增量同步{}数据到ES完成 =====", dataTypeName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{}增量同步被中断", dataTypeName, e);
        } catch (Exception e) {
            String errorMsg = "增量同步" + dataTypeName + "数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES" + dataTypeName + "增量同步异常", errorMsg);
        }
    }

    /**
     * 计算总页数
     *
     * @param totalCount 总记录数
     * @param pageSize 每页大小
     * @return 总页数
     */
    private long calculateTotalPages(long totalCount, int pageSize) {
        return (totalCount + pageSize - 1) / pageSize;
    }

    /**
     * 计算增量同步的时间阈值（当前时间 - 5分钟）
     *
     * @return 时间阈值
     */
    private Date calculateSyncTime() {
        return new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(INCREMENTAL_SYNC_INTERVAL_MINUTES));
    }

    /**
     * 分页并发同步数据（通用方法）
     *
     * @param totalPages 总页数
     * @param pageSize 每页大小
     * @param indexName ES索引名
     * @param queryWrapperBuilder 查询条件构建器
     * @param pageQuery 分页查询方法
     * @param indexQueryBuilder 索引查询构建器
     * @param <T> 数据实体类型
     */
    private <T> void syncByPage(
            long totalPages,
            int pageSize,
            String indexName,
            java.util.function.Supplier<QueryWrapper<T>> queryWrapperBuilder,
            java.util.function.BiFunction<Page<T>, QueryWrapper<T>, IPage<T>> pageQuery,
            java.util.function.Function<T, IndexQuery> indexQueryBuilder
    ) {
        // 边缘场景处理：检查参数是否为空
        if (totalPages <= 0 || pageSize <= 0 || indexName == null || queryWrapperBuilder == null ||
                pageQuery == null || indexQueryBuilder == null) {
            log.warn("全量同步分页参数异常，跳过同步：totalPages={}, pageSize={}, indexName={}", totalPages, pageSize, indexName);
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (long pageNum = 1; pageNum <= totalPages; pageNum++) {
            final long currentPage = pageNum;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 边缘场景处理：检查当前页码是否有效
                    if (currentPage <= 0) {
                        log.warn("无效页码：{}，跳过此页同步", currentPage);
                        return;
                    }

                    // 分页查询数据
                    Page<T> page = new Page<>(currentPage, pageSize);
                    QueryWrapper<T> queryWrapper = queryWrapperBuilder.get();

                    // 边缘场景处理：检查queryWrapper是否为空
                    if (queryWrapper == null) {
                        log.warn("{}同步第{}页：查询条件为空，跳过此页", indexName, currentPage);
                        return;
                    }

                    IPage<T> dataPage = pageQuery.apply(page, queryWrapper);

                    // 边缘场景处理：检查dataPage是否为空
                    if (dataPage == null) {
                        log.warn("{}同步第{}页：查询结果为空，跳过此页", indexName, currentPage);
                        return;
                    }

                    List<T> dataList = dataPage.getRecords();

                    if (dataList == null || dataList.isEmpty()) {
                        log.info("{}同步第{}页：无数据", indexName, currentPage);
                        return;
                    }

                    // 构建索引请求
                    List<IndexQuery> indexQueries = dataList.stream()
                            .map(indexQueryBuilder)
                            .collect(Collectors.toList());

                    // 边缘场景处理：检查indexQueries是否为空
                    if (indexQueries == null || indexQueries.isEmpty()) {
                        log.info("{}同步第{}页：构建的索引请求为空，跳过此页", indexName, currentPage);
                        return;
                    }

                    // 使用批量处理方法同步到ES
                    processInBatches(indexQueries, MAX_BATCH_SIZE, batch -> {
                        retryOperation(() -> {
                            elasticsearchRestTemplate.bulkIndex(batch, IndexCoordinates.of(indexName));
                            return null;
                        }, "批量索引" + indexName + "数据到ES", indexName);
                    });

                    log.info("{}同步第{}页完成，数量：{}", indexName, currentPage, dataList.size());

                    // 主动清理，帮助GC
                    indexQueries.clear();
                } catch (Exception e) {
                    log.error("{}同步第{}页失败: {}", indexName, currentPage, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, syncExecutor);

            futures.add(future);
        }

        // 等待所有分页任务完成
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * 同步增量有效数据（通用方法）
     *
     * @param syncTime 同步时间阈值
     * @param queryWrapperBuilder 查询条件构建器
     * @param dataQuery 数据查询方法
     * @param indexName ES索引名
     * @param indexQueryBuilder 索引查询构建器
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void syncIncrementalValidData(
            Date syncTime,
            java.util.function.Function<Date, QueryWrapper<T>> queryWrapperBuilder,
            java.util.function.Function<QueryWrapper<T>, List<T>> dataQuery,
            String indexName,
            java.util.function.Function<T, IndexQuery> indexQueryBuilder,
            String dataTypeName
    ) {
        // 构建查询条件
        QueryWrapper<T> queryWrapper = queryWrapperBuilder.apply(syncTime);
        List<T> dataList = dataQuery.apply(queryWrapper);

        if (dataList.isEmpty()) {
            log.info("{}增量同步：无需要同步的有效{}", dataTypeName, dataTypeName);
            return;
        }

        log.info("{}增量同步：准备同步有效{}，数量：{}", dataTypeName, dataTypeName, dataList.size());

        // 构建索引请求
        List<IndexQuery> indexQueries = dataList.stream()
                .map(indexQueryBuilder)
                .collect(Collectors.toList());

        // 使用批量处理方法同步到ES
        processInBatches(indexQueries, MAX_BATCH_SIZE, batch -> {
            retryOperation(() -> {
                elasticsearchRestTemplate.bulkIndex(batch, IndexCoordinates.of(indexName));
                return null;
            }, "批量索引增量" + dataTypeName + "数据到ES", indexName);
        });

        log.info("{}增量同步：成功同步有效{}，数量：{}", dataTypeName, dataTypeName, dataList.size());

        // 主动清理
        indexQueries.clear();
    }

    /**
     * 清理增量删除数据（通用方法）
     *
     * @param syncTime 同步时间阈值
     * @param deletedDataQuery 已删除数据查询方法
     * @param indexName ES索引名
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void cleanIncrementalDeletedData(
            Date syncTime,
            java.util.function.Function<Date, List<T>> deletedDataQuery,
            String indexName,
            String dataTypeName
    ) {
        List<T> deletedDataList = deletedDataQuery.apply(syncTime);

        if (deletedDataList.isEmpty()) {
            log.info("{}增量同步：无需要删除的{}", dataTypeName, dataTypeName);
            return;
        }

        log.info("{}增量同步：准备删除已删除的{}，数量：{}", dataTypeName, dataTypeName, deletedDataList.size());

        // 提取ID并执行删除
        List<String> idsToDelete = extractIds(deletedDataList);
        int failCount = batchDeleteFromEs(idsToDelete, indexName, dataTypeName);

        // 失败率超10%发送告警
        checkAndSendDeleteAlert(idsToDelete.size(), failCount, dataTypeName, "增量清理");
    }

    /**
     * 清理增量审核未通过数据（通用方法）
     *
     * @param syncTime 同步时间阈值
     * @param queryWrapperBuilder 查询条件构建器
     * @param dataQuery 数据查询方法
     * @param indexName ES索引名
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void cleanIncrementalUnreviewedData(
            Date syncTime,
            java.util.function.Function<Date, QueryWrapper<T>> queryWrapperBuilder,
            java.util.function.Function<QueryWrapper<T>, List<T>> dataQuery,
            String indexName,
            String dataTypeName
    ) {
        QueryWrapper<T> queryWrapper = queryWrapperBuilder.apply(syncTime);
        List<T> unreviewedDataList = dataQuery.apply(queryWrapper);

        if (unreviewedDataList.isEmpty()) {
            log.info("{}增量同步：无需要删除的审核未通过{}", dataTypeName, dataTypeName);
            return;
        }

        log.info("{}增量同步：准备删除审核未通过的{}，数量：{}", dataTypeName, dataTypeName, unreviewedDataList.size());

        // 提取ID并执行删除
        List<String> idsToDelete = extractIds(unreviewedDataList);
        int failCount = batchDeleteFromEs(idsToDelete, indexName, dataTypeName);

        // 失败率超10%发送告警
        checkAndSendDeleteAlert(idsToDelete.size(), failCount, dataTypeName, "审核状态清理");
    }

    /**
     * 清理增量被封禁数据（通用方法）
     *
     * @param syncTime 同步时间阈值
     * @param queryWrapperBuilder 查询条件构建器
     * @param dataQuery 数据查询方法
     * @param indexName ES索引名
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void cleanIncrementalBannedData(
            Date syncTime,
            java.util.function.Function<Date, QueryWrapper<T>> queryWrapperBuilder,
            java.util.function.Function<QueryWrapper<T>, List<T>> dataQuery,
            String indexName,
            String dataTypeName
    ) {
        QueryWrapper<T> queryWrapper = queryWrapperBuilder.apply(syncTime);
        List<T> bannedDataList = dataQuery.apply(queryWrapper);

        if (bannedDataList.isEmpty()) {
            log.info("{}增量同步：无需要删除的被封禁{}", dataTypeName, dataTypeName);
            return;
        }

        log.info("{}增量同步：准备删除被封禁的{}，数量：{}", dataTypeName, dataTypeName, bannedDataList.size());

        // 提取ID并执行删除
        List<String> idsToDelete = extractIds(bannedDataList);
        int failCount = batchDeleteFromEs(idsToDelete, indexName, dataTypeName);

        // 失败率超10%发送告警
        checkAndSendDeleteAlert(idsToDelete.size(), failCount, dataTypeName, "封禁状态清理");
    }

    /**
     * 同步增量审核通过数据（通用方法）
     *
     * @param syncTime 同步时间阈值
     * @param queryWrapperBuilder 查询条件构建器
     * @param dataQuery 数据查询方法
     * @param indexName ES索引名
     * @param indexQueryBuilder 索引查询构建器
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void syncIncrementalReviewedData(
            Date syncTime,
            java.util.function.Function<Date, QueryWrapper<T>> queryWrapperBuilder,
            java.util.function.Function<QueryWrapper<T>, List<T>> dataQuery,
            String indexName,
            java.util.function.Function<T, IndexQuery> indexQueryBuilder,
            String dataTypeName
    ) {
        QueryWrapper<T> queryWrapper = queryWrapperBuilder.apply(syncTime);
        List<T> reviewedDataList = dataQuery.apply(queryWrapper);

        if (reviewedDataList.isEmpty()) {
            log.info("{}增量同步：无需要添加的审核通过{}", dataTypeName, dataTypeName);
            return;
        }

        log.info("{}增量同步：准备添加审核通过的{}，数量：{}", dataTypeName, dataTypeName, reviewedDataList.size());

        // 构建索引请求
        List<IndexQuery> indexQueries = reviewedDataList.stream()
                .map(indexQueryBuilder)
                .collect(Collectors.toList());

        // 使用批量处理方法同步到ES
        processInBatches(indexQueries, MAX_BATCH_SIZE, batch -> {
            retryOperation(() -> {
                elasticsearchRestTemplate.bulkIndex(batch, IndexCoordinates.of(indexName));
                return null;
            }, "批量索引审核通过的" + dataTypeName + "数据到ES", indexName);
        });

        int failCount = 0; // 注意：这里为了保持原有逻辑，仍然定义failCount但不使用具体的单个处理逻辑

        log.info("{}增量同步：成功添加审核通过的{}，总数：{}，成功：{}，失败：{}",
                dataTypeName, dataTypeName, reviewedDataList.size(), reviewedDataList.size() - failCount, failCount);

        // 失败率超10%发送告警
        checkAndSendDeleteAlert(reviewedDataList.size(), failCount, dataTypeName, "审核状态添加");
    }

    /**
     * 清理已删除数据（通用方法）
     *
     * @param deletedDataQuery 已删除数据查询方法
     * @param indexName ES索引名
     * @param dataTypeName 数据类型名称
     * @param <T> 数据实体类型
     */
    private <T> void cleanDeletedData(
            java.util.function.Supplier<List<T>> deletedDataQuery,
            String indexName,
            String dataTypeName
    ) {
        try {
            List<T> deletedDataList = deletedDataQuery.get();

            if (deletedDataList.isEmpty()) {
                log.info("清理ES{}：无已删除{}数据", dataTypeName, dataTypeName);
                return;
            }

            log.info("准备清理ES中已删除的{}，数量：{}", dataTypeName, deletedDataList.size());

            // 提取ID并执行删除
            List<String> idsToDelete = extractIds(deletedDataList);
            int failCount = batchDeleteFromEs(idsToDelete, indexName, dataTypeName);

            // 失败率超10%发送告警
            checkAndSendDeleteAlert(idsToDelete.size(), failCount, dataTypeName, "全量清理");

        } catch (Exception e) {
            String errorMsg = "清理已删除的" + dataTypeName + "数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES" + dataTypeName + "清理异常", errorMsg);
        }
    }

    /**
     * 批量从ES删除数据（通用方法）
     *
     * @param idsToDelete 待删除ID列表
     * @param indexName ES索引名
     * @param dataTypeName 数据类型名称
     * @return 失败数量
     */
    private int batchDeleteFromEs(List<String> idsToDelete, String indexName, String dataTypeName) {
        int successCount = 0;
        int failCount = 0;

        try {
            // 使用批量处理方法进行删除
            List<String> idBatches = new ArrayList<>();
            for (String id : idsToDelete) {
                idBatches.add(id);
            }

            processInBatches(idBatches, MAX_BATCH_SIZE, batch -> {
                for (String id : batch) {
                    retryOperation(() -> {
                        elasticsearchRestTemplate.delete(id, IndexCoordinates.of(indexName));
                        return null;
                    }, "批量从ES删除" + dataTypeName, indexName);
                }
            });

            successCount = idsToDelete.size();
        } catch (Exception e) {
            log.error("批量从ES删除{}失败, 数量: {}: {}", dataTypeName, idsToDelete.size(), e.getMessage(), e);

            // 批量删除失败，逐个删除
            for (String id : idsToDelete) {
                try {
                    retryOperation(() -> {
                        elasticsearchRestTemplate.delete(id, IndexCoordinates.of(indexName));
                        return null;
                    }, "从ES删除" + dataTypeName, indexName);
                    successCount++;
                } catch (Exception ex) {
                    log.error("从ES删除{}失败, ID: {}: {}", dataTypeName, id, ex.getMessage(), ex);
                    failCount++;
                }
            }
        }

        log.info("清理ES中{}完成, 成功：{}，失败：{}", dataTypeName, successCount, failCount);
        return failCount;
    }

    /**
     * 检查并发送删除失败告警
     *
     * @param totalCount 总数
     * @param failCount 失败数
     * @param dataTypeName 数据类型名称
     * @param operationType 操作类型
     */
    private void checkAndSendDeleteAlert(int totalCount, int failCount, String dataTypeName, String operationType) {
        if (failCount > 0 && failCount > totalCount * 0.1) {
            String alertContent = String.format(
                    "ES%s%s失败数量过多：\n总数：%d\n失败：%d",
                    dataTypeName, operationType, totalCount, failCount
            );
            sendAlertEmail("ES" + dataTypeName + "增量清理异常", alertContent);
        }
    }

    /**
     * 检查并修复数据一致性（通用方法）
     *
     * @param mysqlCountSupplier MySQL数量查询方法
     * @param indexName ES索引名
     * @param fullSyncFunc 全量同步方法
     * @param dataTypeName 数据类型名称
     */
    private void checkAndFixDataConsistency(
            java.util.function.Supplier<Long> mysqlCountSupplier,
            String indexName,
            Runnable fullSyncFunc,
            String dataTypeName
    ) {
        try {
            // 1. 获取MySQL和ES的数量
            long mysqlCount = mysqlCountSupplier.get();
            long esCount = getEsDocumentCount(indexName);

            // 2. 检查数量差异
            boolean needFullSync = Math.abs(mysqlCount - esCount) > CONSISTENCY_CHECK_THRESHOLD;

            // 4. 需要全量同步则执行
            if (needFullSync) {
                log.warn("{}数据数量不一致：MySQL={}, ES={}, 差异={}条，开始全量同步修复",
                        dataTypeName, mysqlCount, esCount, Math.abs(mysqlCount - esCount));
                fullSyncFunc.run();
            }

        } catch (Exception e) {
            String errorMsg = "检查" + dataTypeName + "数据一致性失败: " + e.getMessage();
            log.error(errorMsg, e);
            sendAlertEmail("ES" + dataTypeName + "一致性检查异常", errorMsg);
        }
    }

    /**
     * 重试操作 - 使用指数退避策略
     *
     * @param operation 待重试的操作
     * @param operationName 操作名称（用于日志）
     * @param dataType 数据类型（用于统计）
     * @param <T> 返回值类型
     * @return 操作执行结果
     */
    private <T> T retryOperation(java.util.function.Supplier<T> operation, String operationName, String dataType) {
        Exception lastException = null;

        for (int retryNum = 0; retryNum < retryTimes; retryNum++) {
            try {
                T result = operation.get();
                syncSuccessCount.computeIfAbsent(dataType, k -> new AtomicInteger()).incrementAndGet();
                return result;
            } catch (Exception e) {
                lastException = e;

                if (isRetryableException(e)) {
                    log.warn("{}失败，第{}次重试，原因: {}", operationName, retryNum + 1, e.getMessage());

                    // 非最后一次重试，执行指数退避
                    if (retryNum < retryTimes - 1) {
                        try {
                            long exponentialBackoff = retryInterval * (long) Math.pow(2, retryNum);
                            // 限制最大等待时间，避免总重试等待时间过长
                            long actualSleepTime = Math.min(exponentialBackoff, MAX_RETRY_WAIT_TIME);
                            Thread.sleep(actualSleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试被中断", ie);
                        }
                    }
                } else {
                    log.error("{}遇到不可重试异常，停止重试，原因: {}", operationName, e.getMessage());
                    break;
                }
            }
        }

        // 记录失败统计
        syncFailureCount.computeIfAbsent(dataType, k -> new AtomicInteger()).incrementAndGet();

        String errorMsg = String.format("重试%d次后仍然失败: %s，数据类型：%s", retryTimes, operationName, dataType);
        log.error(errorMsg, lastException);

        // 发送告警邮件
        sendAlertEmail(dataType + "同步重试失败", errorMsg + "\n原因：" + (lastException != null ? lastException.getMessage() : "未知"));

        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * 判断是否为可重试异常
     *
     * @param e 异常
     * @return true-可重试，false-不可重试
     */
    private boolean isRetryableException(Exception e) {
        // 网络/连接相关异常可重试
        if (e instanceof org.springframework.dao.RecoverableDataAccessException) {
            return true;
        }
        if (e instanceof ConnectException || e instanceof SocketTimeoutException) {
            return true;
        }

        // 包含连接/超时/重试关键字的异常可重试
        if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            return msg.contains("connection") || msg.contains("timeout") || msg.contains("retry");
        }

        // 数据格式/参数错误不可重试
        if (e instanceof IllegalArgumentException) {
            return false;
        }

        // 默认可重试
        return true;
    }

    /**
     * 获取ES索引中文档数量
     *
     * @param indexName 索引名
     * @return 文档数量
     */
    private long getEsDocumentCount(String indexName) {
        // 前置判断：检查参数是否为空
        if (indexName == null || indexName.trim().isEmpty()) {
            log.warn("ES索引名称为空，跳过计数操作");
            return 0;
        }

        // 前置判断：检查elasticsearchRestTemplate是否可用
        if (elasticsearchRestTemplate == null) {
            log.error("ElasticsearchRestTemplate未初始化，无法执行计数操作");
            sendAlertEmail("ES连接异常", "ElasticsearchRestTemplate未初始化，无法执行计数操作");
            return 0;
        }

        try {
            NativeSearchQuery countQuery = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.matchAllQuery())
                    .build();

            long count = elasticsearchRestTemplate.count(countQuery, Object.class, IndexCoordinates.of(indexName));
            log.debug("获取ES索引{}文档数量成功：{}", indexName, count);
            return count;
        } catch (Exception e) {
            log.warn("获取ES索引{}文档数量失败: {}", indexName, e.getMessage(), e);
            sendAlertEmail("ES索引计数异常", "获取ES索引[" + indexName + "]文档数量失败：\n" + e.getMessage());
            return 0;
        }
    }

    /**
     * 发送告警邮件（封装频率控制逻辑）
     *
     * @param title 邮件标题
     * @param content 邮件内容
     */
    private void sendAlertEmail(String title, String content) {
        long currentTime = System.currentTimeMillis();

        // 频率控制：避免短时间内发送过多邮件
        if (currentTime - lastAlertTime >= alertInterval) {
            synchronized (this) {
                if (currentTime - lastAlertTime >= alertInterval) {
                    try {
                        emailSenderUtil.sendEsFailureAlert(adminEmail, title, content);
                        log.info("告警邮件发送成功：{}", title);
                        lastAlertTime = currentTime;
                        // 重置该类型告警的失败计数
                        alertFailureCount.computeIfAbsent(title, k -> new AtomicInteger()).set(0);
                    } catch (Exception e) {
                        log.error("发送告警邮件失败: " + e.getMessage(), e);

                        // 增加该类型告警的失败计数
                        String alertType = title;
                        AtomicInteger counter = alertFailureCount.computeIfAbsent(alertType, k -> new AtomicInteger());
                        int currentFailureCount = counter.incrementAndGet();

                        // 只有连续失败达到阈值时才记录错误日志，减少告警噪音
                        if (currentFailureCount >= ALERT_FAILURE_THRESHOLD) {
                            log.error("【终极兜底】告警邮件发送失败，连续失败{}次，原始异常：{} - {}", currentFailureCount, title, content);
                        } else {
                            log.debug("告警邮件发送失败，已累计失败{}次：{}", currentFailureCount, title);
                        }
                    }
                }
            }
        } else {
            log.info("告警邮件发送频率限制，跳过本次发送：{}", title);
            log.debug("【兜底日志】{}：{}", title, content);
        }
    }

    // ===================== QueryWrapper构建器 =====================

    /**
     * 构建图片全量同步查询条件
     *
     * @return 查询条件
     */
    private QueryWrapper<Picture> buildPictureQueryWrapper() {
        return new QueryWrapper<Picture>()
                .eq("isDelete", 0)
                .eq("isDraft", 0)
                .isNull("spaceId");
    }

    /**
     * 构建图片增量同步查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Picture> buildPictureIncrementalQueryWrapper(Date syncTime) {
        return new QueryWrapper<Picture>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("reviewStatus", 1)
                .eq("isDraft", 0)
                .isNull("spaceId");
    }

    /**
     * 构建图片审核未通过查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Picture> buildPictureUnreviewedQueryWrapper(Date syncTime) {
        return new QueryWrapper<Picture>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("reviewStatus", 0)
                .eq("isDraft", 0)
                .isNull("spaceId");
    }

    /**
     * 构建图片审核通过查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Picture> buildPictureReviewedQueryWrapper(Date syncTime) {
        return new QueryWrapper<Picture>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("reviewStatus", 1)
                .eq("isDraft", 0)
                .isNull("spaceId");
    }

    /**
     * 构建用户全量同步查询条件
     *
     * @return 查询条件
     */
    private QueryWrapper<User> buildUserQueryWrapper() {
        return new QueryWrapper<User>()
                .eq("isDelete", 0)
                .ne("userRole", "ban");
    }

    /**
     * 构建用户增量同步查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<User> buildUserIncrementalQueryWrapper(Date syncTime) {
        return new QueryWrapper<User>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .ne("userRole", "ban");
    }

    /**
     * 构建用户被封禁查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<User> buildUserBannedQueryWrapper(Date syncTime) {
        return new QueryWrapper<User>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("userRole", "ban");
    }

    /**
     * 构建帖子全量同步查询条件
     *
     * @return 查询条件
     */
    private QueryWrapper<Post> buildPostQueryWrapper() {
        return new QueryWrapper<Post>()
                .eq("isDelete", 0)
                .eq("status", 1)
                .eq("isDraft", 0); // 只同步非草稿的帖子
    }

    /**
     * 构建帖子增量同步查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Post> buildPostIncrementalQueryWrapper(Date syncTime) {
        return new QueryWrapper<Post>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("status", 1)
                .eq("isDraft", 0); // 只同步非草稿的帖子
    }

    /**
     * 构建帖子审核未通过查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Post> buildPostUnreviewedQueryWrapper(Date syncTime) {
        return new QueryWrapper<Post>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("status", 0)
                .eq("isDraft", 0);  // 只考虑非草稿的帖子
    }

    /**
     * 构建空间全量同步查询条件
     *
     * @return 查询条件
     */
    private QueryWrapper<Space> buildSpaceQueryWrapper() {
        return new QueryWrapper<Space>()
                .eq("isDelete", 0)
                .eq("spaceType", 1);
    }

    /**
     * 构建空间增量同步查询条件
     *
     * @param syncTime 同步时间阈值
     * @return 查询条件
     */
    private QueryWrapper<Space> buildSpaceIncrementalQueryWrapper(Date syncTime) {
        return new QueryWrapper<Space>()
                .ge("updateTime", syncTime)
                .eq("isDelete", 0)
                .eq("spaceType", 1);
    }

    // ===================== IndexQuery构建器 =====================

    /**
     * 构建图片索引查询
     *
     * @param picture 图片实体
     * @return 索引查询
     */
    private IndexQuery buildPictureIndexQuery(Picture picture) {
        // 将Picture实体转换为EsPicture实体，确保所有字段正确映射到ES
        EsPicture esPicture = new EsPicture();
        BeanUtils.copyProperties(picture, esPicture);
        // 确保viewCount等统计字段被正确复制
        esPicture.setViewCount(picture.getViewCount());
        esPicture.setCommentCount(picture.getCommentCount());
        esPicture.setLikeCount(picture.getLikeCount());
        esPicture.setShareCount(picture.getShareCount());

        return retryOperation(() -> new IndexQueryBuilder()
                        .withId(picture.getId().toString())
                        .withObject(esPicture)
                        .build(),
                "构建图片索引查询", INDEX_NAME_PICTURE);
    }

    /**
     * 构建用户索引查询
     *
     * @param user 用户实体
     * @return 索引查询
     */
    private IndexQuery buildUserIndexQuery(User user) {
        return retryOperation(() -> new IndexQueryBuilder()
                        .withId(user.getId().toString())
                        .withObject(user)
                        .build(),
                "构建用户索引查询", INDEX_NAME_USER);
    }

    /**
     * 构建帖子索引查询
     *
     * @param post 帖子实体
     * @return 索引查询
     */
    private IndexQuery buildPostIndexQuery(Post post) {
        return retryOperation(() -> new IndexQueryBuilder()
                        .withId(post.getId().toString())
                        .withObject(post)
                        .build(),
                "构建帖子索引查询", INDEX_NAME_POST);
    }

    /**
     * 构建空间索引查询
     *
     * @param space 空间实体
     * @return 索引查询
     */
    private IndexQuery buildSpaceIndexQuery(Space space) {
        EsSpace esSpace = new EsSpace();
        BeanUtils.copyProperties(space, esSpace);
        esSpace.setSpaceCover(space.getSpaceCover());
        esSpace.setSpaceDesc(space.getSpaceDesc());

        return retryOperation(() -> new IndexQueryBuilder()
                        .withId(space.getId().toString())
                        .withObject(esSpace)
                        .build(),
                "构建空间索引查询", INDEX_NAME_SPACE);
    }

    // ===================== 索引管理方法 =====================

    /**
     * 删除并重建ES索引以确保最新字段映射
     */
    private void deleteAndRecreateIndices() {
        try {
            // 删除现有索引
            if (elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_PICTURE)).exists()) {
                elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_PICTURE)).delete();
                log.info("已删除ES picture索引");
            }

            if (elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_USER)).exists()) {
                elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_USER)).delete();
                log.info("已删除ES user索引");
            }

            if (elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_POST)).exists()) {
                elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_POST)).delete();
                log.info("已删除ES post索引");
            }

            if (elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_SPACE)).exists()) {
                elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME_SPACE)).delete();
                log.info("已删除ES space索引");
            }

            log.info("ES索引删除成功，将通过全量同步自动重建");
        } catch (Exception e) {
            log.error("删除ES索引失败: " + e.getMessage(), e);
            sendAlertEmail("ES索引删除异常", "删除ES索引失败：\n" + e.getMessage());
        }
    }

    // ===================== 反射工具方法（提取ID） =====================

    /**
     * 提取实体ID（通用方法）
     *
     * @param entity 实体对象
     * @return ID字符串
     */
    private String extractId(Object entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity).toString();
        } catch (Exception e) {
            log.error("提取实体ID失败", e);
            return "未知ID";
        }
    }

    /**
     * 提取实体列表ID（通用方法）
     *
     * @param entityList 实体列表
     * @return ID列表
     */
    private <T> List<String> extractIds(List<T> entityList) {
        return entityList.stream()
                .map(this::extractId)
                .collect(Collectors.toList());
    }

    /**
     * 通用批量处理方法，按批次大小拆分处理并添加批次间休眠
     *
     * @param dataList 待处理数据列表
     * @param batchSize 批次大小
     * @param processor 单批次处理器
     * @param <T> 数据类型
     */
    private <T> void processInBatches(List<T> dataList, int batchSize, java.util.function.Consumer<List<T>> processor) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, dataList.size());
            List<T> batch = dataList.subList(i, endIndex);

            try {
                processor.accept(batch);

                // 批次间短暂休眠，降低ES瞬时压力
                if (i + batchSize < dataList.size()) {
                    Thread.sleep(BATCH_SLEEP_TIME);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("批量处理被中断", e);
                break;
            } catch (Exception e) {
                log.error("处理批次数据失败", e);
                // 继续处理下一个批次
            }
        }
    }

    /**
     * Spring容器销毁钩子，优雅关闭线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("ElasticSearchSyncJob正在关闭，开始清理资源...");

        // 优雅关闭线程池
        if (syncExecutor != null && !syncExecutor.isShutdown()) {
            try {
                // 不接受新任务，等待现有任务完成
                syncExecutor.shutdown();

                // 等待最多30秒让任务完成
                if (!syncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("线程池任务未在30秒内完成，强制关闭");
                    syncExecutor.shutdownNow();

                    // 再等待一段时间让线程真正结束
                    if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("线程池未能正常关闭");
                    }
                }
            } catch (InterruptedException e) {
                log.error("等待线程池关闭时被中断", e);
                syncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("ElasticSearchSyncJob资源清理完成");
    }
}
