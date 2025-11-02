package com.vim.tio.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "#{@messageLogCollection}")
public class MessageLog {
    
    /**
     * 原始消息内容
     */
    private String content;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送时间
     */
    private Long sendTime;
} 