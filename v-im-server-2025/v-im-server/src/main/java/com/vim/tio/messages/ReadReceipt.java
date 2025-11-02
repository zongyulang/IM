package com.vim.tio.messages;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息已读回执
 *
 * @author 乐天
 */
@Data
public class ReadReceipt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天室id
     */
    private String chatId;

    /**
     * 消息读取人
     */
    private String fromId;

    /**
     * 最后一条消息读取时间
     */
    private Long timestamp;

    /**
     * 聊天类型
     */
    private String type;

}
