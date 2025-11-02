package com.vim.sdk.service;

import com.vim.modules.group.result.GroupInvite;
import com.vim.modules.group.result.GroupInviteCount;

import java.util.List;

/**
 * 群邀请接口
 *
 * @author 乐天
 */
public interface VimGroupInviteService {

    /**
     * 获取群邀请列表
     *
     * @return List
     */
    List<GroupInvite> list(String groupId);


    /**
     * 获取群邀请列表
     *
     * @return List
     */
    List<GroupInviteCount> waitCheckList();


    /**
     * 获取我发送的申请
     *
     * @return List
     */
    List<GroupInvite> mySendList();


    /**
     * 同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    void agree(String groupInviteId);


    /**
     * 不同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    void refuse(String groupInviteId);
}
