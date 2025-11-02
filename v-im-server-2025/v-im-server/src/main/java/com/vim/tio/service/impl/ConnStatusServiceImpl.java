package com.vim.tio.service.impl;

import com.vim.tio.service.ConnStatusService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户是否是新连接，以便于清理未读的消息
 */
@Service
public class ConnStatusServiceImpl implements ConnStatusService {

    //是否是新连接的用户
    public static final String IS_NEW_CONN = "vim:conn:isNew:";

    /**
     * Redis 模板，用于缓存操作。
     */
    @Resource
    protected RedisTemplate<String, Boolean> redisTemplate;

    @Override
    public void setConnStatus(String userId, boolean status) {
        // 标记是否是新连接
        redisTemplate.opsForValue().set(IS_NEW_CONN + userId, status);
    }

    @Override
    public boolean getConnStatus(String userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(IS_NEW_CONN + userId));
    }
}
