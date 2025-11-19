package com.vim.webpage.service.User;

import com.vim.webpage.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户业务逻辑接口
 */
public interface UserService {
    
    /**
     * 创建新用户
     */
    User createUser(User user);
    
    /**
     * 根据 ID 获取用户
     */
    Optional<User> getUserById(String id);
    
    /**
     * 根据 ID 获取用户（带语言参数）
     */
    Optional<User> getUserById(String id, String lang);
    
    /**
     * 根据邮箱获取用户
     */
    Optional<User> getUserByEmail(String email);
    
    /**
     * 批量获取用户
     */
    List<User> getUsers(List<String> userIds, List<String> fields, String lang);
    
    /**
     * 获取随机用户
     */
    List<User> getRandomUsers(int count, List<String> fields, String lang);
    
    /**
     * 更新用户信息
     */
    User updateUser(String id, User user);
    
    /**
     * 验证用户邮箱
     */
    boolean verifyEmail(String email);
    
    /**
     * 增加用户浏览量
     */
    void incrementViews(String userId);
    
    /**
     * 关注用户
     */
    void followUser(String userId, String targetUserId);
    
    /**
     * 取消关注用户
     */
    void unfollowUser(String userId, String targetUserId);
}