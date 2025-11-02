package com.vim.modules.setting.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.vim.modules.setting.domain.ImSetting;

/**
 * @author z
 */
public interface IImSettingService extends IService<ImSetting> {

    ImSetting getByUserId(Long userId);
}
