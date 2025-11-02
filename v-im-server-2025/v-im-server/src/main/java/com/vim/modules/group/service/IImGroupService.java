package com.vim.modules.group.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vim.modules.group.domain.ImGroup;
import com.vim.modules.user.result.User;

import java.util.List;

/**
 * 群管理Service接口
 *
 * @author 乐天
 * @since 2022-01-26
 */
public interface IImGroupService extends IService<ImGroup> {
    boolean save(ImGroup entity);

    List<ImGroup> getByUserId(String userId);

    List<String> getUserIdsByGroupId(String groupId, int num);

    List<User> getUserByGroupId(String groupId, int num);
}
