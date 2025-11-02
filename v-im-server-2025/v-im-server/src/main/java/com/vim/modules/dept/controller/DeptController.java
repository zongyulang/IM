package com.vim.modules.dept.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.bridge.VimBridgeService;
import com.vim.common.utils.TreeConvert;
import com.vim.modules.dept.result.Dept;
import com.vim.modules.user.result.User;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vim/server/depts")
public class DeptController {

    /**
     * 顶级的 PARENT_ID
     */
    public static final String DEFAULT_PARENT_ID = "0";

    @Resource
    private VimBridgeService vimBridgeService;

    /**
     * 部门列表
     *
     * @return List
     */
    @GetMapping
    @Log(title = "查询部门列表")
    public SaResult list() {
        TreeConvert convert = new TreeConvert();
        return SaResult.data(convert.listToTree(vimBridgeService.getDeptList(), DEFAULT_PARENT_ID));
    }

    /**
     * 获取上级部门
     *
     * @param deptId deptId
     * @return ImDept
     */
    @GetMapping("parent")
    @Log(title = "获取上级部门")
    public SaResult parent(String deptId) {
        Dept dept = vimBridgeService.get(deptId);
        List<Dept> deptList = vimBridgeService.getDeptList(dept.getParentIds());
        deptList.add(dept);
        return SaResult.data(deptList);
    }

    /**
     * 单个部门的用户
     *
     * @param deptId deptId
     * @return ImDept
     */
    @GetMapping("{deptId}/users")
    @Log(title = "查询部门用户")
    public SaResult users(@PathVariable(value = "deptId") String deptId) {
        List<User> userList = vimBridgeService.getUserByDeptId(deptId);
        return SaResult.data(userList);
    }
}
