package com.vim.modules.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.vim.common.utils.VimUtil;
import com.vim.modules.chat.result.Chat;
import com.vim.modules.chat.service.IChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 *
 * @author vim
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ChatServiceImpl implements IChatService {

    /** Redis中聊天列表key */
    private static final String CHAT_LIST = "chat:list:{}";
    /** Redis中置顶聊天列表key */
    private static final String TOP_CHAT_LIST = "chat:top:list:{}";
    /** Redis中单个聊天key */
    private static final String CHAT = "{}:chat:{}";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void add(Chat chat) throws Exception {
        String userId = VimUtil.getLoginId();
        String chatListKey = StrUtil.format(CHAT_LIST, userId);
        addToListIfNotExists(chatListKey, chat.getId());
        String chatKey = StrUtil.format(CHAT, userId, chat.getId());
        redisTemplate.opsForValue().set(chatKey, JSON.toJSONString(chat));
    }

    @Override
    public void update(Chat chat) throws Exception {
        String userId = VimUtil.getLoginId();
        String chatKey = StrUtil.format(CHAT, userId, chat.getId());
        redisTemplate.opsForValue().set(chatKey, JSON.toJSONString(chat));
    }

    @Override
    public void batchUpdate(List<Chat> chatList) {
        String userId = VimUtil.getLoginId();
        chatList.forEach(chat -> {
            String chatKey = StrUtil.format(CHAT, userId, chat.getId());
            try {
                redisTemplate.opsForValue().set(chatKey, JSON.toJSONString(chat));
            } catch (Exception e) {
                log.error("json解析错误：", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void move(String chatId) {
        String userId = VimUtil.getLoginId();
        String chatListKey = StrUtil.format(CHAT_LIST, userId, chatId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        Long index = listOps.indexOf(chatListKey, chatId);
        if (index >= 0) {
            listOps.remove(chatListKey, 0, chatId);
            listOps.leftPush(chatListKey, chatId);
        }
    }

    @Override
    public List<Chat> getTopList() {
        return getChats(TOP_CHAT_LIST);
    }

    private List<Chat> getChats(String topChatList) {
        String key = StrUtil.format(topChatList, VimUtil.getLoginId());
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        List<String> chatIds = listOps.range(key, 0, -1);
        return getChats(key, chatIds == null ? new ArrayList<>() : chatIds);
    }

    @Override
    public List<Chat> getList() {
        return getChats(CHAT_LIST);
    }

    @Override
    public void delete(String chatId) {
        String userId = VimUtil.getLoginId();
        String chatKey = StrUtil.format(CHAT_LIST, userId);
        String topChatKey = StrUtil.format(TOP_CHAT_LIST, userId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(chatKey, 0, chatId);
        listOps.remove(topChatKey, 0, chatId);
    }

    @Override
    public void top(String chatId) {
        String userId = VimUtil.getLoginId();
        String chatKey = StrUtil.format(CHAT_LIST, userId);
        String topChatKey = StrUtil.format(TOP_CHAT_LIST, userId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(chatKey, 0, chatId);
        addToListIfNotExists(topChatKey, chatId);
    }

    @Override
    public void cancelTop(String chatId) {
        String userId = VimUtil.getLoginId();
        String chatKey = StrUtil.format(CHAT_LIST, userId);
        String topChatKey = StrUtil.format(TOP_CHAT_LIST, userId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(topChatKey, 0, chatId);
        addToListIfNotExists(chatKey, chatId);
    }

    /**
     * 获取聊天列表
     *
     * @param key Redis键
     * @param chatIds 聊天ID列表
     * @return 聊天列表
     */
    private List<Chat> getChats(String key, List<String> chatIds) {
        String userId = VimUtil.getLoginId();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        ListOperations<String, String> listOps = redisTemplate.opsForList();

        return chatIds.stream().map(chatId -> {
            String chatKey = StrUtil.format(CHAT, userId, chatId);
            String str = valueOperations.get(chatKey);
            return JSON.parseObject(str, Chat.class);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 将聊天ID添加到列表中（如果不存在）
     *
     * @param key Redis键
     * @param chatId 聊天ID
     */
    private void addToListIfNotExists(String key, String chatId) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        Long size = listOps.size(key);
        if (size != null) {
            for (int i = 0; i < size; i++) {
                if (chatId.equals(listOps.index(key, i))) {
                    return;
                }
            }
            listOps.leftPush(key, chatId);
        }
    }
} 