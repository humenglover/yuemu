package com.lumenglover.yuemupicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.constant.RedisConstant;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.mapper.PostMapper;
import com.lumenglover.yuemupicturebackend.model.vo.DashboardVO;
import com.lumenglover.yuemupicturebackend.model.vo.ChartVO;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.mapper.ReportMapper;
import com.lumenglover.yuemupicturebackend.mapper.AudioFileMapper;
import com.lumenglover.yuemupicturebackend.service.BugReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;

/**
 * 数据看板接口
 */
@RestController
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private SpaceService spaceService;

    @Resource
    private ActivityService activityService;

    @Resource
    private LoveBoardService loveBoardService;

    @Resource
    private FriendLinkService friendLinkService;

    @Resource
    private MessageService messageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AudioFileService audioFileService;

    @Resource
    private ReportService reportService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private BugReportService bugReportService;

    /**
     * 获取数据看板统计信息
     */
    @GetMapping("/stats")
    public BaseResponse<DashboardVO> getDashboardStats() {
        DashboardVO dashboardVO = new DashboardVO();

        // 获取当天日期字符串，格式为 "yyyy-MM-dd"
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // 查询当天新增用户数
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        userQueryWrapper.eq("isDelete", 0);
        Long newUsers = userService.count(userQueryWrapper);
        dashboardVO.setNewUsers(newUsers);

        // 查询当天新增图片数
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        pictureQueryWrapper.eq("isDelete", 0);
        Long newPictures = pictureService.count(pictureQueryWrapper);
        dashboardVO.setNewPictures(newPictures);

        // 查询当天新增帖子数
        QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
        postQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        postQueryWrapper.eq("isDelete", 0);
        Long newPosts = postService.count(postQueryWrapper);
        dashboardVO.setNewPosts(newPosts);

        // 查询当天新增空间数
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        spaceQueryWrapper.eq("isDelete", 0);
        Long newSpaces = spaceService.count(spaceQueryWrapper);
        dashboardVO.setNewSpaces(newSpaces);

        // 查询当天新增活动数
        QueryWrapper<Activity> activityQueryWrapper = new QueryWrapper<>();
        activityQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        activityQueryWrapper.eq("isDelete", 0);
        Long newActivities = activityService.count(activityQueryWrapper);
        dashboardVO.setNewActivities(newActivities);

        // 查询当天新增恋爱空间数
        QueryWrapper<LoveBoard> loveBoardQueryWrapper = new QueryWrapper<>();
        loveBoardQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        loveBoardQueryWrapper.eq("isDelete", 0);
        Long newLoveBoards = loveBoardService.count(loveBoardQueryWrapper);
        dashboardVO.setNewLoveBoards(newLoveBoards);

        // 查询当天新增友链数
        QueryWrapper<FriendLink> friendLinkQueryWrapper = new QueryWrapper<>();
        friendLinkQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        friendLinkQueryWrapper.eq("isDelete", 0);
        Long newFriendLinks = friendLinkService.count(friendLinkQueryWrapper);
        dashboardVO.setNewFriendLinks(newFriendLinks);

        // 查询当天新增留言板数
        QueryWrapper<Message> messageQueryWrapper = new QueryWrapper<>();
        messageQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        messageQueryWrapper.eq("isDelete", 0);
        Long newMessages = messageService.count(messageQueryWrapper);
        dashboardVO.setNewMessages(newMessages);

        // 查询当天新增音频数
        QueryWrapper<AudioFile> audioTodayQueryWrapper = new QueryWrapper<>();
        audioTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        audioTodayQueryWrapper.eq("isDelete", 0);
        Long newAudioFiles = audioFileService.count(audioTodayQueryWrapper);
        dashboardVO.setNewAudioFiles(newAudioFiles);

        // 查询当天新增举报数
        QueryWrapper<Report> reportTodayQueryWrapper = new QueryWrapper<>();
        reportTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        reportTodayQueryWrapper.eq("isDelete", 0);
        Long newReports = reportService.count(reportTodayQueryWrapper);
        dashboardVO.setNewReports(newReports);

        // 查询当天新增恋爱板数
        QueryWrapper<LoveBoard> loveBoardTodayQueryWrapper = new QueryWrapper<>();
        loveBoardTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        loveBoardTodayQueryWrapper.eq("isDelete", 0);
        Long newLoveBoardsToday = loveBoardService.count(loveBoardTodayQueryWrapper);
        dashboardVO.setNewLoveBoards(newLoveBoardsToday);

        // 查询当天新增友链数
        QueryWrapper<FriendLink> friendLinkTodayQueryWrapper = new QueryWrapper<>();
        friendLinkTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        friendLinkTodayQueryWrapper.eq("isDelete", 0);
        Long newFriendLinksToday = friendLinkService.count(friendLinkTodayQueryWrapper);
        dashboardVO.setNewFriendLinks(newFriendLinksToday);

        // 查询当天新增会话数
        QueryWrapper<ChatMessage> chatMessageTodayQueryWrapper = new QueryWrapper<>();
        chatMessageTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        chatMessageTodayQueryWrapper.eq("isDelete", 0);
        Long newChatMessages = chatMessageService.count(chatMessageTodayQueryWrapper);
        dashboardVO.setNewChatMessages(newChatMessages);

        // 查询当天新增bug报告数
        QueryWrapper<BugReport> bugReportTodayQueryWrapper = new QueryWrapper<>();
        bugReportTodayQueryWrapper.apply("DATE(createTime) = {0}", todayStr);
        bugReportTodayQueryWrapper.eq("isDelete", 0);
        Long newBugReports = bugReportService.count(bugReportTodayQueryWrapper);
        dashboardVO.setNewBugReports(newBugReports);

        // 查询bug报告总数
        QueryWrapper<BugReport> bugReportTotalQueryWrapper = new QueryWrapper<>();
        bugReportTotalQueryWrapper.eq("isDelete", 0);
        Long totalBugReports = bugReportService.count(bugReportTotalQueryWrapper);
        dashboardVO.setTotalBugReports(totalBugReports);

        // 获取网站总访问量（从Redis中获取）
        String totalViewsStr = stringRedisTemplate.opsForValue().get(RedisConstant.TOTAL_VIEW_COUNT_KEY);
        Long totalViews = StrUtil.isBlank(totalViewsStr) ? 0L : Long.valueOf(totalViewsStr);
        dashboardVO.setTotalViews(totalViews);

        return ResultUtils.success(dashboardVO);
    }

    /**
     * 获取图表数据
     */
    @GetMapping("/charts")
    public BaseResponse<ChartVO> getChartStats() {
        ChartVO chartVO = new ChartVO();
        // 雷达图数据 - 用户信息
        ChartVO.RadarChartData radarChartData = new ChartVO.RadarChartData();
        List<String> indicators = new ArrayList<>();
        indicators.add("总用户数");
        indicators.add("今日新增");
        indicators.add("本月新增");
        indicators.add("总活跃");
        indicators.add("总封禁");
        radarChartData.setIndicator(indicators);

        // 获取用户统计数据
        long totalUsers = userService.count();
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        QueryWrapper<User> todayQuery = new QueryWrapper<>();
        todayQuery.apply("DATE(createTime) = {0}", todayStr);
        todayQuery.eq("isDelete", 0);
        long todayNewUsers = userService.count(todayQuery);

        String monthStr = new SimpleDateFormat("yyyy-MM").format(new Date());
        QueryWrapper<User> monthQuery = new QueryWrapper<>();
        monthQuery.apply("DATE_FORMAT(createTime, '%Y-%m') = {0}", monthStr);
        monthQuery.eq("isDelete", 0);
        long monthNewUsers = userService.count(monthQuery);

        QueryWrapper<User> activeQuery = new QueryWrapper<>();
        activeQuery.gt("lastActiveTime", new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)); // 7天内活跃
        activeQuery.eq("isDelete", 0);
        long activeUsers = userService.count(activeQuery);

        QueryWrapper<User> banQuery = new QueryWrapper<>();
        banQuery.eq("userRole", "ban");
        banQuery.eq("isDelete", 0);
        long bannedUsers = userService.count(banQuery);

        List<Map<String, Object>> radarData = new ArrayList<>();
        Map<String, Object> radarItem = new HashMap<>();
        radarItem.put("name", "用户统计");
        radarItem.put("value", new Long[]{totalUsers, todayNewUsers, monthNewUsers, activeUsers, bannedUsers});
        radarData.add(radarItem);
        radarChartData.setData(radarData);
        chartVO.setRadarChartData(radarChartData);


        // 饼图数据 - 图片信息
        ChartVO.PieChartData pieChartData = new ChartVO.PieChartData();

        // 查询本月图片分类统计
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);
        queryWrapper.isNotNull("category");
        // 限制为本月创建的图片
        String pictureMonthStr = new SimpleDateFormat("yyyy-MM").format(new Date());
        queryWrapper.apply("DATE_FORMAT(createTime, '%Y-%m') = {0}", pictureMonthStr);

        List<Picture> pictureList = pictureService.list(queryWrapper);

        // 统计各分类的数量
        Map<String, Integer> categoryCountMap = new HashMap<>();
        for (Picture picture : pictureList) {
            String category = picture.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                categoryCountMap.put(category, categoryCountMap.getOrDefault(category, 0) + 1);
            }
        }

        // 按数量排序并取前5个分类
        List<Map.Entry<String, Integer>> sortedEntries = categoryCountMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            labels.add(entry.getKey());
            values.add(entry.getValue());
        }

        // 如果不足5个分类，用默认分类填充
        if (labels.size() < 5) {
            List<String> defaultCategories = java.util.Arrays.asList("风景", "人物", "动物", "静物", "其他");
            for (String defaultCategory : defaultCategories) {
                if (!labels.contains(defaultCategory)) {
                    labels.add(defaultCategory);
                    values.add(0);
                    if (labels.size() >= 5) {
                        break;
                    }
                }
            }
        }

        pieChartData.setLabels(labels);
        pieChartData.setValues(values);
        chartVO.setPieChartData(pieChartData);

        // 堆叠柱状图数据 - 帖子信息
        ChartVO.StackedBarChartData stackedBarChartData = new ChartVO.StackedBarChartData();
        List<String> xAxisData = new ArrayList<>();

        // 设置X轴标签为最近7天的实际日期
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        Calendar cal = Calendar.getInstance();

        // 先将日期调整到7天前
        cal.add(Calendar.DAY_OF_MONTH, -7);

        // 添加7天的日期到xAxisData
        for (int i = 0; i < 7; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);  // 每次加一天
            xAxisData.add(sdf.format(cal.getTime()));
        }

        stackedBarChartData.setXAxisData(xAxisData);

        // 计算最近7天的帖子状态数据，使用相对时间查询
        Integer[] publishedData = new Integer[7];
        Integer[] pendingData = new Integer[7];
        Integer[] rejectedData = new Integer[7];

        // 直接查询最近7天内的数据，然后按天统计
        // 查询已发布帖子（最近7天）
        QueryWrapper<Post> publishedQuery = new QueryWrapper<>();
        publishedQuery.eq("status", 1).eq("isDelete", 0);
        publishedQuery.ge("createTime", new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)); // 7天前
        List<Post> publishedPosts = postMapper.selectList(publishedQuery);
        // 按天统计
        Map<Integer, Integer> publishedCountByDay = new HashMap<>();
        for (Post post : publishedPosts) {
            int dayOffset = (int) ((System.currentTimeMillis() - post.getCreateTime().getTime()) / (24 * 60 * 60 * 1000));
            if (dayOffset >= 0 && dayOffset < 7) { // 确保在7天范围内
                int dayIndex = 6 - dayOffset; // 使最近的一天在右边
                publishedCountByDay.put(dayIndex, publishedCountByDay.getOrDefault(dayIndex, 0) + 1);
            }
        }
        for (int i = 0; i < 7; i++) {
            publishedData[i] = publishedCountByDay.getOrDefault(i, 0);
        }

        // 查询审核中帖子（最近7天）
        QueryWrapper<Post> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("status", 0).eq("isDelete", 0);
        pendingQuery.ge("createTime", new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)); // 7天前
        List<Post> pendingPosts = postMapper.selectList(pendingQuery);
        // 按天统计
        Map<Integer, Integer> pendingCountByDay = new HashMap<>();
        for (Post post : pendingPosts) {
            int dayOffset = (int) ((System.currentTimeMillis() - post.getCreateTime().getTime()) / (24 * 60 * 60 * 1000));
            if (dayOffset >= 0 && dayOffset < 7) { // 确保在7天范围内
                int dayIndex = 6 - dayOffset; // 使最近的一天在右边
                pendingCountByDay.put(dayIndex, pendingCountByDay.getOrDefault(dayIndex, 0) + 1);
            }
        }
        for (int i = 0; i < 7; i++) {
            pendingData[i] = pendingCountByDay.getOrDefault(i, 0);
        }

        // 查询已拒绝帖子（最近7天）
        QueryWrapper<Post> rejectedQuery = new QueryWrapper<>();
        rejectedQuery.in("status", 2, 3).eq("isDelete", 0);
        rejectedQuery.ge("createTime", new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)); // 7天前
        List<Post> rejectedPosts = postMapper.selectList(rejectedQuery);
        // 按天统计
        Map<Integer, Integer> rejectedCountByDay = new HashMap<>();
        for (Post post : rejectedPosts) {
            int dayOffset = (int) ((System.currentTimeMillis() - post.getCreateTime().getTime()) / (24 * 60 * 60 * 1000));
            if (dayOffset >= 0 && dayOffset < 7) { // 确保在7天范围内
                int dayIndex = 6 - dayOffset; // 使最近的一天在右边
                rejectedCountByDay.put(dayIndex, rejectedCountByDay.getOrDefault(dayIndex, 0) + 1);
            }
        }
        for (int i = 0; i < 7; i++) {
            rejectedData[i] = rejectedCountByDay.getOrDefault(i, 0);
        }

        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> series1 = new HashMap<>();
        series1.put("name", "已发布");
        series1.put("type", "bar");
        series1.put("data", publishedData);
        series.add(series1);

        Map<String, Object> series2 = new HashMap<>();
        series2.put("name", "审核中");
        series2.put("type", "bar");
        series2.put("data", pendingData);
        series.add(series2);

        Map<String, Object> series3 = new HashMap<>();
        series3.put("name", "未通过");
        series3.put("type", "bar");
        series3.put("data", rejectedData);
        series.add(series3);

        stackedBarChartData.setSeries(series);
        chartVO.setStackedBarChartData(stackedBarChartData);

        // 面积图数据 - 最近7天新增空间变化趋势
        ChartVO.AreaChartData areaChartData = new ChartVO.AreaChartData();
        List<String> areaXAxisData = new ArrayList<>();

        // 设置面积图X轴为最近7天的日期
        SimpleDateFormat areaSdf = new SimpleDateFormat("MM-dd");
        Calendar areaCal = Calendar.getInstance();

        // 先将日期调整到7天前
        areaCal.add(Calendar.DAY_OF_MONTH, -7);

        // 添加7天的日期到areaXAxisData
        for (int i = 0; i < 7; i++) {
            areaCal.add(Calendar.DAY_OF_MONTH, 1);  // 每次加一天
            areaXAxisData.add(areaSdf.format(areaCal.getTime()));
        }

        areaChartData.setXAxisData(areaXAxisData);

        // 查询最近7天每天新增空间数量
        Integer[] areaData = new Integer[7];
        for (int i = 0; i < 7; i++) {
            // 计算每一天的开始和结束时间
            Calendar startCal = Calendar.getInstance();
            startCal.add(Calendar.DAY_OF_MONTH, -(6 - i)); // 从7天前开始计算
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.add(Calendar.DAY_OF_MONTH, -(6 - i));
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);
            endCal.set(Calendar.MILLISECOND, 999);

            Date startDate = startCal.getTime();
            Date endDate = endCal.getTime();

            QueryWrapper<Space> spaceQuery = new QueryWrapper<>();
            spaceQuery.eq("isDelete", 0);
            spaceQuery.between("createTime", startDate, endDate);
            long count = spaceService.count(spaceQuery);
            areaData[i] = Math.toIntExact(count);
        }

        List<Map<String, Object>> areaSeries = new ArrayList<>();
        Map<String, Object> areaSeriesItem = new HashMap<>();
        areaSeriesItem.put("name", "新增空间数");
        areaSeriesItem.put("type", "line");
        areaSeriesItem.put("smooth", true); // 平滑曲线
        areaSeriesItem.put("areaStyle", new HashMap<>()); // 启用面积样式
        areaSeriesItem.put("data", areaData);
        areaSeries.add(areaSeriesItem);

        areaChartData.setSeries(areaSeries);
        chartVO.setAreaChartData(areaChartData);

        return ResultUtils.success(chartVO);
    }
}
