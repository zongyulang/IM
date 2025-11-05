package com.vim.webpage.Base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author fresleo
 * @description Redis pipeline 管理器
 * @Data 2025/11/6
 */
public class RedisPipelineManager extends RedisCommandManager {

    private static final Logger log = LoggerFactory.getLogger(RedisPipelineManager.class);

    private final RedisTemplate<String, Object> redisTemplate;

    // 存储每个命令对应的 CompletableFuture
    private List<CompletableFuture<Object>> futures;

    /**
     * 构造函数，需要传入 RedisTemplate 实例
     * 
     * @param redisTemplate Redis 模板实例
     */
    public RedisPipelineManager(RedisTemplate<String, Object> redisTemplate) {
        super();
        this.redisTemplate = redisTemplate;
        this.futures = new ArrayList<>();
    }

    /**
     * 批量执行命令
     * 
     * @param commandList 命令列表
     * @return 所有命令的执行结果
     * @throws Exception 执行异常
     */
    public List<Object> executeBatchCommands(List<RedisCommand> commandList) throws Exception {
        List<CompletableFuture<Object>> promises = new ArrayList<>();

        for (RedisCommand command : commandList) {
            String method = command.getMethod();
            // 验证方法是否有效
            validateMethod(method);

            CompletableFuture<Object> future = new CompletableFuture<>();
            this.commands.add(command);
            this.futures.add(future);
            promises.add(future);
        }

        // 执行 pipeline
        flushPipeline();

        // 等待所有命令执行完成并返回结果
        List<Object> results = new ArrayList<>();
        for (CompletableFuture<Object> future : promises) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error getting future result", e);
                throw new Exception("Error executing batch commands", e);
            }
        }

        return results;
    }

    /**
     * 执行单个命令
     * 
     * @param command Redis 命令
     * @return 命令执行结果
     * @throws Exception 执行异常
     */
    public Object executeSingleCommand(RedisCommand command) throws Exception {
        String method = command.getMethod();
        validateMethod(method);

        CompletableFuture<Object> future = new CompletableFuture<>();
        this.commands.add(command);
        this.futures.add(future);

        flushPipeline();

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error executing single command", e);
            throw new Exception("Error executing single command", e);
        }
    }

    /**
     * 执行 pipeline 中的所有命令
     * 
     * @throws Exception 执行异常
     */
    @Override
    public void execute() throws Exception {
        if (commands.isEmpty()) {
            return;
        }

        try {
            // 使用 RedisTemplate 的 executePipelined 方法执行 pipeline
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (RedisCommand command : commands) {
                    String method = command.getMethod();
                    Object[] args = command.getArgs();

                    try {
                        // 根据不同的命令类型执行相应的操作
                        executeRedisCommand((RedisConnection) connection, method, args);
                    } catch (Exception e) {
                        log.error("Error executing command: " + method, e);
                    }
                }
                return null;
            });

            // 检查结果数量是否匹配
            if (results.size() != commands.size()) {
                log.error("Mismatch between number of commands ({}) and results ({})",
                        commands.size(), results.size());
                throw new Exception("Pipeline results do not match the number of commands sent.");
            }

            // 将结果设置到对应的 CompletableFuture 中
            for (int i = 0; i < results.size(); i++) {
                Object result = results.get(i);
                CompletableFuture<Object> future = futures.get(i);

                if (result instanceof Exception) {
                    future.completeExceptionally((Exception) result);
                } else {
                    future.complete(result);
                }
            }

        } catch (Exception e) {
            log.error("Error executing pipeline", e);
            // 如果执行失败,将所有 future 设置为异常状态
            for (CompletableFuture<Object> future : futures) {
                if (!future.isDone()) {
                    future.completeExceptionally(e);
                }
            }
            throw e;
        } finally {
            // 清理资源
            clear();
            futures.clear();
        }
    }

    /**
     * 执行 Redis 命令
     * 
     * @param conn   Redis 连接
     * @param method 命令方法名
     * @param args   命令参数
     */
    private void executeRedisCommand(RedisConnection conn, String method, Object[] args) {
        RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();

        try {
            switch (method.toLowerCase()) {
                case "set":
                    if (args.length >= 2) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[] value = serialize(args[1]);
                        conn.set(key, value);
                    }
                    break;
                case "get":
                    if (args.length >= 1) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        conn.get(key);
                    }
                    break;
                case "hset":
                    if (args.length >= 3) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[] field = serialize(args[1]);
                        byte[] value = serialize(args[2]);
                        conn.hSet(key, field, value);
                    }
                    break;
                case "hgetall":
                    if (args.length >= 1) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        conn.hGetAll(key);
                    }
                    break;
                case "incr":
                    if (args.length >= 1) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        conn.incr(key);
                    }
                    break;
                case "hincrby":
                    if (args.length >= 3) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[] field = serialize(args[1]);
                        long increment = Long.parseLong(String.valueOf(args[2]));
                        conn.hIncrBy(key, field, increment);
                    }
                    break;
                case "lpush":
                    if (args.length >= 2) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[][] values = new byte[args.length - 1][];
                        for (int i = 1; i < args.length; i++) {
                            values[i - 1] = serialize(args[i]);
                        }
                        conn.lPush(key, values);
                    }
                    break;
                case "lrange":
                    if (args.length >= 3) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        long start = Long.parseLong(String.valueOf(args[1]));
                        long end = Long.parseLong(String.valueOf(args[2]));
                        conn.lRange(key, start, end);
                    }
                    break;
                case "expire":
                    if (args.length >= 2) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        long seconds = Long.parseLong(String.valueOf(args[1]));
                        conn.expire(key, seconds);
                    }
                    break;
                case "del":
                    if (args.length >= 1) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        conn.del(key);
                    }
                    break;
                case "exists":
                    if (args.length >= 1) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        conn.exists(key);
                    }
                    break;
                case "hdel":
                    if (args.length >= 2) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[][] fields = new byte[args.length - 1][];
                        for (int i = 1; i < args.length; i++) {
                            fields[i - 1] = serialize(args[i]);
                        }
                        conn.hDel(key, fields);
                    }
                    break;
                case "hmget":
                    if (args.length >= 2) {
                        byte[] key = stringSerializer.serialize(String.valueOf(args[0]));
                        byte[][] fields = new byte[args.length - 1][];
                        for (int i = 1; i < args.length; i++) {
                            fields[i - 1] = serialize(args[i]);
                        }
                        conn.hMGet(key, fields);
                    }
                    break;
                default:
                    log.warn("Unsupported Redis command: {}", method);
            }
        } catch (Exception e) {
            log.error("Error executing Redis command: " + method, e);
        }
    }

    /**
     * 序列化对象为字节数组
     */
    private byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }
        return String.valueOf(obj).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 刷新 pipeline,执行所有待处理的命令
     * 
     * @throws Exception 执行异常
     */
    public void flushPipeline() throws Exception {
        if (!commands.isEmpty()) {
            execute();
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.futures.clear();
    }
}
