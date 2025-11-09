package com.vim.webpage.manager.Storage;

import com.vim.webpage.config.StorageConfig;
import com.vim.webpage.enums.FileTypeEnum;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 文件缓存管理器
 */
@Slf4j
@Component
public class RedisFileManager {

    @Resource(name = "webpageRedisTemplate")  // 指定 Bean 名称
    private RedisTemplate<String, Object> redisTemplate;


    @Autowired
    private StorageConfig storageConfig;

    /**
     * 设置文件根路径缓存
     */
    public void setFileRootPath(FileTypeEnum fileType, String rootPath, String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;

            // 设置根路径
            redisTemplate.opsForHash().put(redisKey, "RootPath", rootPath);

            // 计算并设置过期时间
            long expireTime;
            if (fileType == FileTypeEnum.THUMBNAIL || fileType == FileTypeEnum.PREVIEW
                    || fileType == FileTypeEnum.KEY || fileType == FileTypeEnum.M3U8) {
                // 30 天
                expireTime = storageConfig.getPublicFileRootExpireTime();
            } else if (fileType == FileTypeEnum.TS) {
                // 4 小时
                expireTime = storageConfig.getTSfileExpireTime();
            } else {
                // 其他类型使用默认配置
                expireTime = storageConfig.getPublicFileRootExpireTime();
            }
            redisTemplate.expire(redisKey, expireTime, TimeUnit.SECONDS);

            // 对于某些类型的文件，设置额外的标记
            if (fileType == FileTypeEnum.THUMBNAIL || fileType == FileTypeEnum.PREVIEW
                    || fileType == FileTypeEnum.KEY || fileType == FileTypeEnum.M3U8) {
                redisTemplate.opsForHash().put(redisKey, "FileType", fileType.getType());
            } else if (fileType == FileTypeEnum.TS) {
                redisTemplate.opsForHash().put(redisKey, "FileType", fileType.getType());
                // TS 文件可能需要额外的分辨率信息
                redisTemplate.opsForHash().put(redisKey, "Resolution", extractResolution(rootPath));
            }

            log.debug("Set Redis cache for key: {}, rootPath: {}, expireTime: {}s", redisKey, rootPath, expireTime);

        } catch (Exception e) {
            log.error("Failed to set Redis cache for key: {}", key, e);
        }
    }

    /**
     * 获取文件根路径
     */
    public String getFileRootPath(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            Object value = redisTemplate.opsForHash().get(redisKey, "RootPath");
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Failed to get Redis cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * 刷新文件过期时间
     */
    public void refreshExpireTime(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            long expireTime = storageConfig.getPublicFileRootExpireTime();
            redisTemplate.expire(redisKey, expireTime, TimeUnit.SECONDS);
            log.debug("Refreshed expire time for key: {}, expireTime: {}s", redisKey, expireTime);
        } catch (Exception e) {
            log.error("Failed to refresh expire time for key: {}", key, e);
        }
    }

    /**
     * 删除文件缓存
     */
    public void deleteFileCache(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            redisTemplate.delete(redisKey);
            log.debug("Deleted Redis cache for key: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to delete Redis cache for key: {}", key, e);
        }
    }

    /**
     * 检查文件缓存是否存在
     */
    public boolean existsFileCache(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            Boolean exists = redisTemplate.hasKey(redisKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check Redis cache existence for key: {}", key, e);
            return false;
        }
    }

    /**
     * 从文件路径中提取分辨率信息
     */
    private String extractResolution(String rootPath) {
        if (rootPath == null) {
            return "unknown";
        }

        if (rootPath.contains("1080P")) {
            return "1080P";
        } else if (rootPath.contains("720P")) {
            return "720P";
        } else if (rootPath.contains("480P")) {
            return "480P";
        }

        return "unknown";
    }
}
