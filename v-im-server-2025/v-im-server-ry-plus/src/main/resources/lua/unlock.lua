-- 释放分布式锁脚本
-- KEYS[1]: 锁的 key
-- ARGV[1]: 锁的值（唯一标识）
-- 返回: 1 表示释放成功, 0 表示释放失败

local key = KEYS[1]
local value = ARGV[1]

local current_value = redis.call('get', key)
if current_value == value then
    redis.call('del', key)
    return 1
else
    return 0
end
