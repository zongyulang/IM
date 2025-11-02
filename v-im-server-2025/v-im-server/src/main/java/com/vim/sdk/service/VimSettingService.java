package com.vim.sdk.service;

import com.vim.modules.setting.result.Setting;

public interface VimSettingService {

    Setting getByUserId(Long userId);


    boolean update(Setting setting);
}
