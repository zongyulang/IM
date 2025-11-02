package com.vim.common.bridge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vim.plus.domain.SysDept;
import com.vim.plus.domain.SysUser;
import com.vim.plus.service.ISysDeptService;
import com.vim.plus.service.ISysUserService;
import jakarta.annotation.Resource;


//@SpringBootTest
public class VimBridgeServiceImplTest {

    @Resource
    private VimBridgeService vimBridgeService;

    @Resource
    private ISysUserService iSysUserService;

    @Resource
    private ISysDeptService iSysDeptService;

    void getUserById() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, "000000");
        iSysUserService.list(wrapper);
    }


    // 辅助方法
    private SysUser createMockSysUser(String userId, String userName) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        return user;
    }

    private SysDept createMockSysDept(String deptId, String deptName) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setDeptName(deptName);
        return dept;
    }
}