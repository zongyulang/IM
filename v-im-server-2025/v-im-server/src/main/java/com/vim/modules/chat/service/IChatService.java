package com.vim.modules.chat.service;

import com.vim.modules.chat.result.Chat;

import java.util.List;

/**
 * 聊天服务接口
 *
 * @author vim
 * @since 2024-01-01
 */
public interface IChatService {
    /**
     * 添加聊天
     *
     * @param chat 聊天对象
     * @throws Exception 异常信息
     */
    void add(Chat chat) throws Exception;
    
    /**
     * 更新聊天
     *
     * @param chat 聊天对象
     * @throws Exception 异常信息
     */
    void update(Chat chat) throws Exception;
    
    /**
     * 批量更新聊天
     *
     * @param chatList 聊天列表
     */
    void batchUpdate(List<Chat> chatList);
    
    /**
     * 移动聊天位置到顶部
     *
     * @param chatId 聊天ID
     */
    void move(String chatId);
    
    /**
     * 获取置顶聊天列表
     *
     * @return 置顶聊天列表
     */
    List<Chat> getTopList();
    
    /**
     * 获取普通聊天列表
     *
     * @return 聊天列表
     */
    List<Chat> getList();
    
    /**
     * 删除聊天
     *
     * @param chatId 聊天ID
     */
    void delete(String chatId);
    
    /**
     * 置顶聊天
     *
     * @param chatId 聊天ID
     */
    void top(String chatId);
    
    /**
     * 取消置顶聊天
     *
     * @param chatId 聊天ID
     */
    void cancelTop(String chatId);
} 