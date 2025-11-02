package com.vim.tio.result;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息分页查询参数对象。
 * 用于封装消息查询时的过滤条件，包括搜索文本、聊天ID、发送者ID、消息类型、聊天类型以及日期范围。
 *
 * @author [你的名字]
 * @since [版本号或日期]
 */
@Data
public class MessagePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索文本，用于模糊匹配消息内容。
     */
    private String searchText;

    /**
     * 聊天ID，标识具体的聊天会话。
     */
    private String chatId;

    /**
     * 发送者ID，标识消息的发送者。
     */
    private String fromId;

    /**
     * 消息类型，例如文本、图片、文件等。
     */
    private String messageType;

    /**
     * 聊天类型，例如单聊、群聊等。
     */
    private String chatType;

    /**
     * 日期范围，用于筛选特定时间段内的消息。数组的第一个元素为起始日期，第二个元素为结束日期。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date[] dateRange;
}
