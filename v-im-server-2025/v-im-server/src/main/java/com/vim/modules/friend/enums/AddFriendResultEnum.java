package com.vim.modules.friend.enums;

import lombok.Getter;

@Getter
public enum AddFriendResultEnum {

    SUCCESS("添加成功"),
    ALREADY_FRIEND("已经是好友了"),
    ALREADY_REQUEST("已经申请加好友了"),
    NOT_ALLOW_FRIEND("对方不允许好友"),
    WAIT_CHECK("请等待对方审核");

    private final String msg;

    AddFriendResultEnum(String msg) {
        this.msg = msg;
    }
}
