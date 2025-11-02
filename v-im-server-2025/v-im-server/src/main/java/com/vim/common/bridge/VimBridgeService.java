package com.vim.common.bridge;

import com.vim.modules.dept.result.Dept;
import com.vim.modules.login.param.LoginParam;
import com.vim.modules.login.param.PwdParam;
import com.vim.modules.login.param.RegisterParam;
import com.vim.modules.user.result.User;

import java.util.List;

public interface VimBridgeService {

    /**
     * 根据用户ID获取用户信息
     * 如果启用了IM头像功能，会包含ImSetting中的头像信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(String userId);

    /**
     * 更新用户信息
     * 如果启用了IM头像功能，会同步更新ImSetting中的头像信息
     *
     * @param user 需要更新的用户信息
     * @return 更新是否成功
     */
    boolean updateUser(User user);

    /**
     * 更新用户密码
     *
     * @param pwdParam 密码参数对象，包含新密码信息
     * @return 更新是否成功
     */
    boolean updatePassword(PwdParam pwdParam);

    /**
     * 根据用户ID列表批量获取用户信息
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    List<User> getUserByIds(List<String> userIds);

    /**
     * 根据部门ID获取该部门下的所有用户
     *
     * @param deptId 部门ID
     * @return 用户信息列表
     */
    List<User> getUserByDeptId(String deptId);

    /**
     * 根据关键字搜索用户
     * 支持用户名和昵称的模糊搜索
     *
     * @param str 搜索关键字
     * @return 符合条件的用户列表
     */
    List<User> searchUser(String str);

    /**
     * 获取所有部门列表
     *
     * @return 部门列表
     */
    List<Dept> getDeptList();

    /**
     * 根据父ID获取部门列表
     *
     * @param parentIds 父部门ID
     * @return 部门列表
     */
    List<Dept> getDeptList(String parentIds);

    /**
     * 根据部门ID获取部门信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    Dept get(String deptId);

    /**
     * 用户登录
     *
     * @param loginParam 登录参数，包含用户名、密码、验证码等信息
     * @return 登录成功后的token
     * @throws RuntimeException 当验证码错误、用户名密码错误或用户被停用时抛出
     */
    String login(LoginParam loginParam);

    /**
     * 用户注册
     *
     * @param registerParam 注册参数，包含用户名、密码、验证码等信息
     * @return 注册成功后的用户ID
     * @throws RuntimeException 当验证码错误、用户名已存在时抛出
     */
    String register(RegisterParam registerParam);
}
