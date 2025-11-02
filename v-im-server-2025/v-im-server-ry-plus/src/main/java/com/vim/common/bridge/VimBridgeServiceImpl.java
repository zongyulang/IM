package com.vim.common.bridge;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.vim.modules.login.param.LoginParam;
import com.vim.modules.login.param.PwdParam;
import com.vim.modules.login.param.RegisterParam;
import com.vim.modules.dept.result.Dept;
import com.vim.modules.user.result.User;
import com.vim.common.config.CaptchaConfig;
import com.vim.common.config.VimConfig;
import com.vim.common.utils.VimUtil;
import com.vim.plus.domain.SysDept;
import com.vim.plus.domain.SysUser;
import com.vim.plus.enums.UserType;
import com.vim.plus.service.ISysDeptService;
import com.vim.plus.service.ISysUserService;
import com.vim.modules.upload.utils.CommonAvatarUtil;
import com.vim.modules.setting.domain.ImSetting;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.modules.setting.service.IImSettingService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


//从库查询
@Service
@DS("slave")
public class VimBridgeServiceImpl implements VimBridgeService {

    /**
     * 默认的租户id
     */
    public static final String DEFAULT_TENANT_ID = "000000";

    @Resource
    private ISysUserService iSysUserService;

    @Resource
    private IImSettingService iImSettingService;

    @Resource
    private ISysDeptService iSysDeptService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private VimConfig vimConfig;

    /**
     * 将系统用户对象转换为前端用户对象
     *
     * @param sysUser 系统用户对象
     * @return 转换后的用户对象
     */
    private User convertToUser(SysUser sysUser) {
        if (sysUser == null) {
            return null;
        }
        User user = new User();
        // ID转换
        user.setId(sysUser.getUserId());
        // 名称转换
        user.setName(sysUser.getNickName());
        // 昵称转换
        user.setLoginName(sysUser.getUserName());
        // 手机号转换
        user.setMobile(sysUser.getPhonenumber());

        // 相同字段直接设置
        user.setDeptId(sysUser.getDeptId());
        user.setEmail(sysUser.getEmail());
        user.setSex(sysUser.getSex());
        return user;
    }

    /**
     * 将 SysDept 对象转换为 Dept 对象
     * 字段对应关系：
     *
     * @param sysDept 系统部门对象
     * @return 转换后的部门对象
     */
    private Dept convertToDept(SysDept sysDept) {
        if (sysDept == null) {
            return null;
        }
        Dept dept = new Dept();
        // ID转换
        dept.setId(sysDept.getDeptId());
        // 名称转换
        dept.setName(sysDept.getDeptName());
        // 相同字段直接设置
        dept.setParentId(sysDept.getParentId());
        return dept;
    }

    /**
     * 将 User 对象转换为 SysUser 对象
     *
     * @param user 源用户对象
     * @return 转换后的系统用户对象
     */
    private SysUser convertToSysUser(User user) {
        if (user == null) {
            return null;
        }
        SysUser sysUser = new SysUser();
        // ID转换
        sysUser.setUserId(user.getId());
        // 名称转换
        sysUser.setUserName(user.getLoginName());
        // 昵称转换
        sysUser.setNickName(user.getName());
        // 手机号转换
        sysUser.setPhonenumber(user.getMobile());

        // 相同字段直接设置
        sysUser.setDeptId(user.getDeptId());
        sysUser.setEmail(user.getEmail());
        sysUser.setSex(user.getSex());
        return sysUser;
    }

    /**
     * 将系统用户对象转换为带头像的前端用户对象
     *
     * @param sysUser 系统用户对象
     * @param setting IM设置对象，包含头像信息
     * @return 转换后的用户对象
     */
    private User convertToUserWithAvatar(SysUser sysUser, ImSetting setting) {
        User user = convertToUser(sysUser);
        if (user != null && setting != null && vimConfig.isUseImAvatar()) {
            user.setAvatar(setting.getAvatar());
            if (DictSwitchEnum.NO.getCode().equals(setting.getShowMobile())) {
                user.setMobile("***");
            }
            if (DictSwitchEnum.NO.getCode().equals(setting.getShowEmail())) {
                user.setEmail("***");
            }
        }
        return user;
    }

    /**
     * 根据配置创建默认的ImSetting对象
     *
     * @param userId 用户ID
     * @return 默认的ImSetting对象
     */
    private ImSetting createDefaultSetting(String userId) {
        ImSetting setting = new ImSetting();
        setting.setId(userId);
        setting.setCanAddFriend(vimConfig.getCanAddFriend());
        setting.setAddFriendValidate(vimConfig.getAddFriendValidate());
        setting.setCanSendMessage(vimConfig.getCanSendMessage());
        setting.setCanSoundRemind(vimConfig.getCanSoundRemind());
        setting.setCanVoiceRemind(vimConfig.getCanVoiceRemind());
        setting.setShowMobile(vimConfig.getShowMobile());
        setting.setShowEmail(vimConfig.getShowEmail());
        return setting;
    }

    /**
     * 获取ImSetting，如果不存在则创建默认设置
     *
     * @param userId 用户ID
     * @return ImSetting对象
     */
    private ImSetting getOrCreateSetting(String userId) {
        ImSetting setting = iImSettingService.getById(userId);
        if (setting == null) {
            setting = createDefaultSetting(userId);
            iImSettingService.save(setting);
        }
        return setting;
    }


    /**
     * 根据用户ID获取用户信息
     * 如果启用了IM头像功能，会包含ImSetting中的头像信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    @Cacheable(value = "user", key = "#userId")
    public User getUserById(String userId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserId, userId)
                .eq(SysUser::getTenantId, DEFAULT_TENANT_ID);
        SysUser sysUser = iSysUserService.getOne(wrapper);

        if (sysUser == null) {
            return null;
        }

        if (vimConfig.isUseImAvatar()) {
            ImSetting setting = getOrCreateSetting(userId);
            return convertToUserWithAvatar(sysUser, setting);
        } else {
            return convertToUser(sysUser);
        }
    }


    /**
     * 更新用户信息
     * 如果启用了IM头像功能，会同步更新ImSetting中的头像信息
     *
     * @param user 需要更新的用户信息
     * @return 更新是否成功
     */
    @Override
    @CacheEvict(value = "user", key = "#user.id")
    public boolean updateUser(User user) {
        // 更新系统用户信息
        SysUser sysUser = convertToSysUser(user);
        boolean result = iSysUserService.updateById(sysUser);

        // 如果启用了IM头像功能且更新成功，同步更新ImSetting
        if (result && vimConfig.isUseImAvatar() && user.getAvatar() != null) {
            ImSetting setting = getOrCreateSetting(user.getId());
            setting.setId(user.getId());
            setting.setAvatar(user.getAvatar());
            result = iImSettingService.updateById(setting);
        }

        return result;
    }

    /**
     * 更新用户密码
     *
     * @param pwdParam 密码参数对象，包含新密码信息
     * @return 更新是否成功
     */
    @Override
    public boolean updatePassword(PwdParam pwdParam) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(VimUtil.getLoginId());
        sysUser.setPassword(BCrypt.hashpw(pwdParam.getNewPassword()));
        return iSysUserService.updateById(sysUser);
    }

    /**
     * 批量转换用户列表，根据配置决定是否包含头像信息
     *
     * @param sysUsers 系统用户对象列表
     * @return 转换后的用户对象列表
     */
    private List<User> convertUserList(List<SysUser> sysUsers) {
        if (vimConfig.isUseImAvatar()) {
            // 获取用户ID列表
            List<String> userIds = sysUsers.stream()
                    .map(SysUser::getUserId)
                    .collect(Collectors.toList());
            // 获取设置并转换为Map
            List<ImSetting> settings = getSettingByIds(userIds);
            Map<String, ImSetting> settingMap = settings.stream()
                    .collect(Collectors.toMap(ImSetting::getId, setting -> setting));

            return sysUsers.stream()
                    .map(user -> convertToUserWithAvatar(user, settingMap.get(user.getUserId())))
                    .collect(Collectors.toList());
        } else {
            return sysUsers.stream()
                    .map(this::convertToUser)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 根据用户ID列表批量获取用户信息
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    @Override
    public List<User> getUserByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        final int batchSize = 500;
        // 获取用户列表
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, DEFAULT_TENANT_ID);
        List<SysUser> users = Lists.partition(userIds, batchSize).stream()
                .map(batchIds -> {
                    wrapper.in(SysUser::getUserId, batchIds);
                    return iSysUserService.list(wrapper);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return convertUserList(users);
    }

    /**
     * 根据部门ID获取该部门下的所有用户
     *
     * @param deptId 部门ID
     * @return 用户信息列表
     */
    @Override
    public List<User> getUserByDeptId(String deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getTenantId, DEFAULT_TENANT_ID);
        List<SysUser> sysUsers = iSysUserService.list(wrapper);
        return convertUserList(sysUsers);
    }

    /**
     * 根据关键字搜索用户
     * 支持用户名和昵称的模糊搜索
     *
     * @param str 搜索关键字
     * @return 符合条件的用户列表
     */
    @Override
    public List<User> searchUser(String str) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(SysUser::getUserName, str)
                        .or()
                        .like(SysUser::getNickName, str))
                .eq(SysUser::getTenantId, DEFAULT_TENANT_ID)
                .eq(SysUser::getUserType, UserType.APP_USER.getCode())
                .orderByDesc(SysUser::getCreateTime);
        List<SysUser> sysUsers = iSysUserService.list(wrapper);
        return convertUserList(sysUsers);
    }

    @Override
    public List<Dept> getDeptList() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getTenantId, DEFAULT_TENANT_ID);
        return iSysDeptService.list(wrapper).stream()
                .map(this::convertToDept)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dept> getDeptList(String parentId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getParentId, parentId)
                .eq(SysDept::getTenantId, DEFAULT_TENANT_ID);
        return iSysDeptService.list(wrapper).stream()
                .map(this::convertToDept)
                .collect(Collectors.toList());
    }

    @Override
    public Dept get(String deptId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptId, deptId)
                .eq(SysDept::getTenantId, DEFAULT_TENANT_ID);
        SysDept sysDept = iSysDeptService.getOne(wrapper);
        return convertToDept(sysDept);
    }

    /**
     * 用户登录
     *
     * @param loginParam 登录参数，包含用户名、密码、验证码等信息
     * @return 登录成功后的token
     * @throws RuntimeException 当验证码错误、用户名密码错误或用户被停用时抛出
     */
    @Override
    public String login(LoginParam loginParam) {
        // 根据用户名查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserName, loginParam.getUsername())
                .eq(SysUser::getTenantId, DEFAULT_TENANT_ID);
        SysUser sysUser = iSysUserService.getOne(wrapper);

        // 用户不存在
        if (sysUser == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 密码校验（这里应该使用加密后的密码比对）
        if (!BCrypt.checkpw(loginParam.getPassword(), sysUser.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 判断用户状态 是否停用
        if (DictSwitchEnum.YES.getCode().equals(sysUser.getStatus())) {
            throw new RuntimeException("用户已被停用");
        }

        // 执行登录
        StpUtil.login(sysUser.getUserId());

        // 返回token
        return StpUtil.getTokenValue();
    }


    /**
     * 根据用户ID列表批量获取IM设置信息
     *
     * @param userIds 用户ID列表
     * @return IM设置信息列表
     */
    private List<ImSetting> getSettingByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        final int batchSize = 500;
        LambdaQueryWrapper<ImSetting> wrapper = new LambdaQueryWrapper<>();
        return Lists.partition(userIds, batchSize).stream()
                .map(batchIds -> {
                    wrapper.in(ImSetting::getId, batchIds);
                    return iImSettingService.list(wrapper);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String register(RegisterParam registerParam) {
        // 验证码校验
        String verifyKey = CaptchaConfig.CAPTCHA_KEY + registerParam.getUuid();
        String captcha = redisTemplate.opsForValue().get(verifyKey);
        redisTemplate.delete(verifyKey);  // 用完即删

        if (captcha == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (!captcha.equalsIgnoreCase(registerParam.getCode())) {
            throw new RuntimeException("验证码错误");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserName, registerParam.getUsername())
                .eq(SysUser::getTenantId, DEFAULT_TENANT_ID);
        if (iSysUserService.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        SysUser sysUser = new SysUser();
        sysUser.setUserId(IdUtil.getSnowflakeNextIdStr());  // 生成用户ID
        sysUser.setUserName(registerParam.getUsername());
        sysUser.setNickName(registerParam.getUsername());
        sysUser.setPassword(BCrypt.hashpw(registerParam.getPassword()));  // 密码加密
        sysUser.setTenantId(DEFAULT_TENANT_ID);
        sysUser.setStatus(DictSwitchEnum.NO.getCode());  // 默认启用状态
        sysUser.setCreateBy(null);
        sysUser.setCreateTime(new Date());
        sysUser.setUserType(UserType.APP_USER.getCode());

        try {
            iSysUserService.save(sysUser);
            if (vimConfig.isUseImAvatar()) {
                ImSetting imSetting = createDefaultSetting(sysUser.getUserId());
                imSetting.setAvatar(CommonAvatarUtil.generateImg(registerParam.getUsername()));
                iImSettingService.save(imSetting);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return sysUser.getUserId();
    }

}
