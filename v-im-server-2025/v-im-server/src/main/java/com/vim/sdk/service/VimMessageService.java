package com.vim.sdk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vim.common.enums.ChatTypeEnum;
import com.vim.tio.messages.Message;
import com.vim.tio.messages.SendInfo;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsResponse;

import java.util.Date;
import java.util.List;


/**
 * 消息处理接口
 *
 * @author 乐天
 */
public interface VimMessageService {


    /**
     * 添加消息到redis 队列
     *
     * @param id      消息id
     * @param chatKey 消息集合
     * @throws Exception 抛出异常
     */
    Message get(String id, String chatKey) throws Exception;

    /**
     * 添加消息到redis 队列
     *
     * @param message  消息
     * @param isOnline 是否在线
     * @throws Exception 抛出异常
     */
    void saveOnLine(Message message, boolean isOnline) throws Exception;

    void saveOffLine(Message message) throws Exception;

    /**
     * 删除消息
     *
     * @param message  消息
     * @param isOnline 是否在线
     * @throws Exception 抛出异常
     */
    void delete(Message message, boolean isOnline) throws Exception;

    /**
     * 查询消息
     *
     * @param chatId   聊天室id
     * @param fromId   userId
     * @param chatType 聊天类型  私聊 群聊
     * @param pageSize 每页多少条
     * @return List
     */
    List<Message> list(String chatId, String fromId, String chatType, Long pageSize);


    /**
     * 分页查询
     *
     * @param chatId      聊天室id
     * @param fromId      发送人id
     * @param messageType 消息类型
     * @param chatType    聊天类型
     * @param searchText  搜索内容
     * @param dateRange   时间范围
     * @param page        分页条件
     * @return page
     */
    Page<Message> page(String chatId, String fromId, String messageType, String chatType, String searchText, Date[] dateRange, Page<Message> page);

    /**
     * 读取未读消息，并清空
     * 未读消息只存私聊消息，群聊消息还在群列表里
     *
     * @param chatId 聊天室id
     * @param fromId 发送人id
     * @return List
     */
    List<Message> unreadList(String chatId, String fromId);


    /**
     * 查询聊天记录,大于等于某个id之前的时间段
     *
     * @param chatId    聊天id
     * @param chatType  聊天类型
     * @param fromId    发送人id
     * @param messageId 大于此id的值
     * @return List
     */
    List<Message> list(String chatId, String chatType, String fromId, String messageId);

    /**
     * 查询一个群的未读消息
     *
     * @param userId 用户id
     * @param chatId 群id
     * @return List
     */
    List<Message> unreadGroupList(String userId, String chatId);

    /**
     * 已读消息的条数
     *
     * @param chatId 聊天室id
     * @param type   聊天室类型
     * @return 数量
     */
    Long count(String chatId, String userId, String type);

    /**
     * 读消息，并持久化到redis
     *
     * @param chatId    聊天室id
     * @param fromId    消息读取人
     * @param type      type
     * @param timestamp 系统时间
     */
    void receipt(String chatId, String fromId, String type, long timestamp) throws Exception;


    /**
     * 推送消息
     *
     * @param message 消息
     * @throws Exception 抛出异常
     */
    void push(Message message) throws Exception;

    /**
     * 发送消息
     *
     * @param tioConfig  tioConfig
     * @param message    消息
     * @param wsResponse 响应
     * @param chatId     聊天室id
     * @throws Exception 抛出异常
     */
    default void sendMessage(TioConfig tioConfig, Message message, SendInfo sendInfo, WsResponse wsResponse, String chatId) throws Exception {
        //单聊
        if (ChatTypeEnum.FRIEND.getCode().equals(message.getChatType())) {
            SetWithLock<ChannelContext> channelContextSetWithLock = Tio.getByUserid(tioConfig, chatId);
            //用户没有登录，存储到离线文件
            if (channelContextSetWithLock == null || channelContextSetWithLock.size() == 0) {
                saveOffLine(message);
            } else {
                //入库操作
                saveOnLine(message, true);
                Tio.sendToUser(tioConfig, chatId, wsResponse);
            }
            Tio.sendToUser(tioConfig, message.getFromId(), wsResponse);
        } else {
            Tio.sendToGroup(tioConfig, chatId, wsResponse);
            //入库操作
            saveOnLine(message, true);
        }
    }

    /**
     * 清除聊天记录
     *
     * @param chatId   聊天室id
     * @param fromId   发送人id
     * @param chatType 聊天类型
     */
    void clearChatMessage(String chatId, String fromId, String chatType);

}
