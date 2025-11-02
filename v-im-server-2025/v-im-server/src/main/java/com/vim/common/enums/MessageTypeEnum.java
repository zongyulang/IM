package com.vim.common.enums;

import lombok.Getter;

/**
 * 封装的信息类型 UTILS
 *
 * @author 乐天
 */
@Getter
public enum MessageTypeEnum {

    /**
     * 文本消息
     */
    TEXT("text"),

    /**
     * 图片消息
     */
    IMAGE("image"),

    /**
     * 文件消息
     */
    FILE("file"),

    /**
     * 语音消息
     */
    VOICE("voice"),

    /**
     * 视频消息
     */
    VIDEO("video"),

    /**
     * 转发消息
     */
    FORWARD("forward"),

    /**
     * 事件消息
     */
    EVENT("event");

    private final String code;

    MessageTypeEnum(String code) {
        this.code = code;
    }

}
