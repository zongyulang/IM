package com.vim.common.enums;

import lombok.Getter;

@Getter
public enum SysDelEnum {

    /**
     * 是
     */
    DEL_YES("1"),
    /**
     * 否
     */
    DEL_NO("0");


    private final String code;

    SysDelEnum(String code) {
        this.code = code;
    }

}
