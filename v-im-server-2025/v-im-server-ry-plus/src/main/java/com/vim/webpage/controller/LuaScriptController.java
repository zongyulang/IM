package com.vim.webpage.controller;

import com.vim.webpage.service.LuaScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * Lua 脚本管理测试控制器
 *
 * @author vim
 */
@Slf4j
@RestController
@RequestMapping("/api/lua")
public class LuaScriptController {

    @Resource
    private LuaScriptService luaScriptService;

    /**
     * 测试限流
     * GET /api/lua/rate-limit?key=user:123&seconds=60&maxCount=10
     */
    @GetMapping("/rate-limit")
    public Map<String, Object> testRateLimit(
            @RequestParam String key,
            @RequestParam(defaultValue = "60") int seconds,
            @RequestParam(defaultValue = "10") int maxCount) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            boolean allowed = luaScriptService.checkRateLimit(key, seconds, maxCount);
            
            result.put("success", true);
            result.put("allowed", allowed);
            result.put("message", allowed ? "请求通过" : "请求被限流");
            result.put("key", key);
            result.put("seconds", seconds);
            result.put("maxCount", maxCount);
        } catch (Exception e) {
            log.error("限流测试失败", e);
            result.put("success", false);
            result.put("message", "限流测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试获取分布式锁
     * GET /api/lua/lock?key=order:123&value=uuid123&expireTime=30
     */
    @GetMapping("/lock")
    public Map<String, Object> testLock(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "30") int expireTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            boolean acquired = luaScriptService.acquireLock(key, value, expireTime);
            
            result.put("success", acquired);
            result.put("message", acquired ? "获取锁成功" : "获取锁失败");
            result.put("lockKey", key);
            result.put("lockValue", value);
            result.put("expireTime", expireTime);
        } catch (Exception e) {
            log.error("获取锁测试失败", e);
            result.put("success", false);
            result.put("message", "获取锁测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试释放分布式锁
     * POST /api/lua/unlock
     * Body: {"lockKey": "order:123", "lockValue": "uuid123"}
     */
    @PostMapping("/unlock")
    public Map<String, Object> testUnlock(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String lockKey = body.get("lockKey");
            String lockValue = body.get("lockValue");
            
            if (lockKey == null || lockValue == null) {
                result.put("success", false);
                result.put("message", "参数不完整");
                return result;
            }
            
            boolean released = luaScriptService.releaseLock(lockKey, lockValue);
            
            result.put("success", released);
            result.put("message", released ? "释放锁成功" : "释放锁失败");
        } catch (Exception e) {
            log.error("释放锁测试失败", e);
            result.put("success", false);
            result.put("message", "释放锁测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试批量获取
     * GET /api/lua/batch-get?keys=key1,key2,key3
     */
    @GetMapping("/batch-get")
    public Map<String, Object> testBatchGet(@RequestParam String keys) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> keyList = Arrays.asList(keys.split(","));
            List<Object> values = luaScriptService.batchGet(keyList);
            
            result.put("success", true);
            result.put("keys", keyList);
            result.put("values", values);
        } catch (Exception e) {
            log.error("批量获取测试失败", e);
            result.put("success", false);
            result.put("message", "批量获取测试失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取所有已加载的脚本信息
     * GET /api/lua/scripts
     */
    @GetMapping("/scripts")
    public Map<String, Object> getAllScripts() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, String> scripts = luaScriptService.getAllScripts();
            
            result.put("success", true);
            result.put("count", scripts.size());
            result.put("scripts", scripts);
        } catch (Exception e) {
            log.error("获取脚本列表失败", e);
            result.put("success", false);
            result.put("message", "获取脚本列表失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取指定脚本的 SHA
     * GET /api/lua/sha?scriptName=rate-limit.lua
     */
    @GetMapping("/sha")
    public Map<String, Object> getScriptSha(@RequestParam String scriptName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sha = luaScriptService.getScriptSha(scriptName);
            
            if (sha != null) {
                result.put("success", true);
                result.put("scriptName", scriptName);
                result.put("sha", sha);
            } else {
                result.put("success", false);
                result.put("message", "未找到脚本: " + scriptName);
            }
        } catch (Exception e) {
            log.error("获取脚本 SHA 失败", e);
            result.put("success", false);
            result.put("message", "获取脚本 SHA 失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 重新加载所有脚本
     * POST /api/lua/reload
     */
    @PostMapping("/reload")
    public Map<String, Object> reloadScripts() {
        Map<String, Object> result = new HashMap<>();
        try {
            luaScriptService.reloadAllScripts();
            
            result.put("success", true);
            result.put("message", "所有脚本已重新加载");
            result.put("scripts", luaScriptService.getAllScripts());
        } catch (Exception e) {
            log.error("重新加载脚本失败", e);
            result.put("success", false);
            result.put("message", "重新加载脚本失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 执行自定义脚本
     * POST /api/lua/execute
     * Body: {
     *   "scriptName": "my-script.lua",
     *   "keys": ["key1", "key2"],
     *   "args": ["arg1", "arg2"]
     * }
     */
    @PostMapping("/execute")
    public Map<String, Object> executeCustomScript(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String scriptName = (String) body.get("scriptName");
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) body.getOrDefault("keys", Collections.emptyList());
            @SuppressWarnings("unchecked")
            List<Object> args = (List<Object>) body.getOrDefault("args", Collections.emptyList());
            
            if (scriptName == null || scriptName.isEmpty()) {
                result.put("success", false);
                result.put("message", "脚本名称不能为空");
                return result;
            }
            
            Object scriptResult = luaScriptService.executeCustomScript(
                scriptName, 
                keys, 
                args.toArray()
            );
            
            result.put("success", true);
            result.put("scriptName", scriptName);
            result.put("result", scriptResult);
        } catch (Exception e) {
            log.error("执行自定义脚本失败", e);
            result.put("success", false);
            result.put("message", "执行自定义脚本失败: " + e.getMessage());
        }
        
        return result;
    }
}
