package com.vim.modules.group.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.modules.group.domain.ImGroupUser;
import com.vim.modules.group.mapper.ImGroupUserMapper;
import com.vim.modules.group.service.IImGroupUserService;
import org.springframework.stereotype.Service;

/**
 * 群关系Service业务层处理
 *
 * @author 乐天
 * @since 2022-01-25
 */
@Service
@DS("master")
public class ImGroupUserServiceImpl extends ServiceImpl<ImGroupUserMapper, ImGroupUser> implements IImGroupUserService {

    @Override
    public boolean isGroupUser(String groupId, String userId) {
        LambdaQueryWrapper<ImGroupUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupUser::getGroupId, groupId)
                .eq(ImGroupUser::getUserId, userId);
        return this.count(queryWrapper) > 0;
    }

}
