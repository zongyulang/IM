package com.vim.modules.login.service;

import com.vim.modules.login.param.LoginParam;
import com.vim.modules.login.param.RegisterParam;

import java.util.Map;

/**
 * 登录服务接口
 */
public interface VimLoginService {
    /**
     * 用户登录
     *
     * @param loginParam 登录参数
     * @return 登录结果
     */
    Map<String, Object> login(LoginParam loginParam);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 用户注册
     *
     * @param registerParam 注册参数
     * @return 用户ID
     */
    String register(RegisterParam registerParam);

    /**
     * 更新密码
     *
     * @param userId 用户ID
     */
    void handlePasswordUpdate(String userId);

    /**
     * 检查验证码
     *
     * @param uuid 验证码uuid
     * @param code 验证码
     * @return true-验证通过
     */
    boolean validateCaptcha(String uuid, String code);

    /**
     * 生成验证码
     *
     * @return 验证码信息
     */
    Map<String, String> generateCaptcha();
}
