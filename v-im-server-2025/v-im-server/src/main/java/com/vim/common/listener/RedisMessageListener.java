package com.vim.common.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis 消息监听器示例
 * 用于接收 Redis 发布/订阅的消息
 *
 * @author vim
 */
@Slf4j
@Component
@Profile("prod")
public class RedisMessageListener implements MessageListener {

    /**
     * 处理接收到的消息
     *
     * @param message 消息内容
     * @param pattern 订阅模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 获取频道名称
            String channel = new String(message.getChannel());
            
            // 获取消息内容
            String body = new String(message.getBody());
            
            // 获取订阅模式（如果使用的是模式订阅）
            String patternStr = pattern != null ? new String(pattern) : "N/A";
            
            log.info("收到 Redis 订阅消息 - 频道: {}, 模式: {}, 内容: {}", channel, patternStr, body);
            
            // 在这里添加您的业务逻辑
            handleMessage(channel, body);
            
        } catch (Exception e) {
            log.error("处理 Redis 订阅消息时发生错误", e);
        }
    }

    /**
     * 处理具体的业务逻辑
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    private void handleMessage(String channel, String message) {
        // TODO: 根据不同的频道执行不同的业务逻辑
        switch (channel) {
            case "im:message":
                log.info("处理即时消息: {}", message);
                // 处理即时消息的业务逻辑
                break;
            case "im:notification":
                log.info("处理通知消息: {}", message);
                // 处理通知消息的业务逻辑
                break;
            default:
                log.debug("收到未知频道的消息: {} - {}", channel, message);
                break;
        }
    }
}
