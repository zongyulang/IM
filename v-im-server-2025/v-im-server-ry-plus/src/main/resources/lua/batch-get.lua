-- 批量获取脚本
-- KEYS: 要获取的所有 key
-- 返回: 所有 key 对应的值列表

local result = {}
for i, key in ipairs(KEYS) do
    local value = redis.call('get', key)
    table.insert(result, value)
end
return result
