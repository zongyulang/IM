package com.vim.sdk.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.vim.common.config.VimConfig;
import com.vim.common.enums.SysDelEnum;
import com.vim.modules.setting.domain.ImSetting;
import com.vim.modules.setting.result.Setting;
import com.vim.modules.setting.service.IImSettingService;
import com.vim.sdk.service.VimSettingService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@DS("master")
public class VimSettingServiceImpl implements VimSettingService {

    private static final String CACHE_KEY = "setting";

    @Resource
    private IImSettingService IImSettingService;

    @Resource
    private VimConfig vimConfig;

    @Override
    @Cacheable(value = CACHE_KEY + ":one", key = "#userId")
    public Setting getByUserId(Long userId) {
        ImSetting imSetting = IImSettingService.getByUserId(userId);
        Setting setting = new Setting();
        if (imSetting == null) {
            setting.setCanVoiceRemind(vimConfig.getCanVoiceRemind());
            setting.setAddFriendValidate(vimConfig.getAddFriendValidate());
            setting.setCanSendMessage(vimConfig.getCanSendMessage());
            setting.setCanSoundRemind(vimConfig.getCanSoundRemind());
            setting.setCanAddFriend(vimConfig.getCanAddFriend());
            setting.setShowMobile(vimConfig.getShowMobile());
            setting.setShowEmail(vimConfig.getShowEmail());
        } else {
            setting.setCanVoiceRemind(imSetting.getCanVoiceRemind());
            setting.setAddFriendValidate(imSetting.getAddFriendValidate());
            setting.setCanAddFriend(imSetting.getCanAddFriend());
            setting.setCanSendMessage(imSetting.getCanSendMessage());
            setting.setCanSoundRemind(imSetting.getCanSoundRemind());
            setting.setCanAddFriend(imSetting.getCanAddFriend());
            setting.setShowMobile(imSetting.getShowMobile());
            setting.setShowEmail(imSetting.getShowEmail());
        }
        return setting;
    }

    @Override
    @CacheEvict(value = CACHE_KEY + ":one", key = "#setting.userId")
    public boolean update(Setting setting) {
        Long userId = Long.valueOf(setting.getUserId());
        ImSetting oldSetting = IImSettingService.getByUserId(userId);
        if (oldSetting == null) {
            ImSetting newSetting = new ImSetting();
            settingOper(setting, userId, newSetting);
            newSetting.setCreateTime(new Date());
            newSetting.setDelFlag(SysDelEnum.DEL_NO.getCode());
            return IImSettingService.save(newSetting);
        } else {
            settingOper(setting, userId, oldSetting);
            oldSetting.setUpdateBy(String.valueOf(userId));
            oldSetting.setUpdateTime(new Date());
            oldSetting.setDelFlag(SysDelEnum.DEL_NO.getCode());
            return IImSettingService.updateById(oldSetting);
        }
    }

    private void settingOper(Setting setting, Long userId, ImSetting newSetting) {
        newSetting.setCanVoiceRemind(setting.getCanVoiceRemind());
        newSetting.setAddFriendValidate(setting.getAddFriendValidate());
        newSetting.setCanAddFriend(setting.getCanAddFriend());
        newSetting.setCanSendMessage(setting.getCanSendMessage());
        newSetting.setCanSoundRemind(setting.getCanSoundRemind());
        newSetting.setShowMobile(setting.getShowMobile());
        newSetting.setShowEmail(setting.getShowEmail());
        newSetting.setCreateBy(String.valueOf(userId));
    }
}
