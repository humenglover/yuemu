package com.lumenglover.yuemupicturebackend.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static final String SEPARATOR = ",";

    /**
     * 获取参数不为空值
     */
    public static String blankToDefault(String str, String defaultValue) {
        return StrUtil.blankToDefault(str, defaultValue);
    }

    /**
     * 判断一个字符串是否为空串
     */
    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }

    /**
     * 判断一个字符串是否为非空串
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 去空格
     */
    public static String trim(String str) {
        return StrUtil.trim(str);
    }

    /**
     * 截取字符串
     */
    public static String substring(final String str, int start) {
        return str == null ? "" : substring(str, start, str.length());
    }

    /**
     * 截取字符串
     */
    public static String substring(final String str, int start, int end) {
        return StrUtil.sub(str, start, end);
    }

    /**
     * 格式化文本, {} 表示占位符
     */
    public static String format(String template, Object... params) {
        return StrUtil.format(template, params);
    }

    /**
     * 是否为http(s)://开头
     */
    public static boolean ishttp(String link) {
        return Validator.isUrl(link);
    }

    /**
     * 字符串转set
     */
    public static Set<String> str2Set(String str, String sep) {
        return new HashSet<>(str2List(str, sep, true, false));
    }

    /**
     * 字符串转list
     */
    public static List<String> str2List(String str, String sep, boolean filterBlank, boolean trim) {
        List<String> list = new ArrayList<>();
        if (isEmpty(str)) {
            return list;
        }
        if (filterBlank && isBlank(str)) {
            return list;
        }
        String[] split = str.split(sep);
        for (String string : split) {
            if (filterBlank && isBlank(string)) {
                continue;
            }
            if (trim) {
                string = trim(string);
            }
            list.add(string);
        }
        return list;
    }

    /**
     * 忽略大小写判断是否包含任意指定字符串
     */
    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences) {
        return StrUtil.containsAnyIgnoreCase(cs, searchCharSequences);
    }

    /**
     * 驼峰转下划线命名
     */
    public static String toUnderScoreCase(String str) {
        return StrUtil.toUnderlineCase(str);
    }

    /**
     * 忽略大小写判断字符串是否在指定数组中
     */
    public static boolean inStringIgnoreCase(String str, String... strs) {
        return StrUtil.equalsAnyIgnoreCase(str, strs);
    }

    /**
     * 下划线大写命名转驼峰
     */
    public static String convertToCamelCase(String name) {
        return StrUtil.upperFirst(StrUtil.toCamelCase(name));
    }

    /**
     * 下划线命名转驼峰
     */
    public static String toCamelCase(String s) {
        return StrUtil.toCamelCase(s);
    }

    /**
     * 判断字符串是否匹配指定规则列表中的任意一个
     */
    public static boolean matches(String str, List<String> strs) {
        if (isEmpty(str) || CollUtil.isEmpty(strs)) {
            return false;
        }
        for (String pattern : strs) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * URL规则匹配
     */
    public static boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }

    /**
     * 数字左边补齐0
     */
    public static String padl(final Number num, final int size) {
        return num == null ? padl("", size, '0') : padl(num.toString(), size, '0');
    }

    /**
     * 字符串左补齐（核心修改：兼容低版本）
     */
    public static String padl(final String s, final int size, final char c) {
        final StringBuilder sb = new StringBuilder(size);
        if (s != null) {
            final int len = s.length();
            if (len <= size) {
                // 替换 String.repeat() 为父类 repeat 方法，支持 Java 8+
                sb.append(repeat(String.valueOf(c), size - len));
                sb.append(s);
            } else {
                return substring(s, len - size, len);
            }
        } else {
            sb.append(repeat(String.valueOf(c), Math.max(0, size)));
        }
        return sb.toString();
    }

    /**
     * 切分字符串(分隔符默认逗号)
     */
    public static List<String> splitList(String str) {
        return splitTo(str, Convert::toStr);
    }

    /**
     * 切分字符串
     */
    public static List<String> splitList(String str, String separator) {
        return splitTo(str, separator, Convert::toStr);
    }

    /**
     * 切分字符串自定义转换(分隔符默认逗号)
     */
    public static <T> List<T> splitTo(String str, Function<? super Object, T> mapper) {
        return splitTo(str, SEPARATOR, mapper);
    }

    /**
     * 切分字符串自定义转换
     */
    public static <T> List<T> splitTo(String str, String separator, Function<? super Object, T> mapper) {
        if (isBlank(str)) {
            return new ArrayList<>(0);
        }
        return StrUtil.split(str, separator)
                .stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .collect(Collectors.toList());
    }

}
