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
    private long publicFileRootExpireTime = 30L * 24 * 60 * 60;//30天
    
    /**
     * TS文件过期时间(秒)
     */
    private long TSfileExpireTime = 4L * 60 * 60;//4小时
    
    /**
     * 本地存储根路径
     */
    private String localBasePath = "public";
    
    /**
     * Redis 键前缀
     */
    private String redisKeyPrefix = "file:cache:";
}
