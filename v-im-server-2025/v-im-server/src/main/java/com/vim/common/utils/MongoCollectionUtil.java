package com.vim.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MongoDB集合名称工具类
 */
public class MongoCollectionUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 获取按天分表的集合名称
     * @param baseCollectionName 基础集合名称
     * @return 带日期后缀的集合名称
     */
    public static String getDailyCollection(String baseCollectionName) {
        String dateSuffix = LocalDate.now().format(DATE_FORMATTER);
        return baseCollectionName + "-" + dateSuffix;
    }

    public static String getCurrentMonthCollection(String baseCollection) {
        return baseCollection + "-" + LocalDate.now().format(DATE_FORMATTER);
    }
    
    public static String getMonthCollection(String baseCollection, String month) {
        return baseCollection + "-" + month;
    }
}
