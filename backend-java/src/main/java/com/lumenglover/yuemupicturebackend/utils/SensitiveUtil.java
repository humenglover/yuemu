package com.lumenglover.yuemupicturebackend.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author: quequnlong
 * @date: 2025/3/4
 * @description: 敏感词过滤工具类（需要在resource下放置敏感词文件）
 */
@Component
public class SensitiveUtil {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveUtil.class);

    /**
     * 敏感词文件
     */
    private static final String SENSITIVE_WORD = "sensitive-words/sensitive-words.txt";

    /**
     * 替换符
     */
    private static final String REPLACEMENT = "***";

    /**
     * 根节点
     */
    private static final TrieNode ROOT_NODE = new TrieNode();

    @PostConstruct
    public void init() {
        logger.info("开始加载敏感词文件...");
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(SENSITIVE_WORD);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))
        ) {
            int count = 0;
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
                count++;
            }
            logger.info("敏感词加载完成，共加载 {} 个敏感词", count);
        } catch (Exception e) {
            logger.error("加载敏感词文件失败: ", e);
        }
    }

    /**
     * 将一个敏感词添加到前缀树中
     *
     * @param keyword 敏感词
     */
    private void addKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }
        logger.debug("添加敏感词: {}", keyword);
        TrieNode tempNode = ROOT_NODE;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点,进入下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public static String filter(String text) {

        logger.info("开始过滤敏感词，输入内容：{}", text);

        if (StringUtils.isBlank(text)) {
            return text; // 如果输入为空或null，直接返回原值
        }

        //包含img标签则不过滤
        if (text.contains("<img")) {
            logger.info("内容包含img标签，跳过过滤");
            return text;
        }
        // 指针1
        TrieNode tempNode = ROOT_NODE;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);

            // 跳过符号
            if (isSymbol(c)) {
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if (tempNode == ROOT_NODE) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = ROOT_NODE;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = ROOT_NODE;
                logger.info("发现敏感词，已替换为：{}", REPLACEMENT);
            } else {
                // 检查下一个字符
                position++;
            }
        }

        // 将最后一批字符计入结果
        sb.append(text.substring(begin));

        String result = sb.toString();
        logger.info("敏感词过滤完成，输出内容：{}", result);
        return result;
    }

    /**
     * 判断是否为符号
     *
     * @param c 字符
     * @return 判断
     */
    private static boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    public static boolean isAsciiAlpha(char ch) {
        return isAsciiAlphaUpper(ch) || isAsciiAlphaLower(ch);
    }

    public static boolean isAsciiAlphaUpper(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    public static boolean isAsciiAlphaLower(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    public static boolean isAsciiNumeric(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isAsciiAlphanumeric(char ch) {
        return isAsciiAlpha(ch) || isAsciiNumeric(ch);
    }

    /**
     * 前缀树
     */
    private static class TrieNode {

        // 关键词结束标识
        private boolean isKeywordEnd = false;

        // 子节点(key是下级字符,value是下级节点)
        private final Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
