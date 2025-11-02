package com.vim.tio.task;


import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Set;

/**
 * 定时任务，定时清理redis 里面的多余数据
 *
 * @author 乐天
 * Configuration 主要用于标记配置类，兼备Component的效果。
 * EnableScheduling 开启定时任务
 */
@Slf4j
@Configuration
@EnableScheduling
public class MessageClearTask {

    @Resource
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 定时任务，每个聊天redis 里面只保留最新100条数据
     * 每天4点开始执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void clearRedisMessage() {
        Set<String> keys = redisTemplate.keys("message-*");
        assert keys != null;
        for (String key : keys) {
            Long counted = redisTemplate.opsForZSet().count(key, 0, System.currentTimeMillis());
            if (counted != null) {
                long count = counted - 100;
                if (count > 0) {
                    Long res = redisTemplate.opsForZSet().removeRange(key, 0, count);
                    log.info(StrUtil.format("{}:共清理了{}条数据", key, res));
                }
            }

        }
    }


}
