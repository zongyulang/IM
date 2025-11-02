package com.vim.tio.enums;

import lombok.Getter;

@Getter
public enum GroupUserStatusEnum {

    /**
     * 删除用户
     */
    DELETE("delete"),
    /**
     * 新增用户
     */
    ADD("add");


    private final String code;

    GroupUserStatusEnum(String code) {
        this.code = code;
    }

}
