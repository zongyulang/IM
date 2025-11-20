package com.vim.webpage.listener;

import com.vim.webpage.service.RedisCache2Mongodb.IRedisCache2MongodbService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis 消息监听器示例
 * 用于接收 Redis 发布/订阅的消息
 *
 * @author fres
 */
@Slf4j
@Component
public class RedisMessageListener implements MessageListener {

    @Value("${spring.data.redis.database1:0}")
    private int database;

    @Autowired
    private IRedisCache2MongodbService redisCache2MongodbService;

    /**
     * 处理接收到的消息
     *
     * @param message 消息内容
     * @param pattern 订阅模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.warn("【DEBUG】onMessage 被调用！");
        try {
            // 获取频道名称
            String channel = new String(message.getChannel());

            // 获取消息内容
            String body = new String(message.getBody());

            // 获取订阅模式（如果使用的是模式订阅）
            String patternStr = pattern != null ? new String(pattern) : "N/A";

            log.info("收到 Redis 订阅消息 - 频道: {}, 模式: {}, 内容: {}", channel, patternStr, body);

            // 模拟 Node 的 pmessage 分发
            if (isKeyEventChannel(channel)) {
                handleKeyEvent(channel, body);
            } else {
                handleBusinessChannel(channel, body);
            }

        } catch (Exception e) {
            log.error("处理 Redis 订阅消息时发生错误", e);
        }
    }

    private boolean isKeyEventChannel(String channel) {
        return channel.startsWith("__keyevent@");
    }

    private void handleKeyEvent(String channel, String key) {
        // 频道格式: __keyevent@<db>__:<event>
        Integer db = null;
        String event = null;
        try {
            int at = channel.indexOf('@');
            int sep = channel.indexOf("__:");
            if (at > 0 && sep > at + 1) {
                String dbStr = channel.substring(at + 1, sep);
                db = Integer.parseInt(dbStr);
                event = channel.substring(sep + 3); // 跳过 "__:"
            }
        } catch (Exception ignore) {
        }

        if (event == null) {
            log.debug("无法解析键事件频道: {} -> {}", channel, key);
            return;
        }

        switch (event) {
            case "hexpired": // 非标准事件，按需处理
                log.info("[keyevent] db={} event=hexpired key={}", db, key);
                dropPublicFiles(key);
                break;
            case "expired":
                log.info("[keyevent] db={} event=expired key={}", db, key);
                disposePmessage(key);
                break;
            default:
                log.debug("其他键事件: db={} event={} key={}", db, event, key);
        }
    }

    private void dropPublicFiles(String key) {
        log.info("hexpired 处理，key: {}", key);
        // TODO: 删除公共文件的具体实现
    }

    private void disposePmessage(String key) {
        log.info("expired 处理，key: {}", key);
        // TODO: 处理过期键的具体实现
    }

    private void handleBusinessChannel(String channel, String message) {
        switch (channel) {
            case "im:message":
                log.info("处理即时消息: {}", message);
                break;
            case "im:notice":
            case "im:notification":
                log.info("处理通知消息: {}", message);
                break;
            case "im:sync_monthly_ranking":
                log.info("🔔 接收到月度排行榜同步请求");
                handleMonthlyRankingSync(message);
                break;
            default:
                log.debug("收到业务频道消息: {} - {}", channel, message);
        }
    }

    /**
     * 处理月度排行榜同步请求
     */
    private void handleMonthlyRankingSync(String message) {
        try {
            log.info("📊 开始处理月度排行榜同步: {}", message);
            boolean success = redisCache2MongodbService.syncMonthlyVideoRanking();
            if (success) {
                log.info("✅ 月度排行榜同步成功");
            } else {
                log.warn("⚠️ 月度排行榜同步失败或无数据");
            }
        } catch (Exception e) {
            log.error("❌ 处理月度排行榜同步时发生错误: {}", e.getMessage(), e);
        }
    }
}
