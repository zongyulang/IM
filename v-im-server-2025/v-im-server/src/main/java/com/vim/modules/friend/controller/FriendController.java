package com.vim.modules.friend.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.common.exception.VimBaseException;
import com.vim.common.utils.VimUtil;
import com.vim.modules.friend.result.AddFriendResult;
import com.vim.modules.friend.result.Friend;
import com.vim.modules.group.enums.FriendStatusEnum;
import com.vim.sdk.service.VimFriendApiService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * @author 乐天
 */
@RestController
@RequestMapping("/vim/server/friends")
public class FriendController {

    @Resource
    private VimFriendApiService vimFriendApiService;

    /**
     * 获取好友列表
     *
     * @return SaResult
     */
    @GetMapping
    @Log(title = "获取好友列表")
    public SaResult list() {
        return SaResult.data(vimFriendApiService.getFriends(VimUtil.getLoginId()));
    }

    /**
     * 获取待审核好友列表
     *
     * @return SaResult
     */
    @GetMapping(value = "validateList")
    @Log(title = "获取待审核好友列表")
    public SaResult validateList() {
        return SaResult.data(vimFriendApiService.getOthers(FriendStatusEnum.WAIT.getCode()));
    }


    /**
     * 判断是否是好友
     *
     * @param friendId 好友id
     * @return SaResult
     */
    @GetMapping(value = "isFriend")
    @Log(title = "检查好友关系")
    public SaResult isFriend(String friendId) {
        return SaResult.data(vimFriendApiService.isFriends(VimUtil.getLoginId(), friendId));
    }

    /**
     * 添加好友
     *
     * @param friend friend
     * @return SaResult
     */
    @PostMapping(value = "add")
    @Log(title = "发送好友申请", businessType = BusinessType.INSERT)
    public SaResult add(@RequestBody Friend friend) {
        AddFriendResult addFriendResult = vimFriendApiService.addFriends(friend.getFriendId(), friend.getMessage(), VimUtil.getLoginId());
        return SaResult.data(addFriendResult);
    }

    /**
     * 删除好友
     *
     * @param friendId 好友id
     * @return SaResult
     */
    @DeleteMapping(value = "delete")
    @Log(title = "删除好友", businessType = BusinessType.DELETE)
    public SaResult delete(@RequestBody String friendId) {
        vimFriendApiService.delFriends(friendId, VimUtil.getLoginId());
        return SaResult.ok();
    }

    /**
     * 添加好友
     *
     * @param userId 申请人
     * @return SaResult
     */
    @PostMapping(value = "agree")
    @Log(title = "同意好友申请", businessType = BusinessType.UPDATE)
    public SaResult agree(@RequestBody String userId) {
        try {
            vimFriendApiService.agree(VimUtil.getLoginId(), userId);
            return SaResult.ok();
        } catch (VimBaseException e) {
            return SaResult.error("好友已添加");
        } catch (Exception e) {
            return SaResult.error("添加好友出错");
        }
    }
}
