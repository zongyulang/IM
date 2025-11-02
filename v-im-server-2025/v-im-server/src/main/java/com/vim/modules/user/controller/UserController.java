package com.vim.modules.user.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.bridge.VimBridgeService;
import com.vim.common.enums.BusinessType;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.common.utils.VimUtil;
import com.vim.modules.login.param.PwdParam;
import com.vim.modules.login.service.VimLoginService;
import com.vim.modules.setting.result.Setting;
import com.vim.modules.user.result.User;
import com.vim.sdk.service.VimSettingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vim/server/users")
public class UserController {

    @Resource
    private VimSettingService vimSettingService;

    @Resource
    private VimBridgeService vimBridgeService;

    @Resource
    private VimLoginService vimLoginService;

    /**
     * 获取用户
     *
     * @param userId 用户id
     * @return boolean
     */
    @GetMapping("{userId}")
    @Log(title = "获取用户信息")
    public SaResult get(@PathVariable String userId) {
        User user = vimBridgeService.getUserById(userId);
        Setting setting = vimSettingService.getByUserId(Long.parseLong(userId));
        if (DictSwitchEnum.NO.getCode().equals(setting.getShowMobile())) {
            user.setMobile("***");
        }
        if (DictSwitchEnum.NO.getCode().equals(setting.getShowEmail())) {
            user.setEmail("***");
        }
        return SaResult.data(user);
    }

    /**
     * 获取当前用户
     *
     * @return boolean
     */
    @GetMapping("my")
    @Log(title = "获取当前用户信息")
    public SaResult my() {
        return SaResult.data(vimBridgeService.getUserById(VimUtil.getLoginId()));
    }

    /**
     * 搜索用户
     *
     * @param mobile 手机号或者用户名
     * @return boolean
     */
    @GetMapping("search")
    @Log(title = "搜索用户")
    public SaResult search(String mobile) {
        List<User> users = vimBridgeService.searchUser(mobile);
        return SaResult.data(users);
    }

    /**
     * 更新用户
     *
     * @param user 用户
     * @return boolean
     */
    @PutMapping("update")
    @Log(title = "更新用户信息", businessType = BusinessType.UPDATE)
    public SaResult update(@RequestBody User user) {
        return SaResult.data(vimBridgeService.updateUser(user));
    }

    /**
     * 更新用户密码
     *
     * @param pwdParam 用户密码信息
     * @return boolean
     */
    @PutMapping("updatePwd")
    public SaResult updatePwd(@RequestBody PwdParam pwdParam) {
        boolean result = vimBridgeService.updatePassword(pwdParam);
        if (result) {
            vimLoginService.handlePasswordUpdate(VimUtil.getLoginId());
        }
        return SaResult.data(result);
    }

}
