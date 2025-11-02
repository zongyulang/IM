package com.vim.tio.service;

import com.vim.tio.messages.SendInfo;
import org.tio.core.ChannelContext;
import org.tio.websocket.common.WsResponse;

public interface MessageHandlerService {

    /**
     * 处理好友消息
     *
     * @param channelContext tio上下文
     * @param sendInfo        消息内容
     * @param wsResponse     websocket响应
     */
    void handleFriendMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse);

    /**
     * 处理群聊消息
     *
     * @param channelContext tio上下文
     * @param sendInfo        消息内容
     * @param wsResponse     websocket响应
     * @throws Exception 异常
     */
    void handleGroupMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse) throws Exception;

    /**
     * 处理消息已读
     *
     * @param sendInfo 消息信息
     * @throws Exception 异常
     */
    void handleReadMessage(SendInfo sendInfo) throws Exception;

    /**
     * 处理其他消息
     *
     * @param sendInfo 消息信息
     */
    void handleOtherMessage(ChannelContext channelContext, SendInfo sendInfo, String text);

    /**
     * 加载离线消息，并发送
     *
     * @param channelContext 连接上下文
     * @throws Exception 异常
     */
    void handleOffLineMessage(ChannelContext channelContext) throws Exception;

    /**
     * 处理接收到的消息
     *
     * @param channelContext 连接上下文
     * @param sendInfo       消息信息
     * @throws Exception 异常
     */
    void handleMessage(ChannelContext channelContext, SendInfo sendInfo) throws Exception;


    /**
     * 绑定用户群组
     *
     * @param channelContext 连接上下文
     * @param userId         用户ID
     */
    void bindUserGroups(ChannelContext channelContext, String userId);
}