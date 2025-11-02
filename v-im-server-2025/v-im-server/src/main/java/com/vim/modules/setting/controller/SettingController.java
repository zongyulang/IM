package com.vim.modules.setting.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.common.utils.VimUtil;
import com.vim.modules.setting.result.Setting;
import com.vim.sdk.service.VimSettingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/vim/server/setting")
public class SettingController {

    @Resource
    private VimSettingService vimSettingService;


    @GetMapping(value = "{userId}")
    @Log(title = "获取用户设置")
    public SaResult get(@PathVariable String userId) {
        return SaResult.data(vimSettingService.getByUserId(Long.parseLong(userId)));
    }

    /**
     * 更新自己的设置信息
     *
     * @param setting 设置信息
     * @return SaResult
     */
    @PutMapping
    @Log(title = "更新用户设置", businessType = BusinessType.UPDATE)
    public SaResult update(@RequestBody Setting setting) {
        setting.setUserId(VimUtil.getLoginId());
        return SaResult.data(vimSettingService.update(setting));
    }
}
