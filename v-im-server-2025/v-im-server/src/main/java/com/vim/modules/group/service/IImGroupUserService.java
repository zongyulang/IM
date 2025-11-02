package com.vim.modules.group.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vim.modules.group.domain.ImGroupUser;

/**
 * 群关系Service接口
 *
 * @author 乐天
 * @since 2022-01-25
 */
public interface IImGroupUserService extends IService<ImGroupUser> {
    boolean isGroupUser(String groupId, String userId);
}
