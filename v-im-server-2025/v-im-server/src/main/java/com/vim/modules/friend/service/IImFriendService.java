package com.vim.modules.friend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vim.modules.friend.domain.ImFriend;
import com.vim.modules.user.result.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友Service接口
 *
 * @author 乐天
 * @since 2022-02-03
 */
public interface IImFriendService extends IService<ImFriend> {
    /**
     * 根据用户的ID 获取 用户好友(双向用户关系)
     *
     * @param userId 用户ID
     * @return 好友分组的列表
     */
    List<String> getFriendIdsByUser(@Param("userId") String userId, @Param("state") String state);

    /**
     * 根据用户的ID 获取 用户好友(双向用户关系)
     *
     * @param userId 用户ID
     * @return 好友分组的列表
     */
    List<User> getUserFriends(@Param("userId") String userId, @Param("state") String state);

    /**
     * 根据用户的ID  好友id 获取  双向用户关系
     *
     * @param userId   用户ID
     * @param friendId 好友id
     * @return 好友分组的列表
     */
    List<User> getUserFriendState(@Param("userId") String userId, @Param("friendId") String friendId, @Param("state") String state);

    /**
     * 物理删除好友，不能用status，防止出现脏数据
     *
     * @param userId   用户ID
     * @param friendId 好友id
     */
    void realDelete(@Param("userId") String userId, @Param("friendId") String friendId);
}
