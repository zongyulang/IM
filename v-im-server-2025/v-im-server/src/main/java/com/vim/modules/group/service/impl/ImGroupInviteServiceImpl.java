package com.vim.modules.group.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.modules.group.domain.ImGroupInvite;
import com.vim.modules.group.mapper.ImGroupInviteMapper;
import com.vim.modules.group.service.IImGroupInviteService;
import org.springframework.stereotype.Service;

/**
 * @author 乐天
 * @description 针对表【im_group_invite(群邀请)】的数据库操作Service实现
 * @createDate 2023-06-25 10:08:34
 */
@Service
@DS("master")
public class ImGroupInviteServiceImpl extends ServiceImpl<ImGroupInviteMapper, ImGroupInvite>
        implements IImGroupInviteService {

}




