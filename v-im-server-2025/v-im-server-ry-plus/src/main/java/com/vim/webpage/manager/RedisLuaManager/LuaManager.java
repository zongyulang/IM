package com.vim.webpage.manager.RedisLuaManager;

import com.vim.webpage.Base.Redis.RedisPipelineManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lua 脚本管理器
 * 自动扫描、加载、缓存和执行 Lua 脚本
 * 支持脚本变更检测和自动刷新
 *
 * @author vim
 */
@Slf4j
@Component
public class LuaManager {

    @jakarta.annotation.Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    @jakarta.annotation.Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 中存储脚本 SHA 的哈希表名
     */
    private static final String REDIS_SHA_KEY = "lua:scripts:sha";

    /**
     * Lua 脚本目录（相对于 resources）
     */
    private static final String LUA_SCRIPTS_PATH = "lua/**/*.lua";

    /**
     * 本地 SHA 缓存（脚本文件名 -> SHA）
     */
    private final Map<String, String> luaShaCache = new ConcurrentHashMap<>();

    /**
     * 脚本内容缓存（脚本文件名 -> 脚本内容）
     */
    private final Map<String, String> luaScriptCache = new ConcurrentHashMap<>();

    /**
     * 初始化时加载所有 Lua 脚本
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化 Lua 脚本管理器...");
        try {
            loadLuaScripts();
            log.info("Lua 脚本管理器初始化完成，共加载 {} 个脚本", luaShaCache.size());
        } catch (Exception e) {
            log.error("Lua 脚本管理器初始化失败", e);
            throw new RuntimeException("Lua 脚本管理器初始化失败", e);
        }
    }

    /**
     * 扫描并加载所有 Lua 脚本
     */
    private void loadLuaScripts() throws Exception {
        // 获取所有 Lua 文件
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + LUA_SCRIPTS_PATH);

        if (resources.length == 0) {
            log.warn("未找到任何 Lua 脚本文件");
            return;
        }

        log.info("找到 {} 个 Lua 脚本文件", resources.length);

        // 获取 Redis 中已缓存的 SHA
        Map<Object, Object> oldShaCache = stringRedisTemplate.opsForHash().entries(REDIS_SHA_KEY);

        boolean isChanged = false;
        List<String> newShas = new ArrayList<>();
        Map<String, String> newShaMap = new HashMap<>();

        // 处理每个脚本文件
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null) {
                continue;
            }

            // 读取脚本内容
            String scriptContent = StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8);

            // 计算 SHA1
            String newSha = calculateSha1(scriptContent);

            // 缓存脚本内容和 SHA
            luaScriptCache.put(fileName, scriptContent);
            luaShaCache.put(fileName, newSha);
            newShaMap.put(fileName, newSha);
            newShas.add(newSha);

            // 检查是否有变化
            String oldSha = (String) oldShaCache.get(fileName);
            if (oldSha == null || !oldSha.equals(newSha)) {
                isChanged = true;
                log.debug("检测到脚本变化: {} (旧SHA: {}, 新SHA: {})", fileName, oldSha, newSha);
            }
        }

        // 检查 Redis 中的脚本是否已加载（改用 Pipeline Manager 的 SCRIPT EXISTS）
        if (!isChanged && !newShas.isEmpty()) {
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            Object existsObj = pipelineManager.executeSingleCommand(
                    new RedisPipelineManager.RedisCommand("script", "exists", newShas.toArray()));

            if (existsObj instanceof List<?>) {
                for (Object result : (List<?>) existsObj) {
                    if ((result instanceof Boolean && Boolean.FALSE.equals(result))
                            || (result instanceof Long && (Long) result == 0)
                            || (result instanceof Integer && ((Integer) result) == 0)) {
                        isChanged = true;
                        log.debug("检测到 Redis 中脚本未加载，需要重新加载");
                        break;
                    }
                }
            }
        }

        // 如果有变化，刷新 Redis 脚本
        if (isChanged) {
            log.info("检测到脚本变化，开始刷新 Redis 脚本...");
            refreshRedisScripts(resources, newShaMap);
        } else {
            log.info("脚本无变化，跳过刷新");
        }
    }

    /**
     * 刷新 Redis 中的脚本
     */
    private void refreshRedisScripts(Resource[] resources, Map<String, String> newShaMap) throws Exception {
        // 1. 删除旧的 SHA 缓存
        stringRedisTemplate.delete(REDIS_SHA_KEY);
        log.debug("已清除旧的 SHA 缓存");

        // 2. 直接用 stringRedisTemplate 循环写入 SHA 缓存
        for (Map.Entry<String, String> entry : newShaMap.entrySet()) {
            stringRedisTemplate.opsForHash().put(REDIS_SHA_KEY, entry.getKey(), entry.getValue());
        }
        log.debug("已更新 SHA 缓存到 Redis");

    // 3. 清空所有已加载的脚本（Pipeline Manager）
    new RedisPipelineManager(redisTemplate)
        .executeSingleCommand(new RedisPipelineManager.RedisCommand("script", "flush"));
        log.debug("已清空 Redis 中的所有脚本");

        // 4. 逐个加载所有脚本（不要用 pipeline）
        int loaded = 0;
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null)
                continue;
            String scriptContent = luaScriptCache.get(fileName);
            if (scriptContent != null) {
        // 单独执行 SCRIPT LOAD（Pipeline Manager）
        new RedisPipelineManager(redisTemplate)
            .executeSingleCommand(new RedisPipelineManager.RedisCommand("script", "load", scriptContent));
                loaded++;
            }
        }
        log.info("已成功加载 {} 个脚本到 Redis", loaded);
    }

    /**
     * 计算字符串的 SHA1 值
     */
    private String calculateSha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算 SHA1 失败", e);
        }
    }

    // 删除自定义的 executeSingleCommand，统一使用 RedisPipelineManager

    /**
     * 获取脚本的 SHA
     *
     * @param scriptName 脚本名称（文件名）
     * @return SHA 值
     */
    public String getSha(String scriptName) {
        String sha = luaShaCache.get(scriptName);
        if (sha == null) {
            log.warn("未找到脚本 SHA: {}", scriptName);
        }
        return sha;
    }

    /**
     * 执行 Lua 脚本
     *
     * @param scriptName 脚本名称（文件名，如 "rate-limit.lua"）
     * @param keys       KEYS 参数列表
     * @param args       ARGV 参数列表
     * @return 执行结果
     */
    public Object executeLuaScript(String scriptName, List<String> keys, Object... args) {
        String sha = luaShaCache.get(scriptName);
        if (sha == null) {
            throw new IllegalArgumentException("未找到脚本: " + scriptName);
        }

        try {
            // 构建 EVALSHA 命令参数
            List<Object> evalArgs = new ArrayList<>();
            evalArgs.add(sha);
            evalArgs.add(keys.size());
            evalArgs.addAll(keys);
            evalArgs.addAll(Arrays.asList(args));

            // 使用 Pipeline 执行
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            Object result = pipelineManager.executeSingleCommand(
                    new RedisPipelineManager.RedisCommand("call", "EVALSHA", evalArgs.toArray()));

            log.debug("脚本 {} 执行成功", scriptName);
            return result;

        } catch (Exception e) {
            log.error("执行脚本 {} 失败", scriptName, e);

            // 如果是 NOSCRIPT 错误，尝试重新加载脚本
            if (e.getMessage() != null && e.getMessage().contains("NOSCRIPT")) {
                log.info("检测到 NOSCRIPT 错误，尝试重新加载脚本...");
                try {
                    reloadScript(scriptName);
                    return executeLuaScript(scriptName, keys, args);
                } catch (Exception reloadEx) {
                    log.error("重新加载脚本失败", reloadEx);
                }
            }

            throw new RuntimeException("执行 Lua 脚本失败: " + scriptName, e);
        }
    }

    /**
     * 重新加载单个脚本
     */
    private void reloadScript(String scriptName) throws Exception {
        String scriptContent = luaScriptCache.get(scriptName);
        if (scriptContent == null) {
            throw new IllegalArgumentException("未找到脚本内容: " + scriptName);
        }

    // 加载脚本到 Redis（Pipeline Manager）
    Object loadResult = new RedisPipelineManager(redisTemplate)
        .executeSingleCommand(new RedisPipelineManager.RedisCommand("script", "load", scriptContent));

    if (loadResult != null) {
        String newSha = (loadResult instanceof byte[])
            ? new String((byte[]) loadResult, StandardCharsets.UTF_8)
            : loadResult.toString();
            luaShaCache.put(scriptName, newSha);

            // 更新 Redis 中的 SHA 缓存
            stringRedisTemplate.opsForHash().put(REDIS_SHA_KEY, scriptName, newSha);

            log.info("成功重新加载脚本: {} (SHA: {})", scriptName, newSha);
        }
    }

    /**
     * 手动重新加载所有脚本
     */
    public void reloadAllScripts() {
        log.info("开始手动重新加载所有脚本...");
        try {
            loadLuaScripts();
            log.info("所有脚本重新加载完成");
        } catch (Exception e) {
            log.error("重新加载脚本失败", e);
            throw new RuntimeException("重新加载脚本失败", e);
        }
    }

    /**
     * 获取所有已加载的脚本信息
     */
    public Map<String, String> getAllScripts() {
        return new HashMap<>(luaShaCache);
    }

    /**
     * 清除所有脚本缓存
     */
    public void clearCache() {
        luaShaCache.clear();
        luaScriptCache.clear();
        stringRedisTemplate.delete(REDIS_SHA_KEY);
        log.info("已清除所有脚本缓存");
    }
}
