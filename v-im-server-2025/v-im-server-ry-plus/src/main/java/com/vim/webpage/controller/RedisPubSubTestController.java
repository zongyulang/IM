package com.vim.webpage.controller;

import com.vim.webpage.service.RedisPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 发布/订阅测试控制器
 *
 * @author vim
 */
@Slf4j
@RestController
@RequestMapping("/api/redis/pubsub")
public class RedisPubSubTestController {

    @Resource
    private RedisPublishService redisPublishService;

    /**
     * 发布测试消息
     * GET /api/redis/pubsub/publish?channel=im:message&message=hello
     */
    @GetMapping("/publish")
    public Map<String, Object> publish(
            @RequestParam String channel,
            @RequestParam String message) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            redisPublishService.publish(channel, message);
            result.put("success", true);
            result.put("message", "消息发布成功");
            result.put("channel", channel);
            result.put("content", message);
        } catch (Exception e) {
            log.error("发布消息失败", e);
            result.put("success", false);
            result.put("message", "消息发布失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 发布 IM 消息
     * POST /api/redis/pubsub/im-message
     * Body: {"content": "hello world"}
     */
    @PostMapping("/im-message")
    public Map<String, Object> publishImMessage(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String content = body.get("content");
            if (content == null || content.isEmpty()) {
                result.put("success", false);
                result.put("message", "消息内容不能为空");
                return result;
            }
            
            redisPublishService.publishImMessage(content);
            result.put("success", true);
            result.put("message", "IM消息发布成功");
            result.put("content", content);
        } catch (Exception e) {
            log.error("发布IM消息失败", e);
            result.put("success", false);
            result.put("message", "IM消息发布失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 发布通知消息
     * POST /api/redis/pubsub/notification
     * Body: {"content": "系统通知"}
     */
    @PostMapping("/notification")
    public Map<String, Object> publishNotification(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String content = body.get("content");
            if (content == null || content.isEmpty()) {
                result.put("success", false);
                result.put("message", "通知内容不能为空");
                return result;
            }
            
            redisPublishService.publishNotification(content);
            result.put("success", true);
            result.put("message", "通知消息发布成功");
            result.put("content", content);
        } catch (Exception e) {
            log.error("发布通知消息失败", e);
            result.put("success", false);
            result.put("message", "通知消息发布失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取订阅状态信息
     * GET /api/redis/pubsub/status
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Redis发布/订阅功能已启用");
        result.put("subscribedPatterns", new String[]{"im:*"});
        result.put("info", "使用独立的订阅客户端密码连接");
        return result;
    }
}
