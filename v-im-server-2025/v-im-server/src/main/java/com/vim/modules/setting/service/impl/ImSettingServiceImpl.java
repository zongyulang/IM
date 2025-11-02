package com.vim.modules.setting.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.modules.setting.domain.ImSetting;
import com.vim.modules.setting.mapper.ImSettingMapper;
import com.vim.modules.setting.service.IImSettingService;
import org.springframework.stereotype.Service;

/**
 * @author zkp
 */
@Service
@DS("master")
public class ImSettingServiceImpl extends ServiceImpl<ImSettingMapper, ImSetting> implements IImSettingService {


    @Override
    public ImSetting getByUserId(Long userId) {
        LambdaQueryWrapper<ImSetting> query = new LambdaQueryWrapper<>();
        query.eq(ImSetting::getCreateBy, userId);
        return this.getOne(query);
    }
}




