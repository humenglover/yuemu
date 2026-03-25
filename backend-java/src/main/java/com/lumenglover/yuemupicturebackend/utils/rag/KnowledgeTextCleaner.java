package com.lumenglover.yuemupicturebackend.utils.rag;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.regex.Pattern;

/**
 * 知识库文本轻量清洗工具类
 * 增强版：基于Apache Commons工具类，提升清洗能力和健壮性
 */
public class KnowledgeTextCleaner {

    // 预编译正则（提升性能）
    private static final Pattern PATTERN_CONTROL_CHAR = Pattern.compile("[\\x00-\\x1F\\x7F-\\x9F]");
    private static final Pattern PATTERN_REPEAT_PHRASE_2 = Pattern.compile("(.{2})\\1+");
    private static final Pattern PATTERN_REPEAT_PHRASE_3 = Pattern.compile("(.{3})\\1+");
    private static final Pattern PATTERN_REPEAT_PHRASE_4 = Pattern.compile("(.{4})\\1+");
    private static final Pattern PATTERN_REPEAT_PHRASE_GENERAL = Pattern.compile("(.{2,4})\\1+");
    private static final Pattern PATTERN_MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern PATTERN_INVALID_PUNCT = Pattern.compile("[^\u4e00-\u9fa5a-zA-Z0-9，。！？；：、（）【】《》\"'·,.!?;:\\[\\]()<>\"'\\s]+");
    private static final Pattern PATTERN_CHINESE_SENTENCE_SPLIT = Pattern.compile("[。！？]");
    private static final Pattern PATTERN_MIXED_SENTENCE_SPLIT = Pattern.compile("[。！？.!?]");

    // 编辑距离阈值（用于智能去重）
    private static final int LEVENSHTEIN_THRESHOLD = 2;
    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance();

    /**
     * 增强版清洗知识库文本
     * @param text 原始文本（支持null/空字符串）
     * @return 清洗后的文本
     */
    public static String cleanText(String text) {
        // 1. 空值安全处理（commons-lang3）
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }

        String cleaned = text;

        // 2. 转义HTML/Unicode字符（commons-text）
        cleaned = StringEscapeUtils.unescapeHtml4(cleaned);
        cleaned = StringEscapeUtils.unescapeJava(cleaned);

        // 3. 移除控制字符（预编译正则+commons-lang3）
        cleaned = PATTERN_CONTROL_CHAR.matcher(cleaned).replaceAll(StringUtils.EMPTY);
        // 移除不可见字符（如零宽空格）
        cleaned = StringUtils.strip(cleaned, "\u200B\u200C\u200D\uFEFF");

        // 4. 过滤无效标点：仅保留常用中英文标点
        cleaned = PATTERN_INVALID_PUNCT.matcher(cleaned).replaceAll(StringUtils.EMPTY);

        // 5. 智能去重：先正则去重，再基于编辑距离去重相似短句
        cleaned = removeRepeatedPhrases(cleaned);
        cleaned = removeSimilarPhrases(cleaned);

        // 6. 规范化空白字符（commons-lang3）
        cleaned = PATTERN_MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();
        cleaned = StringUtils.normalizeSpace(cleaned); // 标准化空格（处理全角/半角）

        // 7. 统一标点格式：全角转半角（数字/字母/标点）
        cleaned = toHalfWidth(cleaned);

        // 8. 长度截断：智能按句子分割，优先保留完整语义（增强版）
        cleaned = smartTruncateText(cleaned, 200);

        // 9. 收尾：移除首尾标点/空格（commons-lang3）
        cleaned = StringUtils.strip(cleaned, " ，。！？；：、（）【】《》\"'·,.!?;:\\[\\]()<>\"']");

        return cleaned;
    }

    /**
     * 移除重复短语（增强版：预编译正则+分层去重）
     */
    private static String removeRepeatedPhrases(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        String result = text;
        // 分层移除2-4字重复短语（预编译正则提升性能）
        result = PATTERN_REPEAT_PHRASE_2.matcher(result).replaceAll("$1");
        result = PATTERN_REPEAT_PHRASE_3.matcher(result).replaceAll("$1");
        result = PATTERN_REPEAT_PHRASE_4.matcher(result).replaceAll("$1");
        result = PATTERN_REPEAT_PHRASE_GENERAL.matcher(result).replaceAll("$1");
        return result;
    }

    /**
     * 移除相似短语（基于编辑距离，处理近重复文本）
     */
    private static String removeSimilarPhrases(String text) {
        if (StringUtils.isBlank(text) || text.length() < 5) {
            return text;
        }
        // 按逗号分割短句
        String[] phrases = StringUtils.split(text, "，,");
        if (phrases.length <= 1) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phrases.length; i++) {
            String current = StringUtils.trimToEmpty(phrases[i]);
            if (StringUtils.isBlank(current)) {
                continue;
            }
            // 检查当前短语是否与已保留的短语相似
            boolean isSimilar = false;
            for (int j = 0; j < sb.length(); j++) {
                String existing = StringUtils.substringBeforeLast(sb.toString(), "，");
                if (StringUtils.isBlank(existing)) {
                    continue;
                }
                // 计算编辑距离（commons-text）
                int distance = LEVENSHTEIN_DISTANCE.apply(current, existing);
                if (distance <= LEVENSHTEIN_THRESHOLD) {
                    isSimilar = true;
                    break;
                }
            }
            if (!isSimilar) {
                sb.append(current).append("，");
            }
        }
        // 移除最后一个逗号
        return StringUtils.removeEnd(sb.toString(), "，");
    }

    /**
     * 全角转半角（commons-lang3）
     */
    private static String toHalfWidth(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 12288) { // 全角空格
                chars[i] = 32;
            } else if (chars[i] >= 65281 && chars[i] <= 65374) { // 全角字符（除空格）
                chars[i] = (char) (chars[i] - 65248);
            }
        }
        return new String(chars);
    }

    /**
     * 智能截断文本（增强版：区分中英文句子，优先保留完整语义）
     */
    private static String smartTruncateText(String text, int maxLength) {
        if (StringUtils.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        // 判断是否为纯中文文本
        boolean isChineseOnly = StringUtils.containsOnly(text, '\u4e00', '\u9fa5', '，', '。', '！', '？');

        String[] sentences;
        if (isChineseOnly) {
            sentences = PATTERN_CHINESE_SENTENCE_SPLIT.split(text);
        } else {
            sentences = PATTERN_MIXED_SENTENCE_SPLIT.split(text);
        }

        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            String sentenceWithPunct = sentence + (isChineseOnly ? "。" : ".");
            // 检查长度（commons-lang3）
            if (result.length() + sentenceWithPunct.length() <= maxLength) {
                result.append(sentenceWithPunct);
            } else {
                // 剩余空间不足，截取部分句子
                if (result.length() < maxLength) {
                    int remainLength = maxLength - result.length();
                    result.append(StringUtils.substring(sentence, 0, remainLength));
                }
                break;
            }
        }
        // 确保最终长度不超限
        return StringUtils.substring(result.toString(), 0, maxLength);
    }
}
