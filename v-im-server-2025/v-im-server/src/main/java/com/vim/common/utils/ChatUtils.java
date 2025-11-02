package com.vim.common.utils;

import cn.hutool.core.util.StrUtil;
import com.vim.common.enums.ChatTypeEnum;

/**
 * 封装的信息类型 UTILS
 *
 * @author 乐天
 * @since 2018-10-07
 */
public class ChatUtils {

    /**
     * 存放私聊的已读消息
     * message-{minUserId}-{maxUserId}
     */
    public static final String FRIEND_TEMPLATE = "message-s-{}-{}";

    /**
     * 存放私聊的已读消息
     * message-{minUserId}-{maxUserId}
     */
    public static final String GROUP_TEMPLATE = "message-g-{}";

    /**
     * 存放聊的最后一次查看消息时间
     * message-{userId：读取人}-{chatId：id}
     */
    public static final String READ_TEMPLATE = "read-{}-{}";

    /**
     * 存放"私聊"的离线
     * message-{toUserId} 也是 chatId
     */
    public static final String UNREAD_TEMPLATE = "offline-message-{}";

    /**
     * 存放"私聊"的未收到回执消息
     */
    public static final String UN_ACK_TEMPLATE = "un-ack-message-{}";

    /**
     * 消息分片存储的分片数量（所有的消息分1000个片）
     */
    private static final long SPLIT = 1000L;

    /**
     * 存放私聊的已读消息
     * message-{minUserId}-{maxUserId}
     */
    public static final String COLLECTION_TEMPLATE_SINGLE = "message-s-{}";

    public static final String COLLECTION_TEMPLATE_GROUP = "message-g-{}";

    /**
     * 获取一个固定的key来存储聊天记录到redis
     *
     * @param fromId 来源
     * @param chatId 聊天id
     * @param type   类型
     * @return chatId
     */
    public static String getChatKey(String fromId, String chatId, String type) {
        if (ChatTypeEnum.FRIEND.getCode().equals(type)) {
            if (Long.parseLong(fromId) < Long.parseLong(chatId)) {
                return StrUtil.format(FRIEND_TEMPLATE, fromId, chatId);
            } else {
                return StrUtil.format(FRIEND_TEMPLATE, chatId, fromId);
            }
        } else {
            return StrUtil.format(GROUP_TEMPLATE, chatId);
        }
    }

    /**
     * 获取一个群的key
     *
     * @param chatId 聊天id
     * @return chatId
     */
    public static String getUnAckKey(String chatId) {
        return StrUtil.format(UN_ACK_TEMPLATE, chatId);
    }

    public static String getCollectionNameByChatKey(String chatKey) {
        String[] arr = chatKey.split("-");
        if (arr.length == 3) {
            return getCollectionName(arr[1], arr[2], ChatTypeEnum.FRIEND.getCode());
        } else {
            return getCollectionName("0", arr[1], ChatTypeEnum.GROUP.getCode());
        }
    }

    /**
     * 获取一个固定的key来存储聊天记录读取的最后时间
     *
     * @param userId 来源
     * @param chatId 聊天id
     * @return chatId
     */
    public static String getReadKey(String userId, String chatId) {
        return StrUtil.format(READ_TEMPLATE, userId, chatId);
    }

    /**
     * 根据分片算法获得聊天集合的集合名
     *
     * @param fromId 来源
     * @param chatId 聊天id
     * @param type   类型
     * @return chatId
     */
    public static String getCollectionName(String fromId, String chatId, String type) {
        if (ChatTypeEnum.FRIEND.getCode().equals(type)) {
            long f = Long.parseLong(fromId);
            long c = Long.parseLong(chatId);
            if (f < c) {
                return StrUtil.format(COLLECTION_TEMPLATE_SINGLE, (c - f) % SPLIT);
            } else {
                return StrUtil.format(COLLECTION_TEMPLATE_SINGLE, (f - c) % SPLIT);
            }
        } else {
            return StrUtil.format(COLLECTION_TEMPLATE_GROUP, (Long.parseLong(chatId)) % SPLIT);
        }
    }

    public static String getOffLineCollectionName(String chatId) {
        return StrUtil.format(ChatUtils.UNREAD_TEMPLATE, chatId);
    }

}
