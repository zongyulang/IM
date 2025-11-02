package com.vim.common.enums;

import lombok.Getter;

@Getter
public enum DictSwitchEnum {
    /**
     * 是
     */
    YES("1"),
    /**
     * 否
     */
    NO("0");


    private final String code;

    DictSwitchEnum(String code) {
        this.code = code;
    }

}
