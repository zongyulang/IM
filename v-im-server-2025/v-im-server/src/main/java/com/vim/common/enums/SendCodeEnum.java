package com.vim.common.enums;

import lombok.Getter;

/**
 * 发送码枚举
 *
 * @author 乐天
 */
@Getter
public enum SendCodeEnum {

    /**
     * 心跳
     */
    PING("ping"),

    /**
     * 链接就绪
     */
    READY("ready"),

    /**
     * 文本消息
     */
    MESSAGE("message"),

    /**
     * 消息已读回执
     */
    READ("read"),

    /**
     * 其他地方登录提醒
     */
    OTHER_LOGIN("other-login"),

    /**
     * 新增好友或者好友审核
     */
    FRIEND_REQUEST("friend-request"),

    /**
     * 群组验证
     */
    GROUP_REQUEST("group-request");


    private final String code;

    SendCodeEnum(String code) {
        this.code = code;
    }

}
