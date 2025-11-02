package com.vim.modules.chat.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天实体类，用于表示一个聊天会话的基本信息。
 */
@Data
public class Chat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天会话的唯一标识符。
     */
    private String id;

    /**
     * 聊天会话的名称。
     */
    private String name;

    /**
     * 聊天会话的头像URL。
     */
    private String avatar;

    /**
     * 聊天会话的类型，例如：单聊、群聊等。
     */
    private String type;

    /**
     * 最近一次消息的时间戳。
     */
    private long lastReadTime;

    /**
     * 未读消息的数量。
     */
    private long unreadCount;
}
