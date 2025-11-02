package com.vim.tio.service.impl;

import com.vim.tio.messages.Message;
import com.vim.tio.messages.SendInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsResponse;

/**
 * 单节点模式下的消息处理服务实现类。
 * 继承自 {@link AbstractMessageHandlerService}，实现了单节点模式下的消息投递逻辑。
 */
@Slf4j
@Service(value = "singleMessageHandlerService")
public class SingleMessageHandlerServiceImpl extends AbstractMessageHandlerService {

    /**
     * 投递在线用户消息。
     * 在单节点模式下，直接将消息发送给目标用户。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo        消息对象
     * @param wsResponse     WebSocket响应对象
     * @throws Exception 异常
     */
    @Override
    protected void deliverOnlineMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse) throws Exception {
        Message message = parseMessage(sendInfo);
        // 单节点消息投递逻辑
        Tio.sendToUser(channelContext.tioConfig, message.getChatId(), wsResponse);
        vimMessageService.saveOnLine(message, true);
    }

    /**
     * 投递群聊消息。
     * 在单节点模式下，将消息发送给指定群组的所有用户。
     *
     * @param sendInfo        消息对象
     * @param channelContext 当前通道上下文
     * @param wsResponse     WebSocket响应对象
     * @throws Exception 异常
     */
    @Override
    protected void deliverGroupMessage(SendInfo sendInfo, ChannelContext channelContext, WsResponse wsResponse) throws Exception {
        Message message = parseMessage(sendInfo);
        // 单节点群消息投递逻辑
        String groupId = message.getChatId();
        Tio.sendToGroup(channelContext.tioConfig, groupId, wsResponse);
       // vimMessageService.save(message, true);
    }

    /**
     * 处理好友消息。
     * 检查消息的目标用户是否为当前用户，如果是则直接投递消息，否则处理离线消息。
     * 最后将消息发送给自己。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo        SendInfo
     * @param wsResponse     WebSocket响应对象
     */
    @Override
    public void handleFriendMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse) {
        Message message = parseMessage(sendInfo);
        String userId = message.getChatId();
        SetWithLock<ChannelContext> channelContextSetWithLock = Tio.getByUserid(channelContext.tioConfig, userId);
        try {
            if (channelContextSetWithLock == null || channelContextSetWithLock.size() == 0) {
                handleOfflineMessage(message);
            } else {
                deliverOnlineMessage(channelContext, sendInfo, wsResponse);
            }
            sendToSelf(channelContext, message.getFromId(), wsResponse);
        } catch (Exception e) {
            log.error("处理好友消息失败", e);
        }
    }
}
