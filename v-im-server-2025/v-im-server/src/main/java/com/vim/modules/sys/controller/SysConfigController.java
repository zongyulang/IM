package com.vim.modules.sys.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.modules.sys.result.SysConfig;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vim/server/config")
public class SysConfigController {

    @Resource
    private SysConfig sysConfig;

    /**
     * 获取系统的配置信息
     * @return saResult
     */
    @GetMapping
    @Log(title = "获取系统配置")
    public SaResult get() {
        return SaResult.data(sysConfig);
    }

    /**
     * 获取当前用户的权限列表
     * @return saResult
     */
    @GetMapping(value = "/permission")
    @Log(title = "获取用户权限")
    public SaResult permission() {
        return SaResult.data(StpUtil.getPermissionList());
    }
}
