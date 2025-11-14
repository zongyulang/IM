-- AvatarZaddPut.lua
-- 功能：将头像路径添加到 sorted set，并返回超过7天的旧文件路径
-- KEYS[1]: sorted set 的键名（如：avatar_access_log）
-- ARGV[1]: 文件路径
-- ARGV[2]: 当前时间戳
-- ARGV[3]: 过期时间（秒，默认7天 = 604800秒）

local set_key = KEYS[1]
local file_path = ARGV[1]
local current_time = tonumber(ARGV[2])
local expire_seconds = tonumber(ARGV[3]) or 604800  -- 7天

-- 添加当前文件路径到 sorted set
redis.call('ZADD', set_key, current_time, file_path)

-- 计算7天前的时间戳
local expire_time = current_time - expire_seconds

-- 获取超过7天的文件路径
local expired_files = redis.call('ZRANGEBYSCORE', set_key, '-inf', expire_time)

-- 从 sorted set 中删除过期的文件
if #expired_files > 0 then
    redis.call('ZREMRANGEBYSCORE', set_key, '-inf', expire_time)
end

-- 返回过期的文件路径列表
return expired_files
