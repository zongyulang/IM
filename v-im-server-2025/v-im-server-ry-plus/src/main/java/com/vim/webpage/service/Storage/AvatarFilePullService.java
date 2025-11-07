package com.vim.webpage.service.Storage;

import com.vim.webpage.Base.ObjectStorage.ObjFileManager;
import com.vim.webpage.config.StorageConfig;
import com.vim.webpage.manager.Storage.RedisFileManager;
import com.vim.webpage.Utils.FilePathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 头像文件拉取服务，使用redis锁，保证线程安全
 */
@Slf4j
@Service
public class AvatarFilePullService {

    @Autowired
    private ObjFileManager objFileManager;

    @Autowired
    private RedisFileManager redisFileManager;

    @Autowired
    private StorageConfig storageConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String FILE_LOCK_PREFIX = "file:lock:";
    private static final String FILE_REF_COUNT_PREFIX = "file:ref:";
    private static final long LOCK_TIMEOUT_SECONDS = 30;
    private static final long DOWNLOAD_TIMEOUT_SECONDS = 60;
    private static final long DELETE_WAIT_TIMEOUT_MS = 5000; // 删除等待超时

    /**
     * 获取头像文件，redis线程锁
     */
    public String getAvatar(String avatarPath) {
        String refCountKey = FILE_REF_COUNT_PREFIX + avatarPath;

        try {
            // increment，原子性
            redisTemplate.opsForValue().increment(refCountKey);
            //设置删除锁的过期时间，下载计数，作用：防止下载的时候正在删除
            redisTemplate.expire(refCountKey, Duration.ofSeconds(storageConfig.getLockTimeoutSeconds()));

            try {
                String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), avatarPath);
                Path path = Paths.get(localPath);

                // 检查文件是否存在且完整
                if (Files.exists(path) && Files.isRegularFile(path)) {
                    log.info("Avatar exists locally: {}", localPath);

                    // 刷新 Redis 过期时间
                    String fileName = FilePathUtil.getFileName(avatarPath);
                    redisFileManager.refreshExpireTime("avatar:" + fileName);

                    return localPath;
                } else {
                    log.info("Avatar not found locally, downloading from object storage: {}", avatarPath);
                    return downloadAvatarWithLock(avatarPath);
                }
            } finally {
                // 减少引用计数
                Long refCount = redisTemplate.opsForValue().decrement(refCountKey);
                if (refCount != null && refCount <= 0) {
                    redisTemplate.delete(refCountKey);
                }
            }

        } catch (Exception e) {
            log.error("Error getting avatar: {}", avatarPath, e);
            throw new RuntimeException("Failed to get avatar: " + avatarPath, e);
        }
    }

    /**
     * 使用分布式锁下载头像文件
     */
    private String downloadAvatarWithLock(String avatarPath) {
        String lockKey = FILE_LOCK_PREFIX + avatarPath;
        String lockValue = String.valueOf(System.currentTimeMillis());

        try {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (Boolean.TRUE.equals(locked)) {
                try {
                    return downloadAvatar(avatarPath);
                } finally {
                    String currentValue = redisTemplate.opsForValue().get(lockKey);
                    if (lockValue.equals(currentValue)) {
                        redisTemplate.delete(lockKey);
                    }
                }
            } else {
                log.info("Another thread is downloading, waiting: {}", avatarPath);
                return waitForDownload(avatarPath, lockKey);
            }

        } catch (Exception e) {
            log.error("Error in downloadAvatarWithLock: {}", avatarPath, e);
            throw new RuntimeException("Failed to download avatar with lock: " + avatarPath, e);
        }
    }

    /**
     * 等待其他线程下载完成
     */
    private String waitForDownload(String avatarPath, String lockKey) throws InterruptedException {
        String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), avatarPath);
        Path path = Paths.get(localPath);

        long startTime = System.currentTimeMillis();
        long timeout = DOWNLOAD_TIMEOUT_SECONDS * 1000;

        while (System.currentTimeMillis() - startTime < timeout) {

            if (Files.exists(path) && Files.isRegularFile(path)) {
                log.info("File downloaded by another thread: {}", localPath);
                return localPath;
            }

            String lockValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue == null) {
                log.warn("Lock released but file not found, retrying download: {}", avatarPath);
                return downloadAvatarWithLock(avatarPath);
            }
            // 等待下载完成
            Thread.sleep(100);
        }

        throw new RuntimeException("Timeout waiting for avatar download: " + avatarPath);
    }

    /**
     * 下载头像文件（同步等待异步完成）
     */
    private String downloadAvatar(String avatarPath) {
        try {
            String normalized = FilePathUtil.normalizePath(avatarPath);
            int lastSlash = normalized.lastIndexOf('/');
            String folderPath = lastSlash > 0 ? normalized.substring(0, lastSlash) : "avatar";

            String downloadPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), folderPath);

            CompletableFuture<String> future = objFileManager.downloadFileAsync(avatarPath, downloadPath);
            String downloadedPath = future.get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Path path = Paths.get(downloadedPath);
            if (!Files.exists(path)) {
                throw new RuntimeException("Downloaded file is empty or missing: " + downloadedPath);
            }

            String fileName = FilePathUtil.getFileName(avatarPath);
            redisFileManager.setFileRootPath(
                    com.vim.webpage.enums.FileTypeEnum.AVATAR,
                    folderPath,
                    "avatar:" + fileName);

            String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), avatarPath);
            log.info("Successfully downloaded avatar to: {}", localPath);
            return localPath;

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Download timeout for avatar: {}", avatarPath, e);
            throw new RuntimeException("Avatar download timeout: " + avatarPath, e);
        } catch (Exception e) {
            log.error("Error downloading avatar: {}", avatarPath, e);
            throw new RuntimeException("Failed to download avatar: " + avatarPath, e);
        }
    }

    /**
     * 删除头像文件（线程安全版本）
     */
    public void deleteAvatar(String avatarPath) {
        String lockKey = FILE_LOCK_PREFIX + avatarPath;
        String refCountKey = FILE_REF_COUNT_PREFIX + avatarPath;
        String lockValue = String.valueOf(System.currentTimeMillis());

        try {
            // 1. 获取分布式锁
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (!Boolean.TRUE.equals(locked)) {
                log.warn("Another operation is in progress for file: {}", avatarPath);
                throw new RuntimeException("File is being accessed by another thread: " + avatarPath);
            }

            try {
                // 2. 等待所有读取操作完成（引用计数归零）
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < DELETE_WAIT_TIMEOUT_MS) {
                    String refCountStr = redisTemplate.opsForValue().get(refCountKey);
                    long refCount = refCountStr != null ? Long.parseLong(refCountStr) : 0;

                    if (refCount <= 0) {
                        // 没有线程在使用，可以安全删除
                        break;
                    }

                    log.info("Waiting for {} active references to complete for file: {}", refCount, avatarPath);
                    Thread.sleep(100);
                }

                // 3. 执行删除操作
                String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), avatarPath);
                Path path = Paths.get(localPath);

                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Deleted avatar: {}", localPath);

                    // 删除 Redis 缓存
                    String fileName = FilePathUtil.getFileName(avatarPath);
                    redisFileManager.deleteFileCache("avatar:" + fileName);
                    redisTemplate.delete(refCountKey);
                } else {
                    log.warn("File not found for deletion: {}", localPath);
                }

            } finally {
                // 4. 释放锁
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Delete operation interrupted for avatar: {}", avatarPath, e);
            throw new RuntimeException("Delete operation interrupted: " + avatarPath, e);
        } catch (Exception e) {
            log.error("Error deleting avatar: {}", avatarPath, e);
            throw new RuntimeException("Failed to delete avatar: " + avatarPath, e);
        }
    }

    /**
     * 批量删除头像文件
     */
    public void deleteAvatars(String... avatarPaths) {
        for (String avatarPath : avatarPaths) {
            try {
                deleteAvatar(avatarPath);
            } catch (Exception e) {
                log.error("Failed to delete avatar in batch: {}", avatarPath, e);
                // 继续删除其他文件
            }
        }
    }
}