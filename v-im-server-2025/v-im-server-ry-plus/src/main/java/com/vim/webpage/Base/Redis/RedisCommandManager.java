package com.vim.webpage.Base.Redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Redis 鍛戒护绠＄悊鍣ㄥ熀绫?
 * 鐢ㄤ簬绠＄悊 Redis 鍛戒护闃熷垪
 */
public abstract class RedisCommandManager {

    private static final Logger log = LoggerFactory.getLogger(RedisCommandManager.class);

    // 鏈夋晥鐨?Redis 鏂规硶鍒楄〃
    private static final Set<String> VALID_METHODS = new HashSet<>(Arrays.asList(
            "lrange", "call", "hincrby", "expire", "exists", "get", "set",
            "hset", "del", "hmget", "hgetall", "hdel", "incr", "lpush", "script", "call"));

    // 鍛戒护闃熷垪
    protected List<RedisCommand> commands;

    public RedisCommandManager() {
        this.commands = new ArrayList<>();
    }

    /**
     * 楠岃瘉鏂规硶鏄惁鏈夋晥
     * 
     * @param method Redis 鏂规硶鍚?
     * @throws IllegalArgumentException 濡傛灉鏂规硶鏃犳晥
     */
    protected void validateMethod(String method) {
        if (!VALID_METHODS.contains(method)) {
            throw new IllegalArgumentException("Invalid method: " + method);
        }
    }

    /**
     * 娣诲姞鍛戒护鍒板懡浠ら槦鍒?
     * 
     * @param command Redis 鍛戒护瀵硅薄
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager addCommand(RedisCommand command) {
        validateMethod(command.getMethod());
        this.commands.add(command);
        return this;
    }

    public RedisCommandManager addCommand(String method, Object... args) {
        RedisCommand command = new RedisCommand(method, args);
        validateMethod(command.getMethod());
        this.commands.add(command);
        return this;
    }

    public RedisCommandManager addCommand(List<RedisCommand> command) {
        for (RedisCommand cmd : command) {
            validateMethod(cmd.getMethod());
            this.commands.add(cmd);
        }
        return this;
    }

    /**
     * 璋冪敤 Redis 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager call(Object... args) {
        this.addCommand(new RedisCommand("call", args));
        return this;
    }

    /**
     * SET 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager set(Object... args) {
        this.addCommand(new RedisCommand("set", args));
        return this;
    }

    /**
     * HSET 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager hset(Object... args) {
        this.addCommand(new RedisCommand("hset", args));
        return this;
    }

    /**
     * HGETALL 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager hgetall(Object... args) {
        this.addCommand(new RedisCommand("hgetall", args));
        return this;
    }

    /**
     * INCR 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager incr(Object... args) {
        this.addCommand(new RedisCommand("incr", args));
        return this;
    }

    /**
     * HINCRBY 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager hincrby(Object... args) {
        this.addCommand(new RedisCommand("hincrby", args));
        return this;
    }

    /**
     * LPUSH 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager lpush(Object... args) {
        this.addCommand(new RedisCommand("lpush", args));
        return this;
    }

    /**
     * EXPIRE 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager expire(Object... args) {
        this.addCommand(new RedisCommand("expire", args));
        return this;
    }

    /**
     * LRANGE 鍛戒护
     * 
     * @param args 鍛戒护鍙傛暟
     * @return 褰撳墠瀵硅薄,鏀寔閾惧紡璋冪敤
     */
    public RedisCommandManager lrange(Object... args) {
        this.addCommand(new RedisCommand("lrange", args));
        return this;
    }

    /**
     * SCRIPT 命令
     *
     * @param args 子命令及其参数，例如：("exists", sha1, sha2...)、("load", script)
     * @return 当前对象, 支持链式调用
     */
    public RedisCommandManager script(Object... args) {
        this.addCommand(new RedisCommand("script", args));
        return this;
    }

    /**
     * 鎵ц鍛戒护闃熷垪(鎶借薄鏂规硶,鐢卞瓙绫诲疄鐜?
     * 
     * @throws Exception 鎵ц寮傚父
     */
    public abstract void execute() throws Exception;

    /**
     * 娓呯┖鍛戒护闃熷垪
     */
    public void clear() {
        this.commands.clear();
    }

    /**
     * 鑾峰彇鍛戒护闃熷垪澶у皬
     * 
     * @return 鍛戒护鏁伴噺
     */
    public int getCommandCount() {
        return this.commands.size();
    }

    /**
     * Redis 鍛戒护鍐呴儴绫?
     */
    public static class RedisCommand {
        private String method;
        private Object[] args;

        public RedisCommand(String method, Object... args) {
            this.method = method;
            this.args = args;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }
    }
}
