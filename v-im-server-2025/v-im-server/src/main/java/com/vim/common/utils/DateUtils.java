package com.vim.common.utils;

import cn.hutool.core.date.DateUtil;

import java.util.Date;

public class DateUtils {

    public static String getTime() {
        return DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
    }
}
