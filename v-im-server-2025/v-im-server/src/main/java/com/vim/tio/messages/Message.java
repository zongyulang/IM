package com.vim.tio.messages;

import cn.hutool.json.JSONObject;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;

/**
 * websocket 通讯的消息类型
 *
 * @author 乐天
 * @since 2018-10-07
 */
@Data
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 消息id
     */
    @Id
    private String id;

    /**
     * 消息的来源ID（如果是私聊，则是用户id，如果是群聊，则是群组id）
     */
    @Field("chat_id")
    private String chatId;

    /**
     * 聊天室类型 friend|group
     */
    @Field
    private String chatType;

    /**
     * 消息类型 文本|附件|其他
     */
    @Field("message_type")
    private String messageType;

    /**
     * 消息内容
     */
    @Field("content")
    private String content;

    /**
     * 消息的发送者id
     */
    @Field("from_id")
    private String fromId;

    /**
     * 服务端时间戳毫秒数
     */
    @Field("timestamp")
    private Long timestamp;

    /**
     * 扩展字段，格式化为json
     */
    @Field("extend")
    private JSONObject extend;


    /**
     * 消息的发送者id
     */
    @Field("chat_key")
    private String chatKey;

}
