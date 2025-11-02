package com.vim.modules.chat.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.modules.chat.domain.ImMessageImmunity;
import com.vim.modules.chat.mapper.ImMessageImmunityMapper;
import com.vim.modules.chat.service.IImMessageImmunityService;
import org.springframework.stereotype.Service;

/**
 * 消息免打扰Service业务层处理
 *
 * @author 乐天
 */
@Service
@DS("master")
public class ImMessageImmunityServiceImpl extends ServiceImpl<ImMessageImmunityMapper, ImMessageImmunity> implements IImMessageImmunityService {

}
