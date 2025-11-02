package com.vim.tio.service;

public interface MessageLogService {
    
    /**
     * 记录消息日志
     *
     * @param text 原始消息内容
     * @param userId userId
     */
    void logMessage(String text, String userId);
} 