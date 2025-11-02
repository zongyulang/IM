package com.vim.modules.friend.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.vim.common.bridge.VimBridgeService;
import com.vim.modules.friend.domain.ImFriend;
import com.vim.modules.friend.mapper.ImFriendMapper;
import com.vim.modules.friend.service.IImFriendService;
import com.vim.modules.user.result.User;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友Service业务层处理
 *
 * @author 乐天
 * @since 2022-02-03
 */
@Service
@DS("master")
public class ImFriendServiceImpl extends ServiceImpl<ImFriendMapper, ImFriend> implements IImFriendService {
    @Resource
    private VimBridgeService vimBridgeService;

    /**
     * 单次批量查询的最大数量
     */
    private static final int MAX_BATCH_SIZE = 1000;

    @Override
    public List<String> getFriendIdsByUser(String userId, String state) {
        // 查询指定状态的好友关系（双向查询）
        // 因为好友关系是双向的，所以需要查询userId在user_id或friend_id字段的记录
        List<ImFriend> friends = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(ImFriend::getUserId, userId)
                        .or()
                        .eq(ImFriend::getFriendId, userId))
                .eq(ImFriend::getState, state)
                .list();

        // 提取好友ID列表（需要判断userId是在哪一边）
        // 如果userId是user_id字段，则取friend_id作为好友ID
        // 如果userId是friend_id字段，则取user_id作为好友ID
        List<String> friendIds = friends.stream()
                .map(friend -> friend.getUserId().equals(userId) ?
                        friend.getFriendId() : friend.getUserId())
                .collect(Collectors.toList());

        return friendIds;
    }

    /**
     * 获取指定用户的所有好友列表
     *
     * @param userId 用户ID
     * @param state  好友关系状态
     * @return 好友用户信息列表
     */
    @Override
    public List<User> getUserFriends(String userId, String state) {
        // 查询指定状态的好友关系（双向查询）
        // 因为好友关系是双向的，所以需要查询userId在user_id或friend_id字段的记录
        List<ImFriend> friends = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(ImFriend::getUserId, userId)
                        .or()
                        .eq(ImFriend::getFriendId, userId))
                .eq(ImFriend::getState, state)
                .list();

        // 提取好友ID列表（需要判断userId是在哪一边）
        // 如果userId是user_id字段，则取friend_id作为好友ID
        // 如果userId是friend_id字段，则取user_id作为好友ID
        List<String> friendIds = friends.stream()
                .map(friend -> friend.getUserId().equals(userId) ?
                        friend.getFriendId() : friend.getUserId())
                .collect(Collectors.toList());

        // 通过bridge服务批量获取用户信息
        return batchGetUsers(friendIds);
    }

    /**
     * 获取指定用户与特定好友之间的关系状态
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @param state    好友关系状态
     * @return 好友用户信息列表
     */
    @Override
    public List<User> getUserFriendState(String userId, String friendId, String state) {
        // 查询特定好友关系状态（双向查询）
        // 检查(userId, friendId)或(friendId, userId)的记录
        List<ImFriend> friends = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .and(w -> w
                                .eq(ImFriend::getUserId, userId)
                                .eq(ImFriend::getFriendId, friendId))
                        .or()
                        .and(w -> w
                                .eq(ImFriend::getUserId, friendId)
                                .eq(ImFriend::getFriendId, userId)))
                .eq(ImFriend::getState, state)
                .list();

        // 提取好友ID列表（需要判断userId是在哪一边）
        List<String> friendIds = friends.stream()
                .map(friend -> friend.getUserId().equals(userId) ?
                        friend.getFriendId() : friend.getUserId())
                .collect(Collectors.toList());

        // 通过bridge服务批量获取用户信息
        return batchGetUsers(friendIds);
    }

    /**
     * 物理删除好友，不能用status，防止出现脏数据
     *
     * @param userId   用户ID
     * @param friendId 好友id
     */
    public void realDelete(@Param("userId") String userId, @Param("friendId") String friendId) {
        baseMapper.realDelete(userId, friendId);
    }

    /**
     * 批量获取用户信息，支持大数据量分组查询
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    private List<User> batchGetUsers(List<String> userIds) {
        // 空列表直接返回
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 如果数量不超过最大批量大小，直接查询
        if (userIds.size() <= MAX_BATCH_SIZE) {
            return vimBridgeService.getUserByIds(userIds);
        }

        // 使用 Guava 的 Lists.partition 进行分组查询
        // 将大列表分割成多个小列表，每个小列表最多包含MAX_BATCH_SIZE个元素
        // 分别查询后再合并结果
        return Lists.partition(userIds, MAX_BATCH_SIZE).stream()
                .map(batch -> vimBridgeService.getUserByIds(batch))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
