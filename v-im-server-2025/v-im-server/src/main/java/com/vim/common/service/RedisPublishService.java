package com.vim.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * Redis 发布服务
 * 用于向 Redis 频道发布消息
 *
 * @author vim
 */
@Slf4j
@Service
@Profile("prod")
public class RedisPublishService {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 发布消息到指定频道
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public void publish(String channel, String message) {
        try {
            log.info("发布消息到频道: {}, 内容: {}", channel, message);
            redisTemplate.convertAndSend(channel, message);
            log.info("消息发布成功");
        } catch (Exception e) {
            log.error("发布消息失败: channel={}, message={}", channel, message, e);
            throw new RuntimeException("发布消息失败", e);
        }
    }

    /**
     * 发布 IM 消息
     *
     * @param message 消息内容
     */
    public void publishImMessage(String message) {
        publish("im:message", message);
    }

    /**
     * 发布通知消息
     *
     * @param message 消息内容
     */
    public void publishNotification(String message) {
        publish("im:notification", message);
    }

    /**
     * 发布对象类型的消息
     *
     * @param channel 频道名称
     * @param object  消息对象
     */
    public void publishObject(String channel, Object object) {
        try {
            log.info("发布对象消息到频道: {}, 类型: {}", channel, object.getClass().getSimpleName());
            redisTemplate.convertAndSend(channel, object);
            log.info("对象消息发布成功");
        } catch (Exception e) {
            log.error("发布对象消息失败: channel={}", channel, e);
            throw new RuntimeException("发布对象消息失败", e);
        }
    }
}
