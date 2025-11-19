package com.vim.webpage.Utils;

/**
 * 多语言工具类
 */
public class multi_language {

    /**
     * 处理语言参数
     * 将语言代码转换为小写并移除连字符，默认语言为中文(zhcn)
     * 如果是中文，则返回空字符串
     * 
     * @param lang 原始语言参数，可以为null
     * @return 处理后的语言代码，如果是中文则返回空字符串
     * @throws IllegalArgumentException 如果语言参数为 "undefined"
     */
    public static String processLanguage(String lang) {
        // 默认语言为中文
        if (lang == null || lang.trim().isEmpty()) {
            lang = "zhcn";
        } else {
            // 转换为小写并移除所有连字符
            lang = lang.toLowerCase().replace("-", "");
        }

        // 检查是否为 "undefined" 字符串
        if ("undefined".equals(lang)) {
            throw new IllegalArgumentException("Language parameter is undefined");
        }

        // 如果是中文，返回空字符串
        if ("zhcn".equals(lang)) {
            return "";
        }

        return lang;
    }

    /**
     * 处理语言参数（带默认值版本）
     * 
     * @param lang        原始语言参数，可以为null
     * @param defaultLang 默认语言代码，如果为null则使用"zhcn"
     * @return 处理后的语言代码
     */
    public static String processLanguage(String lang, String defaultLang) {
        if (defaultLang == null) {
            defaultLang = "zhcn";
        }

        if (lang == null || lang.trim().isEmpty()) {
            lang = defaultLang;
        } else {
            lang = lang.toLowerCase().replace("-", "");
        }

        if ("undefined".equals(lang)) {
            throw new IllegalArgumentException("Language parameter is undefined");
        }

        if ("zhcn".equals(lang)) {
            return "";
        }

        return lang;
    }
}
