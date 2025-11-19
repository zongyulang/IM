package com.vim.webpage.service.User.impl;

import com.vim.webpage.service.User.UserService;
import com.vim.webpage.domain.User;
import com.vim.webpage.Utils.CDN_decrypt;
import com.vim.webpage.Utils.UserDataPackageUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CDN_decrypt cdnDecrypt;

    private static final String COLLECTION_NAME = "users";

    /**
     * 创建新用户
     */
    @Override
    public User createUser(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("用户对象不能为空");
            }

            // 设置创建时间和更新时间
            Date now = new Date();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            // 保存到 MongoDB
            User savedUser = mongoTemplate.save(user, COLLECTION_NAME);
            log.info("✅ 成功创建用户: {}", savedUser.getId());

            // 返回安全的用户数据（移除敏感信息）
            return getSafeUser(savedUser);
        } catch (Exception e) {
            log.error("❌ 创建用户失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建用户失败", e);
        }
    }

    /**
     * 根据 ID 获取用户
     */
    @Override
    public Optional<User> getUserById(String id) {
        return getUserById(id, null);
    }

    /**
     * 根据 ID 获取用户（带语言参数）
     */
    @Override
    public Optional<User> getUserById(String id, String lang) {
        try {
            if (!StringUtils.hasText(id)) {
                return Optional.empty();
            }

            // 从 MongoDB 直接查询
            User user = mongoTemplate.findById(id, User.class, COLLECTION_NAME);
            
            if (user == null) {
                return Optional.empty();
            }

            // 处理头像 URL
            processAvatarUrl(user);

            // 获取安全的用户数据
            User safeUser = getSafeUser(user);

            // 应用多语言处理
            if (StringUtils.hasText(lang)) {
                safeUser = UserDataPackageUtil.packageUserDataFromFields(safeUser, lang);
            }

            return Optional.of(safeUser);
        } catch (Exception e) {
            log.error("❌ 根据ID获取用户失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 根据邮箱获取用户
     */
    @Override
    public Optional<User> getUserByEmail(String email) {
        try {
            if (!StringUtils.hasText(email)) {
                return Optional.empty();
            }

            Query query = new Query(Criteria.where("email").is(email));
            User user = mongoTemplate.findOne(query, User.class, COLLECTION_NAME);

            if (user == null) {
                return Optional.empty();
            }

            // 处理头像 URL
            processAvatarUrl(user);

            return Optional.of(getSafeUser(user));
        } catch (Exception e) {
            log.error("❌ 根据邮箱获取用户失败, Email: {}, 错误: {}", email, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 批量获取用户
     */
    @Override
    public List<User> getUsers(List<String> userIds, List<String> fields, String lang) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 构建查询条件
            Query query = new Query(Criteria.where("_id").in(userIds));

            // 如果指定了字段，添加投影
            if (fields != null && !fields.isEmpty()) {
                for (String field : fields) {
                    query.fields().include(field);
                }
            }

            // 从 MongoDB 查询
            List<User> users = mongoTemplate.find(query, User.class, COLLECTION_NAME);

            // 处理用户数据
            return users.stream()
                    .map(user -> {
                        processAvatarUrl(user);
                        User safeUser = getSafeUser(user);
                        // 应用多语言处理
                        return UserDataPackageUtil.packageUserDataFromFields(safeUser, lang);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ 批量获取用户失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取随机用户
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<User> getRandomUsers(int count, List<String> fields, String lang) {
        try {
            // 使用 MongoDB 聚合管道随机采样
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.sample(count),
                    Aggregation.project("_id")
            );

            AggregationResults<Map<String, Object>> results = mongoTemplate.aggregate(
                    aggregation, 
                    COLLECTION_NAME, 
                    (Class<Map<String, Object>>)(Class<?>)Map.class
            );

            List<Map<String, Object>> mappedResults = results.getMappedResults();
            if (mappedResults.isEmpty()) {
                return new ArrayList<>();
            }

            // 提取用户 ID
            List<String> userIds = mappedResults.stream()
                    .map(map -> map.get("_id").toString())
                    .collect(Collectors.toList());

            // 使用 getUsers 方法获取完整用户数据
            return getUsers(userIds, fields, lang);

        } catch (Exception e) {
            log.error("❌ 获取随机用户失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新用户信息
     */
    @Override
    public User updateUser(String id, User user) {
        try {
            if (!StringUtils.hasText(id) || user == null) {
                throw new IllegalArgumentException("用户ID和用户对象不能为空");
            }

            // 检查用户是否存在
            User existingUser = mongoTemplate.findById(id, User.class, COLLECTION_NAME);
            if (existingUser == null) {
                throw new RuntimeException("用户不存在: " + id);
            }

            // 更新时间
            user.setId(id);
            user.setUpdatedAt(new Date());

            // 保存更新
            User updatedUser = mongoTemplate.save(user, COLLECTION_NAME);
            log.info("✅ 成功更新用户: {}", id);

            return getSafeUser(updatedUser);
        } catch (Exception e) {
            log.error("❌ 更新用户失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("更新用户失败", e);
        }
    }

    /**
     * 验证用户邮箱
     */
    @Override
    public boolean verifyEmail(String email) {
        try {
            if (!StringUtils.hasText(email)) {
                return false;
            }

            Query query = new Query(Criteria.where("email").is(email));
            return mongoTemplate.exists(query, User.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 验证邮箱失败, Email: {}, 错误: {}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 增加用户浏览量
     */
    @Override
    public void incrementViews(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                throw new IllegalArgumentException("用户ID不能为空");
            }

            User user = mongoTemplate.findById(userId, User.class, COLLECTION_NAME);
            if (user != null) {
                user.setViews(user.getViews() != null ? user.getViews() + 1 : 1);
                user.setTotalViews(user.getTotalViews() != null ? user.getTotalViews() + 1 : 1);
                user.setUpdatedAt(new Date());
                mongoTemplate.save(user, COLLECTION_NAME);
                log.info("✅ 用户浏览量已增加: {}", userId);
            }
        } catch (Exception e) {
            log.error("❌ 增加浏览量失败, userId: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 关注用户
     */
    @Override
    public void followUser(String userId, String targetUserId) {
        try {
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(targetUserId)) {
                throw new IllegalArgumentException("用户ID不能为空");
            }

            // 更新关注者的关注数
            User follower = mongoTemplate.findById(userId, User.class, COLLECTION_NAME);
            if (follower != null) {
                follower.setFollowingCount(
                    follower.getFollowingCount() != null ? follower.getFollowingCount() + 1 : 1
                );
                follower.setUpdatedAt(new Date());
                mongoTemplate.save(follower, COLLECTION_NAME);
            }

            // 更新被关注者的粉丝数
            User target = mongoTemplate.findById(targetUserId, User.class, COLLECTION_NAME);
            if (target != null) {
                target.setFollowerCount(
                    target.getFollowerCount() != null ? target.getFollowerCount() + 1 : 1
                );
                target.setUpdatedAt(new Date());
                mongoTemplate.save(target, COLLECTION_NAME);
            }

            log.info("✅ 关注成功: {} 关注了 {}", userId, targetUserId);
        } catch (Exception e) {
            log.error("❌ 关注用户失败, userId: {}, targetUserId: {}, 错误: {}", 
                userId, targetUserId, e.getMessage(), e);
            throw new RuntimeException("关注用户失败", e);
        }
    }

    /**
     * 取消关注用户
     */
    @Override
    public void unfollowUser(String userId, String targetUserId) {
        try {
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(targetUserId)) {
                throw new IllegalArgumentException("用户ID不能为空");
            }

            // 更新关注者的关注数
            User follower = mongoTemplate.findById(userId, User.class, COLLECTION_NAME);
            if (follower != null && follower.getFollowingCount() != null && follower.getFollowingCount() > 0) {
                follower.setFollowingCount(follower.getFollowingCount() - 1);
                follower.setUpdatedAt(new Date());
                mongoTemplate.save(follower, COLLECTION_NAME);
            }

            // 更新被关注者的粉丝数
            User target = mongoTemplate.findById(targetUserId, User.class, COLLECTION_NAME);
            if (target != null && target.getFollowerCount() != null && target.getFollowerCount() > 0) {
                target.setFollowerCount(target.getFollowerCount() - 1);
                target.setUpdatedAt(new Date());
                mongoTemplate.save(target, COLLECTION_NAME);
            }

            log.info("✅ 取消关注成功: {} 取消关注了 {}", userId, targetUserId);
        } catch (Exception e) {
            log.error("❌ 取消关注失败, userId: {}, targetUserId: {}, 错误: {}", 
                userId, targetUserId, e.getMessage(), e);
            throw new RuntimeException("取消关注失败", e);
        }
    }

    /**
     * 处理头像 URL，生成带签名的 URL
     */
    private void processAvatarUrl(User user) {
        if (user != null && StringUtils.hasText(user.getAvatarUrl())) {
            String avatarUrl = user.getAvatarUrl();
            if (!avatarUrl.startsWith("/")) {
                avatarUrl = "/" + avatarUrl;
            }
            // 生成带版本的签名 URL
            user.setAvatarUrl(cdnDecrypt.generateSignedPathWithVersion(avatarUrl));
        }
    }

    /**
     * 获取安全的用户对象（移除敏感信息）
     */
    private User getSafeUser(User user) {
        if (user == null) {
            return null;
        }

        // 创建新的用户对象，不包含敏感信息
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setUsernameZHTW(user.getUsernameZHTW());
        safeUser.setUsernameENUS(user.getUsernameENUS());
        safeUser.setUsernameJAJP(user.getUsernameJAJP());
        safeUser.setUsernameKOKR(user.getUsernameKOKR());
        safeUser.setUsernameESES(user.getUsernameESES());
        safeUser.setUsernameTHTH(user.getUsernameTHTH());
        safeUser.setUsernameVIVN(user.getUsernameVIVN());
        safeUser.setUsernameMSMY(user.getUsernameMSMY());
        safeUser.setEmail(user.getEmail());
        safeUser.setStyle(user.getStyle());
        safeUser.setPublicCount(user.getPublicCount());
        safeUser.setLikeVideoCount(user.getLikeVideoCount());
        safeUser.setVideoHistoryCount(user.getVideoHistoryCount());
        safeUser.setSaveVideoCount(user.getSaveVideoCount());
        safeUser.setDislikeVideoCount(user.getDislikeVideoCount());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setIsVerifyEmail(user.getIsVerifyEmail());
        safeUser.setTotalViews(user.getTotalViews());
        safeUser.setRank(user.getRank());
        safeUser.setViews(user.getViews());
        safeUser.setSubscribe(user.getSubscribe());
        safeUser.setIntroduce(user.getIntroduce());
        safeUser.setIntroduceZHTW(user.getIntroduceZHTW());
        safeUser.setIntroduceENUS(user.getIntroduceENUS());
        safeUser.setIntroduceJAJP(user.getIntroduceJAJP());
        safeUser.setIntroduceKOKR(user.getIntroduceKOKR());
        safeUser.setIntroduceESES(user.getIntroduceESES());
        safeUser.setIntroduceTHTH(user.getIntroduceTHTH());
        safeUser.setIntroduceVIVN(user.getIntroduceVIVN());
        safeUser.setIntroduceMSMY(user.getIntroduceMSMY());
        safeUser.setSex(user.getSex());
        safeUser.setHeight(user.getHeight());
        safeUser.setRelationshipStatus(user.getRelationshipStatus());
        safeUser.setLikeCommentCount(user.getLikeCommentCount());
        safeUser.setCurrentCity(user.getCurrentCity());
        safeUser.setHometown(user.getHometown());
        safeUser.setEthnicity(user.getEthnicity());
        safeUser.setHobbies(user.getHobbies());
        safeUser.setHomepageViews(user.getHomepageViews());
        safeUser.setSocialLinks(user.getSocialLinks());
        safeUser.setBirthdate(user.getBirthdate());
        safeUser.setFollowerCount(user.getFollowerCount());
        safeUser.setFollowingCount(user.getFollowingCount());
        safeUser.setFollowInfo(user.getFollowInfo());
        safeUser.setCreatedAt(user.getCreatedAt());
        safeUser.setUpdatedAt(user.getUpdatedAt());

        // 不设置 password 和 key

        return safeUser;
    }
}
