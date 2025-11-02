package com.vim.modules.dept.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Dept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private String id;

    /**
     * 父部门ID
     */
    private String parentId;

    /**
     * 祖级列表
     */
    private String parentIds;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 显示顺序
     */
    private String orderNum;

}
