package com.vim.common.utils;


import com.vim.common.result.TreeNode;
import com.vim.modules.dept.result.Dept;
import com.vim.modules.user.result.User;

import java.util.ArrayList;
import java.util.List;

public class TreeConvert {

    public List<TreeNode> listToTree(List<Dept> list, String pid) {
        List<TreeNode> list1 = convert(list);
        //顶级菜单
        List<TreeNode> sysTreeList = new ArrayList<>();
        for (TreeNode tree : list1) {
            if (pid.equals(tree.getParentId())) {
                sysTreeList.add(tree);
            }
        }
        findChildren(sysTreeList, list1);
        return sysTreeList;
    }

    private void findChildren(List<TreeNode> sysTreeList, List<TreeNode> list) {
        for (TreeNode sysNode : sysTreeList) {
            List<TreeNode> children = new ArrayList<>();
            for (TreeNode treeNode : list) {
                if (sysNode.getId().equals(treeNode.getParentId())) {
                    children.add(treeNode);
                }
            }
            sysNode.setChildren(children);
            findChildren(children, list);
        }
    }

    private List<TreeNode> convert(List<Dept> list) {
        List<TreeNode> treeNodes = new ArrayList<>();
        for (Dept entity : list) {
            TreeNode treeNode = new TreeNode();
            treeNode.setId(entity.getId());
            treeNode.setParentId(entity.getParentId());
            treeNode.setLabel(entity.getName());
            treeNode.setChildren(new ArrayList<>());
            treeNodes.add(treeNode);
        }
        return treeNodes;
    }

    private List<TreeNode> convertUser(List<User> list) {
        List<TreeNode> treeNodes = new ArrayList<>();
        for (User entity : list) {
            TreeNode treeNode = new TreeNode();
            treeNode.setId(entity.getId());
            treeNode.setParentId(entity.getDeptId());
            treeNode.setLabel(entity.getName());
            treeNode.setChildren(new ArrayList<>());
            treeNodes.add(treeNode);
        }
        return treeNodes;
    }

}
