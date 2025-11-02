package com.vim.modules.group.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.vim.common.bridge.VimBridgeService;
import com.vim.modules.group.domain.ImGroup;
import com.vim.modules.group.domain.ImGroupUser;
import com.vim.modules.group.mapper.ImGroupMapper;
import com.vim.modules.group.service.IImGroupService;
import com.vim.modules.group.service.IImGroupUserService;
import com.vim.modules.user.result.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群组管理Service实现类
 *
 * @author 乐天
 * @since 2022-01-26
 */
@Service
@DS("master")
public class ImGroupServiceImpl extends ServiceImpl<ImGroupMapper, ImGroup> implements IImGroupService {
    /**
     * 群组用户关系Service
     */
    @Resource
    private IImGroupUserService imGroupUserService;

    /**
     * 用户信息桥接Service
     */
    @Resource
    private VimBridgeService vimBridgeService;

    /**
     * 批量查询的最大数量
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 获取用户所在的群组列表
     *
     * @param userId 用户ID
     * @return 群组列表
     */
    @Override
    public List<ImGroup> getByUserId(String userId) {
        return lambdaQuery()
                .inSql(ImGroup::getId,
                        "SELECT group_id FROM im_group_user WHERE user_id = '" + userId + "' and del_flag='0'")
                .list();
    }

    /**
     * 保存群组信息
     *
     * @param entity 群组实体
     * @return 保存结果
     */
    @Override
    public boolean save(ImGroup entity) {
        return super.save(entity);
    }

    /**
     * 获取群组中的用户ID列表
     *
     * @param groupId 群组ID
     * @param num     查询数量，当num <= 0时查询所有
     * @return 用户ID列表
     */
    @Override
    public List<String> getUserIdsByGroupId(String groupId, int num) {
        LambdaQueryWrapper<ImGroupUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImGroupUser::getGroupId, groupId)
                .orderByAsc(ImGroupUser::getCreateTime)
                .last(num > 0, "LIMIT " + num);
        List<ImGroupUser> imGroupUsers = imGroupUserService.list(wrapper);
        return imGroupUsers.stream()
                .map(ImGroupUser::getUserId)
                .collect(Collectors.toList());
    }



    /**
     * 获取群组中的用户信息列表
     * 当用户数量超过1000时，会分批查询
     *
     * @param groupId 群组ID
     * @param num     查询数量，当num <= 0时查询所有
     * @return 用户信息列表
     */
    @Override
    public List<User> getUserByGroupId(String groupId, int num) {
        List<String> userIds = getUserIdsByGroupId(groupId, num);
        return Lists.partition(userIds, BATCH_SIZE)
                .stream()
                .map(vimBridgeService::getUserByIds)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
