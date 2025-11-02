package com.vim.common.utils;

import cn.dev33.satoken.stp.StpUtil;

public class VimUtil {

    /**
     * 系统通知用户id
     *
     * @return 用户id
     */
    public static String systemId() {
        return "0";
    }

    /**
     * 获取当前用户id
     *
     * @return 用户id
     */
    public static String getLoginId() {
        return StpUtil.getLoginIdAsString();
    }

    /**
     * 根据用户id获取用户
     *
     * @param token token
     * @return json
     */
    public static String getUserIdByToken(String token) {
        return StpUtil.getLoginIdByToken(token).toString();
    }
}
