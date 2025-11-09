package com.vim.webpage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 存储配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {

    /**
     * 公共文件根路径过期时间(秒)
     */
    private long publicFileRootExpireTime = 30L * 24 * 60 * 61;// 30天

    /**
     * 公共文件最大过期时间(秒) - 用于 Thumbnail/Preview/Key/M3U8
     */
    private long publicFileMaxExpireTime = 30L * 24 * 60 * 60;// 30天

    /**
     * TS文件过期时间(秒)
     */
    private long TSfileExpireTime = 4L * 60 * 60;// 4小时

    /**
     * 删除锁的过期时间，防止死锁(秒)，必须设置过期时间，否则文件删除不成功
     */
    private long lockTimeoutSeconds = 60;// 60秒
    /**
     * 本地存储根路径
     */
    private String localBasePath = "public";

    /**
     * Redis 键前缀
     */
    private String redisKeyPrefix = "file:cache:";
}
