package com.vim.modules.group.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.common.exception.VimBaseException;
import com.vim.common.utils.VimUtil;
import com.vim.modules.group.param.GroupName;
import com.vim.modules.group.param.QuickGroup;
import com.vim.modules.group.result.Group;
import com.vim.sdk.service.VimGroupApiService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 群组控制器
 *
 * @author 乐天
 */
@RestController
@RequestMapping("/vim/server/groups")
public class GroupController {

    @Resource
    private VimGroupApiService vimGroupApiService;

    /**
     * 新建群
     *
     * @param group 群
     * @return boolean
     */
    @PostMapping("")
    @Log(title = "创建群组", businessType = BusinessType.INSERT)
    public SaResult save(@RequestBody Group group) {
        return SaResult.data(vimGroupApiService.save(group));
    }

    /**
     * 更新群
     *
     * @param group 群
     * @return boolean
     */
    @PatchMapping(value = "{id}")
    @Log(title = "更新群组信息", businessType = BusinessType.UPDATE)
    public SaResult update(@PathVariable String id, @RequestBody Group group) {
        group.setId(id);
        checkMaster(id);
        vimGroupApiService.update(group);
        return SaResult.ok();
    }

    /**
     * 获取群
     *
     * @param id 群id
     * @return boolean
     */
    @GetMapping(value = "{id}")
    @Log(title = "获取群组信息")
    public SaResult get(@PathVariable("id") String id) {
        return SaResult.data(vimGroupApiService.get(id));
    }

    /**
     * 查询当前用户所在的群
     *
     * @return List
     */
    @GetMapping
    @Log(title = "查询用户群组列表")
    public SaResult list() {
        return SaResult.data(vimGroupApiService.getGroups(VimUtil.getLoginId()));
    }


    /**
     * 查询群的所有的用户
     *
     * @return List
     */
    @GetMapping("{id}/users")
    @Log(title = "查询群组成员")
    public SaResult users(@PathVariable("id") String id, Integer num) {
        // 验证当前用户是否为群成员
        if (!vimGroupApiService.isMember(id, VimUtil.getLoginId())) {
            return SaResult.error("您已经不是此群成员");
        }
        if (num == null) {
            return SaResult.data(vimGroupApiService.getUsers(id));
        } else {
            return SaResult.data(vimGroupApiService.getUsers(id, num));
        }
    }

    /**
     * 添加群成员
     *
     * @return List
     */
    @PostMapping("{id}/users")
    @Log(title = "添加群组成员", businessType = BusinessType.INSERT)
    public SaResult addUsers(@PathVariable("id") String id, @RequestBody String[] userId) {
        return SaResult.data(vimGroupApiService.addUsers(id, userId));
    }

    /**
     * 快速建群
     *
     * @return List
     */
    @PostMapping("quickGroup")
    @Log(title = "快速建群", businessType = BusinessType.INSERT)
    public SaResult quickGroup(@RequestBody QuickGroup quickGroup) {
        return SaResult.data(vimGroupApiService.quickGroup(quickGroup));
    }


    /**
     * 删除群成员
     *
     * @return List
     */
    @DeleteMapping("{id}/users/{userId}")
    @Log(title = "删除群组成员", businessType = BusinessType.DELETE)
    public SaResult deleteUser(@PathVariable("id") String id, @PathVariable("userId") String userId) {
        String[] ids = {userId};
        vimGroupApiService.delUsers(id, Arrays.asList(ids), true);
        return SaResult.ok();
    }

    /**
     * 删除群成员
     *
     * @return List
     */
    @DeleteMapping("{id}/users")
    @Log(title = "删除群组成员", businessType = BusinessType.DELETE)
    public SaResult deleteUser(@PathVariable("id") String id, @RequestBody String[] userId) {
        vimGroupApiService.delUsers(id, Arrays.asList(userId), true);
        return SaResult.ok();
    }

    /**
     * 主动退群
     *
     * @return List
     */
    @DeleteMapping("{id}/exit")
    @Log(title = "退出群组", businessType = BusinessType.DELETE)
    public SaResult exit(@PathVariable("id") String id) {
        String[] ids = {VimUtil.getLoginId()};
        vimGroupApiService.delUsers(id, Arrays.asList(ids), true);
        return SaResult.ok();
    }

    /**
     * 删一个群,只有群主能删除
     *
     * @param id 群id
     * @return boolean
     */
    @DeleteMapping(value = "{id}")
    @Log(title = "删除群组", businessType = BusinessType.DELETE)
    public SaResult delete(@PathVariable("id") String id) {
        checkMaster(id);
        vimGroupApiService.del(id);
        return SaResult.ok();
    }

    /**
     * 群转让
     *
     * @param id     群id
     * @param userId 受让人
     * @return 结果
     */
    @PostMapping("{id}/transference/{userId}")
    @Log(title = "群组转让", businessType = BusinessType.UPDATE)
    public SaResult transference(@PathVariable("id") String id, @PathVariable("userId") String userId) {
        vimGroupApiService.transference(id, userId);
        return SaResult.data("转让成功");
    }

    /**
     * 判断是否群主
     *
     * @param groupId 群id
     */
    private void checkMaster(String groupId) {
        Group group = vimGroupApiService.get(groupId);
        if (group == null) {
            throw new VimBaseException("群已解散！");
        }
        if (!VimUtil.getLoginId().equals(group.getMaster())) {
            throw new VimBaseException("非群主不能执行此操作！");
        }
    }

    @PatchMapping(value = "name")
    @Log(title = "更新群组名称", businessType = BusinessType.UPDATE)
    public SaResult updateName(@RequestBody GroupName groupName) {
        // 检查当前用户是否为群主
        checkMaster(groupName.getId());
        vimGroupApiService.updateGroupName(groupName.getId(), groupName.getName());
        return SaResult.ok();
    }

}
