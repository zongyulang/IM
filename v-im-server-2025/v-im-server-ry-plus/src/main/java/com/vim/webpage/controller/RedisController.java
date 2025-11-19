package com.vim.webpage.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import com.vim.webpage.manager.Redis.RedisCommandManager;
import com.vim.webpage.manager.Redis.RedisPipelineManager;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Redis操作示例Controller
 * 演示如何在Controller中使用Redis
 * 使用 DB0 数据库(webpage业务专用)
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    // 注入 webpageStringRedisTemplate,专门操作 Redis DB0
    @Resource(name = "webpageStringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    // 注入 RedisTemplate,用于创建 Pipeline 管理器
    @Resource(name = "webpageRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置Redis值
     * 示例: GET http://localhost:8080/api/redis/set?key=name&value=张三
     */
    @GetMapping("/set")
    public Map<String, Object> set(@RequestParam String key, @RequestParam String value) {
        stringRedisTemplate.opsForValue().set(key, value);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "设置成功");
        result.put("key", key);
        result.put("value", value);
        return result;
    }

    /**
     * 获取Redis值
     * 示例: GET http://localhost:8080/api/redis/get?key=name
     */
    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam String key) {
        String value = stringRedisTemplate.opsForValue().get(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("exists", value != null);
        return result;
    }

    /**
     * 设置带过期时间的值(秒)
     * 示例: GET
     * http://localhost:8080/api/redis/setex?key=code&value=123456&seconds=60
     */
    @GetMapping("/setex")
    public Map<String, Object> setWithExpire(@RequestParam String key,
            @RequestParam String value,
            @RequestParam long seconds) {
        stringRedisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "设置成功,将在" + seconds + "秒后过期");
        result.put("key", key);
        result.put("value", value);
        result.put("expireSeconds", seconds);
        return result;
    }

    /**
     * 删除Redis键
     * 示例: DELETE http://localhost:8080/api/redis/delete?key=name
     */
    @DeleteMapping("/delete")
    public Map<String, Object> delete(@RequestParam String key) {
        Boolean deleted = stringRedisTemplate.delete(key);

        Map<String, Object> result = new HashMap<>();
        result.put("success", deleted != null && deleted);
        result.put("message", deleted != null && deleted ? "删除成功" : "删除失败或key不存在");
        result.put("key", key);
        return result;
    }

    /**
     * 检查key是否存在
     * 示例: GET http://localhost:8080/api/redis/exists?key=name
     */
    @GetMapping("/exists")
    public Map<String, Object> exists(@RequestParam String key) {
        Boolean exists = stringRedisTemplate.hasKey(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("exists", exists != null && exists);
        return result;
    }

    /**
     * 获取key的剩余过期时间(秒)
     * 示例: GET http://localhost:8080/api/redis/ttl?key=code
     * 返回值: -1表示永久有效, -2表示key不存在, 其他正数表示剩余秒数
     */
    @GetMapping("/ttl")
    public Map<String, Object> getTtl(@RequestParam String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("ttl", ttl);
        result.put("message", ttl == -1 ? "永久有效" : ttl == -2 ? "key不存在" : ttl + "秒后过期");
        return result;
    }

    /**
     * 自增操作
     * 示例: GET http://localhost:8080/api/redis/incr?key=counter
     */
    @GetMapping("/incr")
    public Map<String, Object> increment(@RequestParam String key) {
        Long newValue = stringRedisTemplate.opsForValue().increment(key);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("key", key);
        result.put("value", newValue);
        result.put("message", "自增成功");
        return result;
    }

    /**
     * 自减操作
     * 示例: GET http://localhost:8080/api/redis/decr?key=counter
     */
    @GetMapping("/decr")
    public Map<String, Object> decrement(@RequestParam String key) {
        Long newValue = stringRedisTemplate.opsForValue().decrement(key);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("key", key);
        result.put("value", newValue);
        result.put("message", "自减成功");
        return result;
    }

    // ==================== Hash 操作 ====================

    /**
     * Hash设置值
     * 示例: GET http://localhost:8080/api/redis/hset?key=user:1&field=name&value=张三
     */
    @GetMapping("/hset")
    public Map<String, Object> hset(@RequestParam String key,
            @RequestParam String field,
            @RequestParam String value) {
        stringRedisTemplate.opsForHash().put(key, field, value);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Hash设置成功");
        result.put("key", key);
        result.put("field", field);
        result.put("value", value);
        return result;
    }

    /**
     * Hash获取值
     * 示例: GET http://localhost:8080/api/redis/hget?key=user:1&field=name
     */
    @GetMapping("/hget")
    public Map<String, Object> hget(@RequestParam String key, @RequestParam String field) {
        Object value = stringRedisTemplate.opsForHash().get(key, field);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("field", field);
        result.put("value", value);
        return result;
    }

    /**
     * 获取Hash的所有字段
     * 示例: GET http://localhost:8080/api/redis/hgetall?key=user:1
     */
    @GetMapping("/hgetall")
    public Map<String, Object> hgetAll(@RequestParam String key) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("data", entries);
        result.put("size", entries.size());
        return result;
    }

    // ==================== List 操作 ====================

    /**
     * 从列表左侧推入
     * 示例: GET http://localhost:8080/api/redis/lpush?key=list:msg&value=消息1
     */
    @GetMapping("/lpush")
    public Map<String, Object> lpush(@RequestParam String key, @RequestParam String value) {
        Long size = stringRedisTemplate.opsForList().leftPush(key, value);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "推入成功");
        result.put("key", key);
        result.put("value", value);
        result.put("listSize", size);
        return result;
    }

    /**
     * 从列表右侧推入
     * 示例: GET http://localhost:8080/api/redis/rpush?key=list:msg&value=消息2
     */
    @GetMapping("/rpush")
    public Map<String, Object> rpush(@RequestParam String key, @RequestParam String value) {
        Long size = stringRedisTemplate.opsForList().rightPush(key, value);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "推入成功");
        result.put("key", key);
        result.put("value", value);
        result.put("listSize", size);
        return result;
    }

    /**
     * 获取列表范围内的元素
     * 示例: GET http://localhost:8080/api/redis/lrange?key=list:msg&start=0&end=-1
     * 说明: end=-1 表示获取到列表末尾
     */
    @GetMapping("/lrange")
    public Map<String, Object> lrange(@RequestParam String key,
            @RequestParam(defaultValue = "0") long start,
            @RequestParam(defaultValue = "-1") long end) {
        var list = stringRedisTemplate.opsForList().range(key, start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("data", list);
        result.put("size", list != null ? list.size() : 0);
        return result;
    }

    // ==================== Pipeline 批量操作 ====================

    /**
     * 批量执行Redis命令 - Pipeline方式
     * 示例: POST http://localhost:8080/api/redis/pipeline/batch
     * 
     * 请求体示例:
     * {
     * "commands": [
     * {"method": "set", "args": ["key1", "value1"]},
     * {"method": "set", "args": ["key2", "value2"]},
     * {"method": "hset", "args": ["user:1", "name", "张三"]},
     * {"method": "expire", "args": ["key1", 60]}
     * ]
     * }
     */
    @PostMapping("/pipeline/batch")
    public Map<String, Object> executeBatchPipeline(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commandMaps = (List<Map<String, Object>>) requestBody.get("commands");

            if (commandMaps == null || commandMaps.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "命令列表不能为空");
                return errorResult;
            }

            // 转换为 RedisCommand 对象列表
            List<RedisCommandManager.RedisCommand> commands = new ArrayList<>();
            for (Map<String, Object> cmdMap : commandMaps) {
                String method = (String) cmdMap.get("method");
                @SuppressWarnings("unchecked")
                List<Object> argsList = (List<Object>) cmdMap.get("args");
                Object[] args = argsList != null ? argsList.toArray() : new Object[0];

                commands.add(new RedisCommandManager.RedisCommand(method, args));
            }

            // 执行批量命令 - 每次创建新的 Pipeline 管理器实例（线程安全）
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            long startTime = System.currentTimeMillis();
            List<Object> results = pipelineManager.executeBatchCommands(commands);
            long endTime = System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量执行成功");
            result.put("commandCount", commands.size());
            result.put("results", results);
            result.put("executionTime", (endTime - startTime) + "ms");
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "批量执行失败: " + e.getMessage());
            errorResult.put("error", e.getClass().getName());
            return errorResult;
        }
    }

    /**
     * Pipeline批量设置键值对
     * 示例: POST http://localhost:8080/api/redis/pipeline/mset
     * 
     * 请求体示例:
     * {
     * "data": {
     * "key1": "value1",
     * "key2": "value2",
     * "key3": "value3"
     * }
     * }
     */
    @PostMapping("/pipeline/mset")
    public Map<String, Object> pipelineMultiSet(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) requestBody.get("data");

            if (data == null || data.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "数据不能为空");
                return errorResult;
            }

            // 构建命令列表
            List<RedisCommandManager.RedisCommand> commands = new ArrayList<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                commands.add(new RedisCommandManager.RedisCommand("set", entry.getKey(), entry.getValue()));
            }

            // 执行批量设置 - 每次创建新的 Pipeline 管理器实例（线程安全）
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            long startTime = System.currentTimeMillis();
            List<Object> results = pipelineManager.executeBatchCommands(commands);
            long endTime = System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量设置成功");
            result.put("setCount", data.size());
            result.put("executionTime", (endTime - startTime) + "ms");
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "批量设置失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Pipeline批量获取值
     * 示例: POST http://localhost:8080/api/redis/pipeline/mget
     * 
     * 请求体示例:
     * {
     * "keys": ["key1", "key2", "key3"]
     * }
     */
    @PostMapping("/pipeline/mget")
    public Map<String, Object> pipelineMultiGet(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) requestBody.get("keys");

            if (keys == null || keys.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "键列表不能为空");
                return errorResult;
            }

            // 构建命令列表
            List<RedisCommandManager.RedisCommand> commands = new ArrayList<>();
            for (String key : keys) {
                commands.add(new RedisCommandManager.RedisCommand("get", key));
            }

            // 执行批量获取 - 每次创建新的 Pipeline 管理器实例（线程安全）
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            long startTime = System.currentTimeMillis();
            List<Object> results = pipelineManager.executeBatchCommands(commands);
            long endTime = System.currentTimeMillis();

            // 组装结果
            Map<String, Object> dataMap = new HashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                dataMap.put(keys.get(i), results.get(i));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量获取成功");
            result.put("data", dataMap);
            result.put("executionTime", (endTime - startTime) + "ms");
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "批量获取失败: " + e.getMessage());
            return errorResult;
        }
    }

    @GetMapping("/xadd")
    public Map<String, Object> postMethodName(@RequestParam String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接使用 PipelineManager 执行单条命令
            // XADD 命令格式: XADD key * field value [field value ...]
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            Object response = pipelineManager.executeSingleCommand(
                    new RedisPipelineManager.RedisCommand("call", "xadd", key, "*", "name", "mingzi"));

            result.put("success", true);
            result.put("message", "XADD 执行完成");
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "XADD 执行失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
        }
        return result;
    }

    /**
     * Pipeline批量设置Hash
     * 示例: POST http://localhost:8080/api/redis/pipeline/hmset
     * 
     * 请求体示例:
     * {
     * "key": "user:1",
     * "fields": {
     * "name": "张三",
     * "age": "25",
     * "city": "北京"
     * }
     * }
     */
    @PostMapping("/pipeline/hmset")
    public Map<String, Object> pipelineHashMultiSet(@RequestBody Map<String, Object> requestBody) {
        try {
            String key = (String) requestBody.get("key");
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) requestBody.get("fields");

            if (key == null || key.isEmpty() || fields == null || fields.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "key和fields不能为空");
                return errorResult;
            }

            // 构建命令列表
            List<RedisCommandManager.RedisCommand> commands = new ArrayList<>();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                commands.add(new RedisCommandManager.RedisCommand("hset", key, entry.getKey(), entry.getValue()));
            }

            // 执行批量设置 - 每次创建新的 Pipeline 管理器实例（线程安全）
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            long startTime = System.currentTimeMillis();
            List<Object> results = pipelineManager.executeBatchCommands(commands);
            long endTime = System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Hash批量设置成功");
            result.put("key", key);
            result.put("fieldCount", fields.size());
            result.put("executionTime", (endTime - startTime) + "ms");
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Hash批量设置失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 测试Pipeline性能对比
     * 示例: GET http://localhost:8080/api/redis/pipeline/performance-test?count=100
     * 
     * 对比普通方式和Pipeline方式执行相同命令的性能差异
     */
    @GetMapping("/pipeline/performance-test")
    public Map<String, Object> performanceTest(@RequestParam(defaultValue = "100") int count) {
        try {
            // 1. 普通方式执行
            long normalStartTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                stringRedisTemplate.opsForValue().set("test:normal:" + i, "value" + i);
            }
            long normalEndTime = System.currentTimeMillis();
            long normalTime = normalEndTime - normalStartTime;

            // 2. Pipeline方式执行
            List<RedisCommandManager.RedisCommand> commands = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                commands.add(new RedisCommandManager.RedisCommand("set", "test:pipeline:" + i, "value" + i));
            }

            // 每次创建新的 Pipeline 管理器实例（线程安全）
            RedisPipelineManager pipelineManager = new RedisPipelineManager(redisTemplate);
            long pipelineStartTime = System.currentTimeMillis();
            pipelineManager.executeBatchCommands(commands);
            long pipelineEndTime = System.currentTimeMillis();
            long pipelineTime = pipelineEndTime - pipelineStartTime;

            // 计算性能提升
            double improvement = normalTime > 0 ? ((double) (normalTime - pipelineTime) / normalTime * 100) : 0;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("commandCount", count);
            result.put("normalExecutionTime", normalTime + "ms");
            result.put("pipelineExecutionTime", pipelineTime + "ms");
            result.put("performanceImprovement", String.format("%.2f%%", improvement));
            result.put("speedup", normalTime > 0 ? String.format("%.2fx", (double) normalTime / pipelineTime) : "N/A");
            result.put("message", "Pipeline方式比普通方式快 " + String.format("%.2f%%", improvement));
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "性能测试失败: " + e.getMessage());
            return errorResult;
        }
    }
}
