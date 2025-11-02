package com.vim.plus.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.plus.domain.SysUser;
import com.vim.plus.mapper.SysUserMapper;
import com.vim.plus.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author zkp
 * @since 2024-11-03
 */
@Service
@DS("slave")
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

}
