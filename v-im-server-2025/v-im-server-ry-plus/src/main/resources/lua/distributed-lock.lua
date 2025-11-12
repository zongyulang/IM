-- 分布式锁脚本
-- KEYS[1]: 锁的 key
-- ARGV[1]: 锁的值（唯一标识）
-- ARGV[2]: 过期时间（秒）
-- 返回: 1 表示获取锁成功, 0 表示获取锁失败

local key = KEYS[1]
local value = ARGV[1]
local expire_time = tonumber(ARGV[2])

local result = redis.call('set', key, value, 'NX', 'EX', expire_time)
if result then
    return 1
else
    return 0
end
