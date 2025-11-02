package com.vim.sdk.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongodb.client.result.DeleteResult;
import com.vim.common.enums.ChatTypeEnum;
import com.vim.common.enums.SendCodeEnum;
import com.vim.common.utils.ChatUtils;
import com.vim.sdk.service.VimMessageService;
import com.vim.tio.StartTioRunner;
import com.vim.tio.messages.Message;
import com.vim.tio.messages.ReadReceipt;
import com.vim.tio.messages.SendInfo;
import com.vim.tio.service.ConnStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tio.core.Tio;
import org.tio.server.TioServerConfig;
import org.tio.websocket.common.WsResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息处理
 *
 * @author 乐天
 */
@Slf4j
@Service
@DS("master")
public class VimMessageServiceImpl implements VimMessageService {

    public static final String CHAT_KEY = "chat_key";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ConnStatusService connStatusService;

    @Resource
    private ApplicationContext applicationContext;


    /**
     * 添加消息到redis 队列，有可能受到的是离线消息，所以要去删除下离线消息里面的记录
     *
     * @param message  消息
     * @param isOnline 是否在线
     * @throws Exception 抛出异常
     */
    @Override
    public void saveOnLine(Message message, boolean isOnline) throws Exception {
        saveChatMessageToDatabase(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOffLine(Message message) throws Exception {
        saveChatMessageToDatabase(message);
        saveOffLineChatMessageToDatabase(message);
    }

    /**
     * 根据消息ID和聊天键获取消息
     *
     * @param id      消息ID
     * @param chatKey 聊天键
     * @return 消息对象
     * @throws Exception 抛出异常
     */
    @Override
    public Message get(String id, String chatKey) throws Exception {
        String collectionName = ChatUtils.getCollectionNameByChatKey(chatKey);
        return mongoTemplate.findById(id, Message.class, collectionName);
    }

    /**
     * 删除消息
     *
     * @param message  消息对象
     * @param isOnline 是否在线
     * @throws Exception 抛出异常
     */
    @Override
    public void delete(Message message, boolean isOnline) throws Exception {
        String key = isOnline ? ChatUtils.getChatKey(message.getFromId(), message.getChatId(), message.getChatType()) : StrUtil.format(ChatUtils.UNREAD_TEMPLATE, message.getChatId());
        Set<String> set = redisTemplate.opsForZSet().range(key, 0, -1);
        // 如果是离线消息，去离线队列里处理
        if (set != null && !set.isEmpty()) {
            set.forEach(item -> {
                Message msg = toMessage(item);
                if (msg != null && msg.getId().equals(message.getId())) {
                    redisTemplate.opsForZSet().remove(key, item);
                }
            });
        }
        mongoTemplate.remove(message, ChatUtils.getCollectionName(message.getFromId(), message.getChatId(), message.getChatType()));
    }

    /**
     * 异步保存消息到数据库
     *
     * @param message 消息对象
     */
    public void saveChatMessageToDatabase(Message message) {
        String chatId = message.getChatId();
        String chatKey = ChatUtils.getChatKey(message.getFromId(), chatId, message.getChatType());
        message.setChatKey(chatKey);
        String collectionName = ChatUtils.getCollectionName(message.getFromId(), chatId, message.getChatType());
        mongoTemplate.save(message, collectionName);
    }

    /**
     * 保存离线消息到monngodb
     *
     * @param message
     */
    public void saveOffLineChatMessageToDatabase(Message message) {
        String chatId = message.getChatId();
        String chatKey = ChatUtils.getChatKey(message.getFromId(), chatId, message.getChatType());
        message.setChatKey(chatKey);
        String collectionName = ChatUtils.getOffLineCollectionName(chatId);
        mongoTemplate.save(message, collectionName);
    }

    /**
     * 查询消息
     *
     * @param chatId   聊天室ID
     * @param fromId   发送人ID
     * @param chatType 聊天类型（私聊/群聊）
     * @param pageSize 每页多少条
     * @return 消息列表
     */
    @Override
    public List<Message> list(String chatId, String fromId, String chatType, Long pageSize) {
        String collectionName = ChatUtils.getCollectionName(fromId, chatId, chatType);

        // 查询最近的消息（按时间倒序）
        Query query = Query.query(Criteria.where("chat_id").is(chatId))
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(pageSize.intValue());

        List<Message> recentMessages = mongoTemplate.find(query, Message.class, collectionName);

        // 反转列表，保持时间正序
        Collections.reverse(recentMessages);

        // 加上未读消息
        recentMessages.addAll(unreadList(chatId, fromId));

        return recentMessages;
    }

    /**
     * 分页查询消息
     *
     * @param chatId      聊天室ID
     * @param fromId      发送人ID
     * @param messageType 消息类型
     * @param chatType    聊天类型（私聊/群聊）
     * @param searchText  搜索文本
     * @param dataRange   时间范围
     * @param page        分页对象
     * @return 分页后的消息对象
     */
    @Override
    public Page<Message> page(String chatId, String fromId, String messageType, String chatType, String searchText, Date[] dataRange, Page<Message> page) {
        Query query = new Query();
        if (StrUtil.isNotBlank(messageType)) {
            query.addCriteria(Criteria.where("message_type").is(messageType));
        }

        if (dataRange != null) {
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("timestamp").gte(dataRange[0].getTime()),
                    Criteria.where("timestamp").lt(dataRange[1].getTime() + 24 * 60 * 60 * 1000)
            ));
        }
        if (StrUtil.isNotBlank(searchText)) {
            String regex = ".*" + searchText + ".*";
            query.addCriteria(Criteria.where("content").regex(regex));
        }
        String key = ChatUtils.getChatKey(fromId, chatId, chatType);
        query.addCriteria(Criteria.where(CHAT_KEY).is(key));
        String collectionName = ChatUtils.getCollectionName(fromId, chatId, chatType);

        // 查询总条数
        page.setTotal(mongoTemplate.count(query, Message.class, collectionName));

        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        int skip = (int) ((page.getCurrent() - 1) * page.getSize());
        query.skip(skip).limit((int) page.getSize());
        page.setRecords(mongoTemplate.find(query, Message.class, collectionName));
        return page;
    }

    /**
     * 根据聊天ID、聊天类型和发送人ID查询消息
     *
     * @param chatId    聊天室ID
     * @param chatType  聊天类型（私聊/群聊）
     * @param fromId    发送人ID
     * @param messageId 消息ID
     * @return 消息列表
     */
    @Override
    public List<Message> list(String chatId, String chatType, String fromId, String messageId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CHAT_KEY).is(ChatUtils.getChatKey(fromId, chatId, chatType)));
        if (StrUtil.isNotBlank(messageId)) {
            query.addCriteria(Criteria.where("id").is(messageId));
        }
        query.with(Sort.by(Sort.Direction.DESC, "id"));
        return mongoTemplate.find(query, Message.class, ChatUtils.getCollectionName(fromId, chatId, chatType));
    }

    /**
     * 读取未读消息
     * 未读消息只存私聊消息，群聊消息还在群列表里
     *
     * @param chatId 聊天室ID = toUserId
     * @param fromId 发送人ID
     * @return 未读消息列表
     */
    @Override
    public List<Message> unreadList(String chatId, String fromId) {
        return unreadListFromMongo(chatId, fromId);
    }

    public List<Message> unreadListFromMongo(String chatId, String fromId) {
        String collectionName = ChatUtils.getOffLineCollectionName(chatId);
        Criteria criteria = new Criteria();
        if (StrUtil.isNotBlank(fromId)) {
            criteria.and("from_id").is(fromId);
        }
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.ASC, "timestamp")); // 按时间排序
        // 从MongoDB查询未读消息
        return mongoTemplate.find(query, Message.class, collectionName);
    }

    /**
     * 未读群消息，服务器时间是个很重要的数据，时间不对可能造成离线消息不能全部发送
     *
     * @param userId 用户ID
     * @param chatId 群ID
     * @return 未读群消息列表
     */
    @Override
    public List<Message> unreadGroupList(String userId, String chatId) {
        String key = ChatUtils.getReadKey(userId, chatId);
        String value = redisTemplate.opsForValue().get(key);
        long score = -1;
        if (StrUtil.isNotBlank(value)) {
            score = Long.parseLong(value);
        }
        String collectionName = ChatUtils.getCollectionName(userId, chatId, ChatTypeEnum.GROUP.getCode());
//        Set<String> set = redisTemplate.opsForZSet().rangeByScore(StrUtil.format(ChatUtils.GROUP_TEMPLATE, chatId), score, System.currentTimeMillis());
//        if (set != null) {
//            return set.stream().map(this::toMessage).collect(Collectors.toList());
//        }
        // 构建查询条件：timestamp 在 [score, currentTime] 范围内
        Criteria criteria = Criteria.where("timestamp")
                .gte(score)  // 大于等于 score
                .lte(System.currentTimeMillis());  // 小于等于当前时间

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.ASC, "timestamp"));  // 按时间升序
        return mongoTemplate.find(query, Message.class, collectionName);
    }

    /**
     * 已读消息的条数
     *
     * @param chatId 聊天室ID
     * @param formId 发送人ID
     * @param type   聊天室类型
     * @return 已读消息数量
     */
    @Override
    public Long count(String chatId, String formId, String type) {
        String key = ChatUtils.getChatKey(formId, chatId, type);
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 读消息，并持久化到redis
     *
     * @param chatId    聊天室ID
     * @param fromId    消息读取人ID
     * @param type      聊天室类型
     * @param timestamp 系统时间
     */
    @Override
    public void receipt(String chatId, String fromId, String type, long timestamp) {
        String key = ChatUtils.getReadKey(fromId, chatId);
        redisTemplate.opsForValue().set(key, String.valueOf(timestamp));
        clearOfflineMessage(chatId, fromId, type);
        sendReceiptMessage(chatId, fromId, type, timestamp);
    }

    /**
     * 清除离线消息
     *
     * @param chatId 聊天室ID
     * @param fromId 消息读取人ID
     * @param type   聊天室类型
     */
//    private void clearOfflineMessage(String chatId, String fromId, String type) {
//        // 前置条件检查：非私聊或非新连接直接退出
//        if (!ChatTypeEnum.FRIEND.getCode().equals(type) || !connStatusService.getConnStatus(fromId)) {
//            return;
//        }
//
//        String unreadKey = StrUtil.format(ChatUtils.UNREAD_TEMPLATE, fromId);
//        Set<String> members = redisTemplate.opsForZSet().range(unreadKey, 0, -1);
//        if (members == null || members.isEmpty()) {
//            connStatusService.setConnStatus(fromId, false);
//            return;
//        }
//        // 批量收集待删除的成员
//        List<String> toRemove = members.stream()
//                .map(this::toMessage)
//                .filter(message -> message != null && chatId.equals(message.getFromId()))
//                .map(this::serializeMessage)  // 确保使用与存储时相同的序列化方式
//                .toList();
//
//        // 批量删除操作
//        if (!toRemove.isEmpty()) {
//            redisTemplate.opsForZSet().remove(unreadKey, toRemove.toArray());
//        }
//        connStatusService.setConnStatus(fromId, false);
//    }

    /**
     * 清除离线消息
     *
     * @param chatId
     * @param fromId
     * @param type
     */
    private void clearOfflineMessage(String chatId, String fromId, String type) {
        // 前置条件检查：非私聊或非新连接直接退出
        if (!ChatTypeEnum.FRIEND.getCode().equals(type) || !connStatusService.getConnStatus(fromId)) {
            return;
        }

        String collectionName = ChatUtils.getOffLineCollectionName(fromId);

        // 更精确的查询条件：指定会话和接收者
        Criteria criteria = new Criteria();
        criteria.and("chat_id").is(fromId);
        Query query = Query.query(criteria);
        // 检查是否有离线消息
        long count = mongoTemplate.count(query, collectionName);
        if (count == 0) {
            connStatusService.setConnStatus(fromId, false);
            return;
        }

        DeleteResult result = mongoTemplate.remove(query, collectionName);
        if (result.getDeletedCount() > 0) {
            log.info("删除用户 {} 的 {} 条离线消息", chatId, result.getDeletedCount());
        }
        connStatusService.setConnStatus(fromId, false);
    }

    /**
     * 序列化消息对象为JSON字符串
     *
     * @param message 消息对象
     * @return JSON字符串
     */
    private String serializeMessage(Message message) {
        return JSON.toJSONString(message);
    }

    /**
     * 将JSON字符串转换为消息对象
     *
     * @param str JSON字符串
     * @return 消息对象
     */
    private Message toMessage(String str) {
        return JSONUtil.toBean(str, Message.class);
    }

    /**
     * 发送已读消息回执给对方
     *
     * @param chatId    聊天对象ID
     * @param fromId    发送人ID
     * @param type      聊天类型
     * @param timestamp 时间戳
     */
    private void sendReceiptMessage(String chatId, String fromId, String type, long timestamp) {
        ReadReceipt readReceipt = new ReadReceipt();
        readReceipt.setChatId(chatId);
        readReceipt.setFromId(fromId);
        readReceipt.setTimestamp(timestamp);
        readReceipt.setType(type);
        SendInfo sendInfo = new SendInfo(SendCodeEnum.READ.getCode(), JSONUtil.parseObj(readReceipt));
        StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
        TioServerConfig tioServerConfig = startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
        WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(sendInfo), "utf-8");
        Tio.sendToUser(tioServerConfig, chatId, wsResponse);

    }

    /**
     * 推送消息
     *
     * @param message 消息对象
     * @throws Exception 抛出异常
     */
    @Override
    public void push(Message message) throws Exception {
        SendInfo sendInfo = new SendInfo(SendCodeEnum.MESSAGE.getCode(), JSONUtil.parseObj(message));
        StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
        TioServerConfig tioServerConfig = startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
        WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(sendInfo), "utf-8");
        this.sendMessage(tioServerConfig, message, sendInfo, wsResponse, message.getChatId());
    }


    /**
     * 清除聊天消息
     *
     * @param chatId   聊天室ID
     * @param fromId   发送人ID
     * @param chatType 聊天类型
     */
    @Override
    public void clearChatMessage(String chatId, String fromId, String chatType) {
        String key = ChatUtils.getChatKey(fromId, chatId, chatType);
        // 清除已读消息
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
        // 清除未读消息
        redisTemplate.opsForZSet().removeRangeByScore(StrUtil.format(ChatUtils.UNREAD_TEMPLATE, chatId), 0, System.currentTimeMillis());
        // 清除数据库消息
        Query query = new Query();
        query.addCriteria(Criteria.where(CHAT_KEY).is(ChatUtils.getChatKey(fromId, chatId, chatType)));
        mongoTemplate.remove(query, Message.class, ChatUtils.getCollectionName(fromId, chatId, chatType));
    }

}
