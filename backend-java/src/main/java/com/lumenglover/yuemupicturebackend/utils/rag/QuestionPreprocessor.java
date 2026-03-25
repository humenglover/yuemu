package com.lumenglover.yuemupicturebackend.utils.rag;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 用户问题预处理工具类
 * 适配悦木图片管理系统RAG智能客服场景，提供标准化、清洗、同义词扩展、关键词提取等能力
 */
public class QuestionPreprocessor {

    // 特殊字符过滤正则
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[\\p{Punct}\\p{Space}]+");
    // 语气词/无意义词汇过滤
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "啊", "呀", "呢", "哦", "吧", "嘛", "咯", "哈", "喂", "嗯", "呃",
            "的", "了", "着", "过", "也", "就", "都", "很", "非常", "特别", "请问", "想问问", "怎么", "如何"
    ));
    // 问题模块分类关键词
    private static final Map<String, List<String>> QUESTION_MODULES = new HashMap<>();
    // 错别字映射表（补充悦木业务专属）
    private static final Map<String, String> TYPO_MAP = new HashMap<>();
    // 同义词映射表（补充悦木业务专属）
    private static final Map<String, String[]> SYNONYM_MAP = new HashMap<>();

    static {
        initTypoMap();
        initSynonymMap();
        initQuestionModules();
    }

    /**
     * 初始化业务专属错别字映射
     */
    private static void initTypoMap() {
        // 基础通用错别字
        TYPO_MAP.put("悦木", "悦木");
        TYPO_MAP.put("图册", "相册");
        TYPO_MAP.put("图象", "图像");
        TYPO_MAP.put("相片", "照片");
        TYPO_MAP.put("登陆", "登录");
        TYPO_MAP.put("帐号", "账号");
        TYPO_MAP.put("密碼", "密码");
        TYPO_MAP.put("刪除", "删除");
        TYPO_MAP.put("編輯", "编辑");
        TYPO_MAP.put("備份", "备份");
        TYPO_MAP.put("還原", "恢复");
        TYPO_MAP.put("同步", "同步");
        TYPO_MAP.put("緩存", "缓存");
        // 业务专属错别字
        TYPO_MAP.put("爱情广场", "爱情广场");
        TYPO_MAP.put("时光相冊", "时光相册");
        TYPO_MAP.put("树洞功能", "树洞功能");
        TYPO_MAP.put("便签墙", "便签墙");
        TYPO_MAP.put("学习打卡", "学习打卡");
        TYPO_MAP.put("音乐专辑", "音乐专辑");
        TYPO_MAP.put("空间权限", "空间权限");
        TYPO_MAP.put("消息免打扰", "消息免打扰");
        TYPO_MAP.put("内容审核", "内容审核");
        TYPO_MAP.put("智能推荐", "智能推荐");
        TYPO_MAP.put("数据备份", "数据备份");
        TYPO_MAP.put("批量导出", "批量导出");
        TYPO_MAP.put("断点续传", "断点续传");
        TYPO_MAP.put("CDN加速", "CDN加速");
    }

    /**
     * 初始化业务专属同义词映射
     */
    private static void initSynonymMap() {
        // 系统功能核心同义词
        SYNONYM_MAP.put("登录", new String[]{"登录", "登陆", "登入", "进入系统", "访问账号"});
        SYNONYM_MAP.put("注册", new String[]{"注册", "创建账号", "开通账号", "申请账号"});
        SYNONYM_MAP.put("上传", new String[]{"上传", "发布", "提交", "传图", "上传图片"});
        SYNONYM_MAP.put("下载", new String[]{"下载", "导出", "保存到本地", "下载图片"});
        SYNONYM_MAP.put("删除", new String[]{"删除", "移除", "清除", "删掉", "移除内容"});
        SYNONYM_MAP.put("相册", new String[]{"相册", "图册", "图片库", "相片集", "图库"});
        SYNONYM_MAP.put("空间", new String[]{"空间", "圈子", "专属空间", "私密空间", "公开空间", "团队空间"});
        SYNONYM_MAP.put("备份", new String[]{"备份", "导出", "保存", "数据备份", "自动备份"});
        SYNONYM_MAP.put("恢复", new String[]{"恢复", "还原", "找回", "恢复数据", "还原图片"});
        // 业务特色功能同义词
        SYNONYM_MAP.put("爱情广场", new String[]{"爱情广场", "情侣空间", "情侣相册", "恋爱日记"});
        SYNONYM_MAP.put("时光相册", new String[]{"时光相册", "时间线相册", "成长相册", "旅行相册"});
        SYNONYM_MAP.put("树洞", new String[]{"树洞", "匿名发布", "匿名倾诉", "私密吐槽"});
        SYNONYM_MAP.put("便签墙", new String[]{"便签墙", "便签发布", "短文本发布", "心情笔记"});
        SYNONYM_MAP.put("学习打卡", new String[]{"学习打卡", "打卡小组", "学习记录", "打卡提醒"});
        SYNONYM_MAP.put("音乐专辑", new String[]{"音乐专辑", "歌单", "情侣歌单", "音乐收藏"});
        SYNONYM_MAP.put("消息免打扰", new String[]{"免打扰", "消息静音", "关闭通知", "屏蔽消息"});
        SYNONYM_MAP.put("内容审核", new String[]{"审核", "内容检查", "违规检测", "内容安全"});
        SYNONYM_MAP.put("智能推荐", new String[]{"推荐", "个性化推荐", "兴趣推荐", "内容推荐"});
        SYNONYM_MAP.put("断点续传", new String[]{"断点续传", "续传", "暂停后继续上传", "分片上传"});
    }

    /**
     * 初始化问题模块分类（用于快速定位问题所属业务域）
     */
    private static void initQuestionModules() {
        QUESTION_MODULES.put("账号管理", Arrays.asList("登录", "注册", "密码", "找回", "冻结", "注销", "账号安全"));
        QUESTION_MODULES.put("图片管理", Arrays.asList("上传", "下载", "删除", "相册", "导出", "备份", "恢复", "回收站"));
        QUESTION_MODULES.put("空间管理", Arrays.asList("空间", "创建空间", "加入空间", "成员管理", "权限", "删除空间"));
        QUESTION_MODULES.put("互动沟通", Arrays.asList("聊天", "私聊", "群聊", "评论", "点赞", "分享", "消息", "免打扰"));
        QUESTION_MODULES.put("特色功能", Arrays.asList("爱情广场", "时光相册", "树洞", "便签墙", "学习打卡", "音乐专辑"));
        QUESTION_MODULES.put("系统设置", Arrays.asList("设置", "隐私", "通知", "推送", "备份", "无障碍", "数据统计"));
        QUESTION_MODULES.put("客服支持", Arrays.asList("客服", "人工", "反馈", "举报", "投诉", "帮助"));
    }

    /**
     * 基础文本清洗：过滤特殊字符、转义字符、冗余空格、无意义语气词
     * @param question 原始问题
     * @return 清洗后的文本
     */
    public static String cleanText(String question) {
        if (StrUtil.isBlank(question)) {
            return StringUtils.EMPTY;
        }

        // 1. 转义HTML字符
        String cleaned = StringEscapeUtils.unescapeHtml4(question);
        // 2. 去除特殊字符（保留中文、英文、数字）
        cleaned = SPECIAL_CHAR_PATTERN.matcher(cleaned).replaceAll(" ");
        // 3. 去除冗余空格
        cleaned = StrUtil.trim(cleaned.replaceAll("\\s+", " "));
        // 4. 过滤无意义语气词
        String[] words = cleaned.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!STOP_WORDS.contains(word) && StrUtil.isNotBlank(word)) {
                sb.append(word).append(" ");
            }
        }
        // 5. 最终去重空格并返回
        return StrUtil.trim(sb.toString());
    }

    /**
     * 纠正常见错别字、标准化核心词汇
     * @param question 清洗后的问题
     * @return 标准化后的问题
     */
    public static String normalizeQuestion(String question) {
        if (StrUtil.isBlank(question)) {
            return question;
        }

        String normalized = question;
        // 替换错别字
        for (Map.Entry<String, String> entry : TYPO_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                normalized = normalized.replace(entry.getKey(), entry.getValue());
            }
        }

        return normalized;
    }

    /**
     * 扩展检索词同义词（适配Elasticsearch检索语法）
     * @param question 标准化后的问题
     * @return 包含同义词扩展的检索语句
     */
    public static String expandQuerySynonyms(String question) {
        if (StrUtil.isBlank(question)) {
            return question;
        }

        String expanded = question;
        // 按长度倒序处理（避免短词覆盖长词，如"爱情广场"优先于"广场"）
        List<Map.Entry<String, String[]>> sortedEntries = new ArrayList<>(SYNONYM_MAP.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()));

        for (Map.Entry<String, String[]> entry : sortedEntries) {
            String keyword = entry.getKey();
            String[] synonyms = entry.getValue();

            if (expanded.contains(keyword)) {
                // 构建ES检索语法：(keyword OR synonym1 OR synonym2)
                StringBuilder synonymQuery = new StringBuilder();
                synonymQuery.append("(");
                Set<String> uniqueSynonyms = new LinkedHashSet<>(Arrays.asList(synonyms)); // 去重
                Iterator<String> iterator = uniqueSynonyms.iterator();
                while (iterator.hasNext()) {
                    synonymQuery.append(iterator.next());
                    if (iterator.hasNext()) {
                        synonymQuery.append(" OR ");
                    }
                }
                synonymQuery.append(")");
                // 替换原关键词
                expanded = expanded.replace(keyword, synonymQuery.toString());
            }
        }

        return expanded;
    }

    /**
     * 提取问题核心关键词（用于ES精准检索）
     * @param question 标准化后的问题
     * @return 核心关键词列表
     */
    public static List<String> extractKeywords(String question) {
        if (StrUtil.isBlank(question)) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();
        // 1. 提取同义词映射中的核心关键词
        for (String coreWord : SYNONYM_MAP.keySet()) {
            if (question.contains(coreWord) && !keywords.contains(coreWord)) {
                keywords.add(coreWord);
            }
        }
        // 2. 补充基础关键词（无同义词的核心业务词）
        String[] baseWords = {"图片", "用户", "权限", "审核", "推荐", "备份", "导出", "导入", "通知", "设置"};
        for (String word : baseWords) {
            if (question.contains(word) && !keywords.contains(word)) {
                keywords.add(word);
            }
        }
        // 3. 若未提取到关键词，按空格拆分取长度≥2的词
        if (keywords.isEmpty()) {
            String[] words = question.split(" ");
            for (String word : words) {
                if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                    keywords.add(word);
                }
            }
        }

        return keywords;
    }

    /**
     * 识别问题所属业务模块
     * @param question 标准化后的问题
     * @return 模块名称（如"账号管理"、"图片管理"）
     */
    public static String identifyQuestionModule(String question) {
        if (StrUtil.isBlank(question)) {
            return "未知模块";
        }

        Map<String, Integer> moduleScore = new HashMap<>();
        // 计算每个模块的匹配得分
        for (Map.Entry<String, List<String>> entry : QUESTION_MODULES.entrySet()) {
            String module = entry.getKey();
            List<String> moduleKeywords = entry.getValue();
            int score = 0;
            for (String keyword : moduleKeywords) {
                if (question.contains(keyword)) {
                    score++;
                }
            }
            if (score > 0) {
                moduleScore.put(module, score);
            }
        }

        // 返回得分最高的模块
        if (moduleScore.isEmpty()) {
            return "未知模块";
        }
        return Collections.max(moduleScore.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * 提取问题关键词（按用户期望格式）：移除停用词，返回关键词列表
     * @param question 原始问题
     * @return 关键词列表
     */
    public static List<String> extractCleanKeywords(String question) {
        if (StrUtil.isBlank(question)) {
            return new ArrayList<>();
        }

        // 1. 转义HTML字符
        String cleaned = StringEscapeUtils.unescapeHtml4(question);
        // 2. 去除特殊字符（保留中文、英文、数字）
        cleaned = SPECIAL_CHAR_PATTERN.matcher(cleaned).replaceAll(" ");
        // 3. 去除冗余空格
        cleaned = StrUtil.trim(cleaned.replaceAll("\\s+", " "));
        // 4. 按空格分割成词
        String[] words = cleaned.split(" ");

        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            // 过滤停用词和空字符串
            if (StrUtil.isNotBlank(word) && !STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * 完整预处理流程：清洗 → 标准化 → 同义词扩展 → 关键词提取 → 模块识别
     * @param question 原始用户问题
     * @return 预处理结果封装
     */
    public static PreprocessResult preprocess(String question) {
        // 1. 基础文本清洗
        String cleanedText = cleanText(question);
        // 2. 错别字纠正与标准化
        String normalizedText = normalizeQuestion(cleanedText);
        // 3. 同义词扩展（用于ES检索）
        String expandedText = expandQuerySynonyms(normalizedText);
        // 4. 提取核心关键词
        List<String> keywords = extractKeywords(normalizedText);
        // 5. 识别问题模块
        String module = identifyQuestionModule(normalizedText);

        return new PreprocessResult(cleanedText, normalizedText, expandedText, keywords, module);
    }

    /**
     * 预处理结果封装类
     */
    public static class PreprocessResult {
        // 清洗后的文本
        private final String cleanedText;
        // 标准化后的文本
        private final String normalizedText;
        // 同义词扩展后的检索文本
        private final String expandedText;
        // 核心关键词列表
        private final List<String> keywords;
        // 问题所属模块
        private final String module;

        public PreprocessResult(String cleanedText, String normalizedText, String expandedText,
                                List<String> keywords, String module) {
            this.cleanedText = cleanedText;
            this.normalizedText = normalizedText;
            this.expandedText = expandedText;
            this.keywords = keywords;
            this.module = module;
        }

        // Getter方法
        public String getCleanedText() {
            return cleanedText;
        }

        public String getNormalizedText() {
            return normalizedText;
        }

        public String getExpandedText() {
            return expandedText;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public String getModule() {
            return module;
        }

        @Override
        public String toString() {
            return "PreprocessResult{" +
                    "cleanedText='" + cleanedText + '\'' +
                    ", normalizedText='" + normalizedText + '\'' +
                    ", expandedText='" + expandedText + '\'' +
                    ", keywords=" + keywords +
                    ", module='" + module + '\'' +
                    '}';
        }
    }
}
