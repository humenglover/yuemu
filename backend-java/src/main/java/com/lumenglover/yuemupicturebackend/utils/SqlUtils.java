package com.lumenglover.yuemupicturebackend.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * SQL 工具
 */
public class SqlUtils {

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     * @param sortField 排序字段
     * @return 是否合法
     */
    public static boolean validSortField(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return false;
        }
        // 只允许字母、数字和下划线
        return sortField.matches("^[a-zA-Z0-9_]+$");
    }
}
