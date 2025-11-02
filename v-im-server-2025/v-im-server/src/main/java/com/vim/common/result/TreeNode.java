package com.vim.common.result;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 树状菜单
 *
 * @author 乐天kp
 */
@Data
public class TreeNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String label;
    private String parentId;
    private List<TreeNode> children;

    private int count;


}
