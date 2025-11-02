package com.vim.sdk.service;

import com.vim.modules.friend.result.AddFriendResult;
import com.vim.modules.friend.result.Friend;
import com.vim.modules.user.result.User;

import java.util.List;

/**
 * 用户接口
 *
 * @author 乐天
 */
public interface VimFriendApiService {

    /**
     * 获取好友列表
     *
     * @param userId 用户id
     * @return List
     */
    List<User> getFriends(String userId);

    /**
     * 获取待审核状态好友列表
     *
     * @return List
     */
    List<Friend> getOthers(String state);


    /**
     * 添加好友
     *
     * @param friendId 好友id
     * @param message  消息
     * @param userId   用户id
     */
    AddFriendResult addFriends(String friendId, String message, String userId);

    /**
     * 同意添加好友
     *
     * @param friendId 好友id
     * @param userId   用户id
     */
    void agree(String friendId, String userId);

    /**
     * 删除好友
     *
     * @param friendId 好友id
     * @param userId   用户id
     */
    void delFriends(String friendId, String userId);

    /**
     * 是否是好友
     *
     * @param friendId 好友id
     * @param userId   用户id
     * @return boolean
     */
    boolean isFriends(String friendId, String userId);


}
