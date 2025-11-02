package com.vim.sdk.service;

import com.vim.modules.group.param.QuickGroup;
import com.vim.modules.group.result.Group;
import com.vim.modules.user.result.User;

import java.util.List;

/**
 * 群组接口
 *
 * @author 乐天
 */
public interface VimGroupApiService {

    /**
     * 获取用户的群组
     *
     * @param userId 用户id
     * @return List
     */
    List<Group> getGroups(String userId);



    /**
     * 获取群组的用户
     *
     * @param groupId 群组id
     * @return List
     */
    List<User> getUsers(String groupId);

    /**
     * 获取群组的用户
     *
     * @param groupId 群组id
     * @param num     获取数量
     * @return List
     */
    List<User> getUsers(String groupId, int num);

    /**
     * 获取群组信息
     *
     * @param groupId 群组id
     * @return Group
     */
    Group get(String groupId);

    /**
     * 添加用户到群
     *
     * @param groupId 群id
     * @param userIds 用户id
     */
    String[] addUsers(String groupId, String[] userIds);

    /**
     * 快速建群
     * @param quickGroup quickGroup
     * @return userIds
     */
    Group quickGroup(QuickGroup quickGroup);

    /**
     * 转让
     *
     * @param groupId 群id
     * @param userId  用户id
     */
    void transference(String groupId, String userId);

    /**
     * 删除群组用户
     *
     * @param groupId  群组id
     * @param userIds  用户id
     * @param sendTips 是否发生通知
     */
    void delUsers(String groupId, List<String> userIds, boolean sendTips);

    /**
     * 删除群组
     *
     * @param groupId 群组id
     */
    void del(String groupId);

    /**
     * 保存群组
     *
     * @param group 群组信息
     * @return Group
     */
    Group save(Group group);

    /**
     * 更新群组
     *
     * @param group 群组信息
     */
    void update(Group group);

    /**
     * 更新群组
     *
     * @param id 群组信息
     * @param groupName 群组信息
     */
    void updateGroupName(String id, String groupName);

    /**
     * 检查用户是否为群成员
     *
     * @param groupId 群id
     * @param userId  用户id
     * @return boolean 是否为群成员
     */
    boolean isMember(String groupId, String userId);

    /**
     * 获取群组中的用户ID列表
     * @param groupId 群组ID
     * @return  用户ID列表
     */
    List<String> getUserIdsByGroupId(String groupId);
}
