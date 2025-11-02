package com.vim.common.config;

import cn.dev33.satoken.stp.StpInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    // 管理员ID
    private static final String ADMIN_ID = "1";

    /**
     * 根据登录ID和登录类型获取权限列表
     *
     * @param loginId   登录ID
     * @param loginType 登录类型
     * @return 权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId.equals(ADMIN_ID)) {
            return List.of("vim:plugin:oauth2", "vim:server:log");
        }
        return List.of();
    }

    /**
     * 根据登录ID和登录类型获取角色列表
     *
     * @param o 登录ID
     * @param s 登录类型
     * @return 角色列表
     */
    @Override
    public List<String> getRoleList(Object o, String s) {
        return List.of();
    }
}
