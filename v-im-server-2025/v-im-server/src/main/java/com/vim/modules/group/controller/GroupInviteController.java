package com.vim.modules.group.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.sdk.service.VimGroupInviteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
/**
 * 群邀请 控制器
 *
 * @author 乐天
 */
@RestController
@RequestMapping("/vim/server/groupInvites")
public class GroupInviteController {

    @Resource
    private VimGroupInviteService vimGroupInviteService;

    /**
     * 获取待审核群邀请列表（群主）
     *
     * @return List
     */
    @GetMapping
    @Log(title = "查询群邀请列表")
    public SaResult list(String groupId) {
        return SaResult.data(vimGroupInviteService.list(groupId));
    }

    /**
     * 获取待审核群邀请数量（群主）
     *
     * @return List
     */
    @GetMapping("waitCheckList")
    @Log(title = "查询待审核群邀请")
    public SaResult waitCheckList() {
        return SaResult.data(vimGroupInviteService.waitCheckList());
    }

    /**
     * 同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    @PostMapping(value = "agree/{groupInviteId}")
    @Log(title = "同意群邀请", businessType = BusinessType.UPDATE)
    public SaResult agree(@PathVariable String groupInviteId) {
        vimGroupInviteService.agree(groupInviteId);
        return SaResult.ok();
    }

    /**
     * 不同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    @PostMapping(value = "refuse/{groupInviteId}")
    @Log(title = "拒绝群邀请", businessType = BusinessType.UPDATE)
    public SaResult refuse(@PathVariable String groupInviteId) {
        vimGroupInviteService.refuse(groupInviteId);
        return SaResult.ok();
    }
}
