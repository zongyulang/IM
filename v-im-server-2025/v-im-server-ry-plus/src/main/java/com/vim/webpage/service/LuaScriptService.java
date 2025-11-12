package com.vim.webpage.service;

import com.vim.webpage.manager.RedisLuaManager.LuaManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * Lua 脚本服务
 * 提供常用的 Lua 脚本业务方法
 *
 * @author fresleo
 */
@Slf4j
@Service
public class LuaScriptService {

    @Resource
    private LuaManager luaManager;

    /**
     * 执行限流脚本
     * 需要创建 lua/rate-limit.lua 文件
     *
     * @param key       限流的 key
     * @param seconds   时间窗口（秒）
     * @param maxCount  最大次数
     * @return true 允许访问, false 被限流
     */
    public boolean checkRateLimit(String key, int seconds, int maxCount) {
        try {
            Object result = luaManager.executeLuaScript(
                "rate-limit.lua",
                Collections.singletonList(key),
                seconds,
                maxCount
            );
            
            if (result instanceof Long) {
                return (Long) result == 1;
            } else if (result instanceof Integer) {
                return (Integer) result == 1;
            }
            
            return false;
        } catch (Exception e) {
            log.error("限流检查失败: key={}", key, e);
            // 发生异常时，为安全起见返回 false（拒绝访问）
            return false;
        }
    }

    /**
     * 获取分布式锁
     * 需要创建 lua/distributed-lock.lua 文件
     *
     * @param lockKey    锁的 key
     * @param lockValue  锁的值（唯一标识）
     * @param expireTime 过期时间（秒）
     * @return true 获取成功, false 获取失败
     */
    public boolean acquireLock(String lockKey, String lockValue, int expireTime) {
        try {
            Object result = luaManager.executeLuaScript(
                "distributed-lock.lua",
                Collections.singletonList(lockKey),
                lockValue,
                expireTime
            );
            
            if (result instanceof Long) {
                return (Long) result == 1;
            } else if (result instanceof Integer) {
                return (Integer) result == 1;
            }
            
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁失败: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * 需要创建 lua/unlock.lua 文件
     *
     * @param lockKey   锁的 key
     * @param lockValue 锁的值（唯一标识）
     * @return true 释放成功, false 释放失败
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            Object result = luaManager.executeLuaScript(
                "unlock.lua",
                Collections.singletonList(lockKey),
                lockValue
            );
            
            if (result instanceof Long) {
                return (Long) result == 1;
            } else if (result instanceof Integer) {
                return (Integer) result == 1;
            }
            
            return false;
        } catch (Exception e) {
            log.error("释放分布式锁失败: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 批量获取多个 key 的值
     * 需要创建 lua/batch-get.lua 文件
     *
     * @param keys 要获取的 key 列表
     * @return 值列表
     */
    @SuppressWarnings("unchecked")
    public List<Object> batchGet(List<String> keys) {
        try {
            Object result = luaManager.executeLuaScript(
                "batch-get.lua",
                keys
            );
            
            if (result instanceof List) {
                return (List<Object>) result;
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("批量获取失败: keys={}", keys, e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行自定义 Lua 脚本
     *
     * @param scriptName 脚本名称（文件名）
     * @param keys       KEYS 参数
     * @param args       ARGV 参数
     * @return 执行结果
     */
    public Object executeCustomScript(String scriptName, List<String> keys, Object... args) {
        try {
            return luaManager.executeLuaScript(scriptName, keys, args);
        } catch (Exception e) {
            log.error("执行自定义脚本失败: scriptName={}", scriptName, e);
            throw new RuntimeException("执行自定义脚本失败: " + scriptName, e);
        }
    }

    /**
     * 获取脚本的 SHA 值
     *
     * @param scriptName 脚本名称
     * @return SHA 值
     */
    public String getScriptSha(String scriptName) {
        return luaManager.getSha(scriptName);
    }

    /**
     * 获取所有已加载的脚本信息
     *
     * @return 脚本名称 -> SHA 的映射
     */
    public Map<String, String> getAllScripts() {
        return luaManager.getAllScripts();
    }

    /**
     * 重新加载所有脚本
     */
    public void reloadAllScripts() {
        luaManager.reloadAllScripts();
    }
}
