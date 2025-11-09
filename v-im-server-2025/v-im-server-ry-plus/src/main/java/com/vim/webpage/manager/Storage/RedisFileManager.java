package com.vim.webpage.manager.Storage;

import com.vim.webpage.config.StorageConfig;
import com.vim.webpage.enums.FileTypeEnum;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 文件缓存管理器
 * 使用 Hash 字段级别的过期时间（HEXPIRE）
 */
@Slf4j
@Component
public class RedisFileManager {

    @Resource(name = "webpageRedisTemplate")  // 指定 Bean 名称
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StorageConfig storageConfig;

    /**
     * 设置文件根路径缓存并为 Hash 字段设置独立过期时间
     * 对应 Node.js 的 setTypeRedis 方法
     */
    public void setFileRootPath(FileTypeEnum fileType, String rootPath, String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            
            // 1. 设置 RootPath 字段
            redisTemplate.opsForHash().put(redisKey, "RootPath", rootPath);
            log.debug("Set RootPath for key: {}, root: {}", redisKey, rootPath);
            
            // 2. 为 RootPath 字段设置过期时间
            long rootExpireSeconds = storageConfig.getPublicFileRootExpireTime();
            setHashFieldExpire(redisKey, "RootPath", rootExpireSeconds);
            
            // 3. 根据文件类型设置对应字段（字段名为文件类型名称，值为 "1"）
            String typeFieldName = getTypeFieldName(fileType);
            long typeExpireSeconds;
            
            if (fileType == FileTypeEnum.THUMBNAIL || fileType == FileTypeEnum.PREVIEW 
                    || fileType == FileTypeEnum.KEY || fileType == FileTypeEnum.M3U8) {
                typeExpireSeconds = storageConfig.getPublicFileMaxExpireTime();
            } else if (fileType == FileTypeEnum.TS) {
                typeExpireSeconds = storageConfig.getTSfileExpireTime();
            } else {
                log.warn("Unknown file type, skipping type field: {}", fileType);
                return;
            }
            
            // 4. 设置类型字段（值为 "1" 表示存在）
            redisTemplate.opsForHash().put(redisKey, typeFieldName, "1");
            log.debug("Set type field: {} = 1 for key: {}", typeFieldName, redisKey);
            
            // 5. 为类型字段设置过期时间
            setHashFieldExpire(redisKey, typeFieldName, typeExpireSeconds);
            
            log.info("✅ Set Redis hash for type: {}, key: {}, root: {}, rootExpire: {}s, typeExpire: {}s", 
                    typeFieldName, redisKey, rootPath, rootExpireSeconds, typeExpireSeconds);
            
        } catch (Exception e) {
            log.error("❌ Failed to set Redis hash for key: {}", key, e);
            throw new RuntimeException("Failed to set file root path in Redis", e);
        }
    }

    /**
     * 设置 Redis Hash 字段的过期时间
     * 使用 HEXPIRE 命令（Redis 7.4+）
     * 命令格式: HEXPIRE key seconds FIELDS numfields field [field ...]
     */
    private void setHashFieldExpire(String key, String field, long expireSeconds) {
        try {
            // 构建 Lua 脚本执行 HEXPIRE
            // HEXPIRE key seconds FIELDS 1 field
            String luaScript = 
                "if redis.call('hexists', KEYS[1], ARGV[2]) == 1 then " +
                "    local result = redis.call('hexpire', KEYS[1], ARGV[1], 'FIELDS', 1, ARGV[2]) " +
                "    if result and result[1] == 1 then " +
                "        return 1 " +
                "    else " +
                "        return 0 " +
                "    end " +
                "else " +
                "    return -1 " +
                "end";
            
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
            
            Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(expireSeconds),
                field
            );
            
            if (result != null && result == 1) {
                log.debug("✅ Set field expire: key={}, field={}, seconds={}", key, field, expireSeconds);
            } else if (result != null && result == -1) {
                log.warn("⚠️ Field does not exist: key={}, field={}", key, field);
            } else {
                log.warn("⚠️ HEXPIRE failed or returned unexpected result: key={}, field={}, result={}", 
                        key, field, result);
            }
            
        } catch (Exception e) {
            // 如果 HEXPIRE 不支持（Redis < 7.4），记录警告但不影响主流程
            log.warn("⚠️ HEXPIRE not supported or failed for key: {}, field: {}. Error: {}", 
                    key, field, e.getMessage());
            log.warn("💡 Please upgrade Redis to 7.4+ for hash field expiration support");
            
            // 可选：降级为全局键过期（不推荐，但可作为兜底方案）
            // redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 获取文件类型对应的字段名称
     * 对应 Node.js 中的 type 参数
     */
    private String getTypeFieldName(FileTypeEnum fileType) {
        switch (fileType) {
            case THUMBNAIL:
                return "Thumbnail";
            case PREVIEW:
                return "preview";
            case KEY:
                return "key";
            case M3U8:
                return "m3u8";
            case TS:
                return "ts";
            default:
                return fileType.getType();
        }
    }

    /**
     * 刷新 RootPath 字段的过期时间
     */
    public void refreshExpireTime(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            
            // 检查键是否存在
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (!Boolean.TRUE.equals(exists)) {
                log.warn("⚠️ Key does not exist, cannot refresh: {}", redisKey);
                return;
            }
            
            // 刷新 RootPath 字段的过期时间
            long expireSeconds = storageConfig.getPublicFileRootExpireTime();
            setHashFieldExpire(redisKey, "RootPath", expireSeconds);
            
            log.debug("✅ Refreshed RootPath expire time for key: {}, seconds: {}", redisKey, expireSeconds);
            
        } catch (Exception e) {
            log.error("❌ Failed to refresh expire time for key: {}", key, e);
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
            log.error("❌ Failed to get root path for key: {}", key, e);
            return null;
        }
    }

    /**
     * 删除文件类型字段
     */
    public void deleteFileTypeField(String key, FileTypeEnum fileType) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            String typeFieldName = getTypeFieldName(fileType);
            
            redisTemplate.opsForHash().delete(redisKey, typeFieldName);
            log.info("✅ Deleted type field: {} for key: {}", typeFieldName, redisKey);
        } catch (Exception e) {
            log.error("❌ Failed to delete type field for key: {}", key, e);
        }
    }

    /**
     * 检查文件类型字段是否存在
     */
    public boolean hasFileType(String key, FileTypeEnum fileType) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            String typeFieldName = getTypeFieldName(fileType);
            
            return redisTemplate.opsForHash().hasKey(redisKey, typeFieldName);
        } catch (Exception e) {
            log.error("❌ Failed to check type field for key: {}", key, e);
            return false;
        }
    }

    /**
     * 删除文件缓存（删除整个 Hash 键）
     */
    public void deleteFileCache(String key) {
        try {
            String redisKey = storageConfig.getRedisKeyPrefix() + key;
            redisTemplate.delete(redisKey);
            log.debug("✅ Deleted Redis cache for key: {}", redisKey);
        } catch (Exception e) {
            log.error("❌ Failed to delete Redis cache for key: {}", key, e);
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
            log.error("❌ Failed to check Redis cache existence for key: {}", key, e);
            return false;
        }
    }
}
