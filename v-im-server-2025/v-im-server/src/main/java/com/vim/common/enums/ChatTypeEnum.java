package com.vim.common.enums;

import lombok.Getter;

/**
 * 聊天类型
 *
 * @author 乐天
 */
@Getter
public enum ChatTypeEnum {

    /**
     * 私聊
     */
    FRIEND("friend"),

    /**
     * 群聊
     */
    GROUP("group");

    private final String code;

    ChatTypeEnum(String code) {
        this.code = code;
    }

}
