package com.vim.modules.login.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.modules.login.param.LoginParam;
import com.vim.modules.login.param.RegisterParam;
import com.vim.modules.login.service.VimLoginService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
/**
 * 登录控制器
 */
@RestController
public class LoginController {

    @Resource
    private VimLoginService vimLoginService;

    /**
     * 生成验证码
     */
    @GetMapping("/captchaImage")
    @Log(title = "获取验证码")
    public SaResult captchaImage() {
        return SaResult.data(vimLoginService.generateCaptcha());
    }

    /**
     * 登录验证
     */
    @PostMapping("/login")
    public SaResult login(@RequestBody LoginParam loginParam) {
        return SaResult.data(vimLoginService.login(loginParam));
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok();
    }

    /**
     * 检查是否登录
     */
    @GetMapping("/checkLogin")
    public SaResult checkLogin() {
        StpUtil.checkLogin();
        return SaResult.ok();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public SaResult register(@RequestBody RegisterParam registerParam) {
        return SaResult.data(vimLoginService.register(registerParam));
    }
}
