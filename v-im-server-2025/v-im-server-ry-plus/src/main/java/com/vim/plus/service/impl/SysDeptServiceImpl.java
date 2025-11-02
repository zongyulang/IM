package com.vim.plus.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vim.plus.domain.SysDept;
import com.vim.plus.mapper.SysDeptMapper;
import com.vim.plus.service.ISysDeptService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 部门表 服务实现类
 * </p>
 *
 * @author zkp
 * @since 2024-11-03
 */
@Service
@DS("slave")
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

}
