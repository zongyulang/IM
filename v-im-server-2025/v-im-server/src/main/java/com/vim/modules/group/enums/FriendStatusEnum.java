package com.vim.modules.group.enums;

import lombok.Getter;

@Getter
public enum FriendStatusEnum {

    /**
     * 正常
     */
    COMMON("0"),
    /**
     * 拒绝
     */
    REFUSE("1"),
    /**
     * 待审核
     */
    WAIT("2");

    private final String code;

    FriendStatusEnum(String code) {
        this.code = code;
    }

}
