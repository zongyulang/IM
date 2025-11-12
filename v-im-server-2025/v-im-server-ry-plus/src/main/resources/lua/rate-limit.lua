-- 限流脚本
-- KEYS[1]: 限流的 key
-- ARGV[1]: 限流窗口时间（秒）
-- ARGV[2]: 限流次数
-- 返回: 1 表示允许访问, 0 表示被限流

local key = KEYS[1]
local limit = tonumber(ARGV[2])
local expire_time = tonumber(ARGV[1])

local current = redis.call('get', key)
if current and tonumber(current) >= limit then
    return 0
end

redis.call('incr', key)
if tonumber(redis.call('ttl', key)) == -1 then
    redis.call('expire', key, expire_time)
end

return 1
