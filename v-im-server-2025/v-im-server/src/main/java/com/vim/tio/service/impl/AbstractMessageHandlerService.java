package com.vim.tio.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.vim.common.enums.ChatTypeEnum;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.common.enums.SendCodeEnum;
import com.vim.modules.group.result.Group;
import com.vim.sdk.service.VimGroupApiService;
import com.vim.sdk.service.VimMessageService;
import com.vim.tio.messages.Message;
import com.vim.tio.messages.ReadReceipt;
import com.vim.tio.messages.SendInfo;
import com.vim.tio.service.MessageHandlerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.websocket.common.WsResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抽象消息处理服务类，包含处理好友消息、群聊消息、消息已读、加载离线消息等功能。
 * 具体的消息投递逻辑由子类实现。
 */
@Service
@Slf4j
public abstract class AbstractMessageHandlerService implements MessageHandlerService {

    /**
     * 消息服务，用于处理消息的存储和删除。
     */
    @Resource
    protected VimMessageService vimMessageService;

    /**
     * Vim 集群 API 服务，用于获取群组信息和用户权限。
     */
    @Resource
    protected VimGroupApiService vimGroupApiService;

    /**
     * Redis 模板，用于缓存操作。
     */
    @Resource
    protected RedisTemplate<String, String> redisTemplate;


    /**
     * 处理好友消息。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo       消息对象
     * @param wsResponse     WebSocket响应对象
     */
    @Override
    public abstract void handleFriendMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse);


    /**
     * 投递在线用户消息。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo       消息对象
     * @param wsResponse     WebSocket响应对象
     * @throws Exception 异常
     */
    protected abstract void deliverOnlineMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse) throws Exception;

    /**
     * 投递群聊消息。
     *
     * @param sendInfo       sendInfo
     * @param channelContext 当前通道上下文
     * @param wsResponse     WebSocket响应对象
     * @throws Exception 异常
     */
    protected abstract void deliverGroupMessage(SendInfo sendInfo, ChannelContext channelContext, WsResponse wsResponse) throws Exception;


    /**
     * 接受并处理消息。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo       消息信息
     * @throws Exception 异常
     */
    @Override
    public void handleMessage(ChannelContext channelContext, SendInfo sendInfo) throws Exception {
        Message message = parseMessage(sendInfo);
        WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(sendInfo), "utf-8");
        if (ChatTypeEnum.FRIEND.getCode().equals(message.getChatType())) {
            handleFriendMessage(channelContext, sendInfo, wsResponse);
        } else {
            handleGroupMessage(channelContext, sendInfo, wsResponse);
        }
    }

    /**
     * 处理群聊消息。
     *
     * @param channelContext 当前通道上下文
     * @param sendInfo       SendInfo
     * @param wsResponse     WebSocket响应对象
     * @throws Exception 异常
     */
    @Override
    public void handleGroupMessage(ChannelContext channelContext, SendInfo sendInfo, WsResponse wsResponse) throws Exception {
        Message message = parseMessage(sendInfo);
        String groupId = message.getChatId();
        if (!canSendGroupMessage(channelContext, groupId)) {
            log.warn("用户 {} 没有权限发送群 {} 的消息", channelContext.userid, groupId);
            return;
        }
        try {
            handleGroupMessageStorage(message);
            deliverGroupMessage(sendInfo, channelContext, wsResponse);
        } catch (Exception e) {
            log.error("处理群消息失败: groupId={} ", groupId, e);
            throw e;
        }
    }


    /**
     * 处理群聊消息的存储。
     *
     * @param message 消息对象
     * @throws Exception 异常
     */
    protected void handleGroupMessageStorage(Message message) throws Exception {
        vimMessageService.saveOnLine(message, true);
    }

    /**
     * 检查用户是否有权限发送群聊消息。
     *
     * @param channelContext 当前通道上下文
     * @param groupId        群组ID
     * @return 是否有权限发送群聊消息
     */
    protected boolean canSendGroupMessage(ChannelContext channelContext, String groupId) {
        return vimGroupApiService.getUserIdsByGroupId(groupId).contains(channelContext.userid);
    }

    /**
     * 处理消息已读。
     *
     * @param sendInfo 消息信息
     * @throws Exception 异常
     */
    @Override
    public void handleReadMessage(SendInfo sendInfo) throws Exception {
        JSONObject jsonObject = sendInfo.getMessage();
        ReadReceipt readReceipt = JSONUtil.toBean(jsonObject, ReadReceipt.class);
        readReceipt.setTimestamp(System.currentTimeMillis());
        vimMessageService.receipt(readReceipt.getChatId(), readReceipt.getFromId(), readReceipt.getType(), System.currentTimeMillis());
    }

    /**
     * 其他消息直接转发
     *
     * @param channelContext Tio 上下文
     * @param sendInfo       消息信息
     */
    @Override
    public void handleOtherMessage(ChannelContext channelContext, SendInfo sendInfo, String text) {
        Message message = parseMessage(sendInfo);
        String chatId = String.valueOf(message.getChatId());
        WsResponse wsResponse = WsResponse.fromText(text, "utf-8");
        if (message.getChatType().equals(ChatTypeEnum.FRIEND.getCode())) {
            Tio.sendToUser(channelContext.tioConfig, chatId, wsResponse);
        } else {
            Tio.sendToGroup(channelContext.tioConfig, chatId, wsResponse);
        }
    }

    /**
     * 加载离线消息并发送。
     *
     * @param channelContext 当前通道上下文
     * @throws Exception 异常
     */
    @Override
    public void handleOffLineMessage(ChannelContext channelContext) throws Exception {
        String userId = channelContext.userid;
        TioConfig tioConfig = channelContext.tioConfig;

        // 发送私聊离线消息
        sendMessage(tioConfig, vimMessageService.unreadList(userId, null), userId);
        List<Group> groups = vimGroupApiService.getGroups(userId);
        for (Group group : groups) {
            // 发送群聊离线消息
            sendMessage(tioConfig, vimMessageService.unreadGroupList(userId, group.getId()), userId);
        }
    }

    /**
     * 发送消息。
     *
     * @param tioConfig   TIO配置对象
     * @param messageList 消息列表
     * @param userId      用户ID
     * @throws Exception 异常
     */
    protected void sendMessage(TioConfig tioConfig, List<Message> messageList, String userId) throws Exception {
        if (messageList != null && !messageList.isEmpty()) {
            Map<String, List<Message>> map = messageList.stream().collect(Collectors.groupingBy(Message::getChatId));
            for (Map.Entry<String, List<Message>> entry : map.entrySet()) {
                List<Message> messages = entry.getValue();
                if (!messages.isEmpty()) {
                    Message message = messages.get(0);
                    send(tioConfig, userId, message);
                    Thread.sleep(500);
                }
                messages.remove(0);
                for (Message message : messages) {
                    send(tioConfig, userId, message);
                }
            }
        }
    }

    /**
     * 保存消息并发送给用户。
     *
     * @param tioConfig TIO配置对象
     * @param userId    用户ID
     * @param message   消息对象
     * @throws Exception 异常
     */
    protected void send(TioConfig tioConfig, String userId, Message message) throws Exception {
        vimMessageService.saveOnLine(message, true);
        SendInfo sendInfo = new SendInfo(SendCodeEnum.MESSAGE.getCode(), JSONUtil.parseObj(message));
        WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(sendInfo), "utf-8");
        Tio.sendToUser(tioConfig, userId, wsResponse);
    }

    /**
     * 解析消息。
     *
     * @param sendInfo 消息信息
     * @return 解析后的消息对象
     */
    protected Message parseMessage(SendInfo sendInfo) {
        JSONObject jsonObject = sendInfo.getMessage();
        jsonObject.set("timestamp", System.currentTimeMillis());
        if (StrUtil.isBlank((String) sendInfo.getMessage().get("id"))) {
            jsonObject.set("id", IdUtil.getSnowflakeNextIdStr());
        }
        return JSONUtil.toBean(jsonObject, Message.class);
    }

    /**
     * 绑定用户群组。
     *
     * @param channelContext 当前通道上下文
     * @param userId         用户ID
     */
    @Override
    public void bindUserGroups(ChannelContext channelContext, String userId) {
        List<Group> groups = vimGroupApiService.getGroups(userId);
        for (Group group : groups) {
            if (DictSwitchEnum.NO.getCode().equals(group.getProhibition())) {
                Tio.bindGroup(channelContext, group.getId());
            }
        }
    }

    /**
     * 处理离线消息。
     *
     * @param message 消息对象
     * @throws Exception 异常
     */
    protected void handleOfflineMessage(Message message) throws Exception {
        vimMessageService.saveOffLine(message);
    }

    /**
     * 发送消息给自己。
     *
     * @param channelContext 当前通道上下文
     * @param userId         用户ID
     * @param wsResponse     WebSocket响应对象
     */
    protected void sendToSelf(ChannelContext channelContext, String userId, WsResponse wsResponse) {
        Tio.sendToUser(channelContext.tioConfig, userId, wsResponse);
    }
}
