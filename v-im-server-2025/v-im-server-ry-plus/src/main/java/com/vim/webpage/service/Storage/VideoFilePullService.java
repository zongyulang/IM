package com.vim.webpage.service.Storage;

import com.vim.webpage.manager.ObjectStorage.ObjFileManager;
import com.vim.webpage.config.StorageConfig;
import com.vim.webpage.enums.FileTypeEnum;
import com.vim.webpage.manager.Storage.RedisFileManager;
import com.vim.webpage.Utils.FilePathUtil;
import com.vim.webpage.Utils.FileTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 视频文件拉取服务
 * 步骤：
 * 1请求来时检查本地文件是否存在且有效，存在则直接返回路径并刷新过期时间
 * 2本地文件不存在时，使用分布式锁防止多线程/多实例重复下载
 * 3下载相关文件（缩略图、m3u8、ts、key、预览图等）
 * 4下载完成后释放锁，其他等待线程获取文件
 */
@Slf4j
@Service
public class VideoFilePullService {

    @Autowired
    private ObjFileManager objFileManager;

    @Autowired
    private RedisFileManager redisFileManager;

    @Autowired
    private StorageConfig storageConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String FILE_LOCK_PREFIX = "videoLock:";//下载所
    private static final String FILE_REF_COUNT_PREFIX = "videoRef:";//删除锁
    private static final long LOCK_TIMEOUT_SECONDS = 60;//所的过期时间
    private static final long DOWNLOAD_TIMEOUT_SECONDS = 35; // 等待下载时长
    private static final long DELETE_WAIT_TIMEOUT_MS = 10000; // 删除等待超时10秒

    /**
     * 检查本地文件是否存在且有效
     */
    private boolean isFileValid(Path path) {
        try {
            //isRegularFile:是否是一个标准文件（不是目录、不是符号链接等）
            return Files.exists(path) && Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (Exception e) {
            log.warn("Error checking file validity: {}", path, e);
            return false;
        }
    }

    /**
     * 检查本地文件是否存在
     */
    public boolean isFileExists(String filePath) {
        try {
            String normalized = FilePathUtil.normalizePath(filePath);
            Path path = Paths.get(normalized);
            return isFileValid(path);
        } catch (Exception e) {
            log.error("Error checking file existence: {}", filePath, e);
            return false;
        }
    }

    /**
     * 获取文件 - 主入口-线程安全
     * 逻辑：先检查本地是否存在，存在则刷新过期时间；不存在则从对象存储下载
     */
    public String getFile(String filePath) {
        FileTypeEnum fileType = FileTypeUtil.getFileType(filePath);
        String rootPath = FilePathUtil.getRootPath(filePath, fileType);
        String uuid = FilePathUtil.getUUID(filePath);
        String refCountKey = FILE_REF_COUNT_PREFIX + (uuid != null ? uuid : filePath);

        try {
            // 增加引用计数
            redisTemplate.opsForValue().increment(refCountKey);
            redisTemplate.expire(refCountKey, Duration.ofSeconds(storageConfig.getLockTimeoutSeconds()));

            try {
                String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), filePath);
                Path path = Paths.get(localPath);

                // 检查文件是否存在且有效
                if (isFileValid(path)) {
                    log.info("File exists locally: {}", localPath);

                    // 刷新 Redis 过期时间
                    if (uuid != null && rootPath != null) {
                        redisFileManager.refreshExpireTime(uuid);
                    }

                    return localPath;
                } else {
                    log.info("File not found locally, downloading from object storage: {}", filePath);
                    return downloadRelatedFilesWithLock(filePath);
                }
            } finally {
                // 减少引用计数
                Long refCount = redisTemplate.opsForValue().decrement(refCountKey);
                if (refCount != null && refCount <= 0) {
                    redisTemplate.delete(refCountKey);
                }
            }

        } catch (Exception e) {
            log.error("Error getting file: {}", filePath, e);
            throw new RuntimeException("Failed to get file: " + filePath, e);
        }
    }

    /**
     * 使用分布式锁下载相关文件
     */
    private String downloadRelatedFilesWithLock(String filePath) {
        String uuid = FilePathUtil.getUUID(filePath);
        //suo名称
        String lockKey = FILE_LOCK_PREFIX + (uuid != null ? uuid : filePath);
        String lockValue = String.valueOf(System.currentTimeMillis());

        try {
            //设置锁，过期时间为LOCK_TIMEOUT_SECONDS，防止死锁，当存在锁是返回false
            //setIfAbsent相当于rediscli命令：SET key value NX EX seconds
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (Boolean.TRUE.equals(locked)) {
                try {
                    return downloadRelatedFiles(filePath);
                } finally {
                    String currentValue = redisTemplate.opsForValue().get(lockKey);
                    if (lockValue.equals(currentValue)) {
                        redisTemplate.delete(lockKey);
                    }
                }
            } else {
                log.info("Another thread is downloading, waiting: {}", filePath);
                return waitForDownload(filePath, lockKey);
            }

        } catch (Exception e) {
            log.error("Error in downloadRelatedFilesWithLock: {}", filePath, e);
            throw new RuntimeException("Failed to download with lock: " + filePath, e);
        }
    }

    /**
     * 等待其他线程下载完成
     */
    private String waitForDownload(String filePath, String lockKey) throws InterruptedException {
        String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), filePath);
        Path path = Paths.get(localPath);

        long startTime = System.currentTimeMillis();
        long timeout = DOWNLOAD_TIMEOUT_SECONDS * 1000;

        while (System.currentTimeMillis() - startTime < timeout) {
            if (isFileValid(path)) {
                log.info("File downloaded by another thread: {}", localPath);
                return localPath;
            }

            String lockValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue == null) {
                log.warn("Lock released but file not found, retrying download: {}", filePath);
                return downloadRelatedFilesWithLock(filePath);
            }

            Thread.sleep(200); // 等待200ms后重试
        }

        throw new RuntimeException("Timeout waiting for file download: " + filePath);
    }

    /**
     * 下载相关文件
     * 根据文件类型下载对应的文件或文件夹
     */
    private String downloadRelatedFiles(String filePath) {
        FileTypeEnum fileType = FileTypeUtil.getFileType(filePath);
        String uuid = FilePathUtil.getUUID(filePath);

        try {
            switch (fileType) {
                case THUMBNAIL:
                    // 下载缩略图（包含 combined 视频）
                    downloadThumbnailFiles(filePath, uuid);
                    break;
                    
                case KEY:
                    // 下载整个 key 所在的文件夹(异步)
                    String keyFolder = FilePathUtil.normalizePath(filePath);
                    int lastSlash = keyFolder.lastIndexOf('/');
                    if (lastSlash > 0) {
                        keyFolder = keyFolder.substring(0, lastSlash);
                    }
                    String keyDownloadPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), keyFolder);
                    log.info("Downloading key folder: {}", keyFolder);
                    downloadFolderSynchronously(keyFolder, keyDownloadPath);
                    break;
                    
                case M3U8:
                case TS:
                    // 下载单个 m3u8 或 ts 文件
                    String videoFolder = FilePathUtil.normalizePath(filePath);
                    int videoLastSlash = videoFolder.lastIndexOf('/');
                    if (videoLastSlash > 0) {
                        videoFolder = videoFolder.substring(0, videoLastSlash);
                    }
                    String videoDownloadPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), videoFolder);
                    log.info("Downloading {} file: {}", fileType.getType(), filePath);
                    downloadFileSynchronously(filePath, videoDownloadPath);
                    break;
                    
                case PREVIEW:
                    // 下载整个预览图文件夹(异步)
                    String previewFolder = FilePathUtil.normalizePath(filePath);
                    int previewLastSlash = previewFolder.lastIndexOf('/');
                    if (previewLastSlash > 0) {
                        previewFolder = previewFolder.substring(0, previewLastSlash);
                    }
                    String previewDownloadPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), previewFolder);
                    log.info("Downloading preview folder: {}", previewFolder);
                    downloadFolderSynchronously(previewFolder, previewDownloadPath);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
            
            // ⭐ 设置 Redis 缓存（所有文件类型下载完成后统一设置）
            if (uuid != null) {
                String rootPath = FilePathUtil.getRootPath(filePath, fileType);
                if (rootPath != null) {
                    redisFileManager.setFileRootPath(fileType, rootPath, uuid);
                    log.info("✅ Set Redis hash for type: {}, uuid: {}, root: {}", 
                            fileType.getType(), uuid, rootPath);
                } else {
                    log.warn("⚠️ Cannot set Redis: rootPath is null for filePath: {}", filePath);
                }
            } else {
                log.warn("⚠️ Cannot set Redis: uuid is null for filePath: {}", filePath);
            }
            
            String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), filePath);
            log.info("✅ Successfully downloaded and cached file: {}", localPath);
            return localPath;
            
        } catch (Exception e) {
            // 检查是否是文件不存在的错误
            if (e.getMessage() != null && (e.getMessage().contains("NoSuchKey") 
                    || e.getMessage().contains("not found") 
                    || e.getMessage().contains("does not exist"))) {
                log.warn("❌ File not found in storage: {}", filePath);
                throw new RuntimeException("File not found in storage: " + filePath, e);
            }
            
            log.error("❌ Error downloading files for: {}", filePath, e);
            throw new RuntimeException("Failed to download files: " + filePath, e);
        }
    }

    /**
     * 下载缩略图文件（包含 combined 视频）
     * 对应 Node.js 的 combineThumbnailNeed 逻辑
     */
    private void downloadThumbnailFiles(String filePath, String uuid) {
        try {
            // 提取缩略图和 combined 视频的路径
            // 例如: video/2025/07/uuid/Thumbnail.webp
            //      video/2025/07/uuid/Thumbnail_combined.mp4
            String normalized = FilePathUtil.normalizePath(filePath);
            int lastSlash = normalized.lastIndexOf('/');
            String folder = lastSlash > 0 ? normalized.substring(0, lastSlash) : "";
            
            // 构建缩略图文件路径
            String thumbnailPath = filePath;
            
            // 构建 combined 视频路径（固定为 Thumbnail_combined.mp4）
            String combinedVideoPath = FilePathUtil.joinPath(folder, "Thumbnail_combined.mp4");
            
            String downloadPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), folder);
            
            log.info("Downloading thumbnail files to: {}", downloadPath);
            log.info("Files: {} and {}", thumbnailPath, combinedVideoPath);
            
            // 使用批量下载（并发下载2个文件）
            java.util.List<String> filePaths = java.util.Arrays.asList(thumbnailPath, combinedVideoPath);
            CompletableFuture<ObjFileManager.DownloadSummary> downloadFuture = objFileManager.downloadFilesAsync(filePaths, downloadPath);
            
            // 等待下载完成
            ObjFileManager.DownloadSummary summary = downloadFuture.get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            log.info("✅ Successfully downloaded thumbnail files: success={}, failed={}", 
                    summary.getSuccess(), summary.getFailed());
            
            if (summary.getFailed() > 0) {
                log.warn("⚠️ Some thumbnail files failed to download: {}", summary.getErrors());
            }
            
        } catch (Exception e) {
            log.error("❌ Error downloading thumbnail files: {}", filePath, e);
            throw new RuntimeException("Failed to download thumbnail files: " + filePath, e);
        }
    }

    /**
     * 同步下载单个文件
     */
    private void downloadFileSynchronously(String filePath, String downloadPath) {
        try {
            CompletableFuture<String> future = objFileManager.downloadFileAsync(filePath, downloadPath);
            String downloadedPath = future.get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            Path path = Paths.get(downloadedPath);
            if (!isFileValid(path)) {
                throw new RuntimeException("Downloaded file is empty or missing: " + downloadedPath);
            }
            
            log.debug("Successfully downloaded file: {}", downloadedPath);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Download timeout for file: {}", filePath, e);
            throw new RuntimeException("File download timeout: " + filePath, e);
        } catch (Exception e) {
            log.error("Error downloading file: {}", filePath, e);
            throw new RuntimeException("Failed to download file: " + filePath, e);
        }
    }

    /**
     * 同步下载整个文件夹(使用异步方法并等待完成)
     */
    private void downloadFolderSynchronously(String folderPrefix, String downloadPath) {
        try {
            CompletableFuture<Boolean> future = objFileManager.downloadFolderAsync(folderPrefix, downloadPath);
            Boolean success = future.get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!success) {
                throw new RuntimeException("Folder download failed: " + folderPrefix);
            }
            
            // 验证下载的文件夹是否存在
            Path folderPath = Paths.get(downloadPath);
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                throw new RuntimeException("Downloaded folder is missing: " + downloadPath);
            }
            
            log.debug("Successfully downloaded folder: {}", downloadPath);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Download timeout for folder: {}", folderPrefix, e);
            throw new RuntimeException("Folder download timeout: " + folderPrefix, e);
        } catch (Exception e) {
            log.error("Error downloading folder: {}", folderPrefix, e);
            throw new RuntimeException("Failed to download folder: " + folderPrefix, e);
        }
    }

    /**
     * 删除缩略图文件(线程安全版本)
     */
    public void deleteThumbnailFiles(String rootPath) {
        deleteFilesWithLock(rootPath, FileTypeEnum.THUMBNAIL);
    }

    /**
     * 删除预览图文件（线程安全版本）
     */
    public void deletePreviewFiles(String rootPath) {
        deleteFilesWithLock(rootPath, FileTypeEnum.PREVIEW);
    }

    /**
     * 删除 TS 文件（线程安全版本）
     */
    public void deleteTsFiles(String rootPath) {
        deleteFilesWithLock(rootPath, FileTypeEnum.TS);
    }

    /**
     * 删除 M3U8 文件（线程安全版本）
     */
    public void deleteM3u8Files(String rootPath) {
        deleteFilesWithLock(rootPath, FileTypeEnum.M3U8);
    }

    /**
     * 删除 Key 文件（线程安全版本）
     */
    public void deleteKeyFiles(String rootPath) {
        deleteFilesWithLock(rootPath, FileTypeEnum.KEY);
    }

    /**
     * 使用分布式锁删除文件
     */
    private void deleteFilesWithLock(String rootPath, FileTypeEnum fileType) {
        String uuid = FilePathUtil.getUUID(rootPath);
        String lockKey = FILE_LOCK_PREFIX + (uuid != null ? uuid : rootPath);
        String refCountKey = FILE_REF_COUNT_PREFIX + (uuid != null ? uuid : rootPath);
        String lockValue = String.valueOf(System.currentTimeMillis());

        try {
            // 获取分布式锁
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (!Boolean.TRUE.equals(locked)) {
                log.warn("Another operation is in progress for root: {}", rootPath);
                throw new RuntimeException("Files are being accessed: " + rootPath);
            }

            try {
                // 等待所有读取操作完成
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < DELETE_WAIT_TIMEOUT_MS) {
                    String refCountStr = redisTemplate.opsForValue().get(refCountKey);
                    long refCount = refCountStr != null ? Long.parseLong(refCountStr) : 0;

                    if (refCount <= 0) {
                        break;
                    }

                    log.info("Waiting for {} active references for root: {}", refCount, rootPath);
                    Thread.sleep(200);
                }

                // 执行删除操作
                performDelete(rootPath, fileType);

            } finally {
                // 释放锁
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Delete operation interrupted for root: {}", rootPath, e);
            throw new RuntimeException("Delete operation interrupted: " + rootPath, e);
        } catch (Exception e) {
            log.error("Error deleting files for root: {}", rootPath, e);
            throw new RuntimeException("Failed to delete files: " + rootPath, e);
        }
    }

    /**
     * redis过期时
     * 执行实际的删除操作
     */
    private void performDelete(String rootPath, FileTypeEnum fileType) {
        try {
            switch (fileType) {
                case THUMBNAIL:
                    String localPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), rootPath);
                    deleteLocalFolder(localPath);
                    log.info("Deleted thumbnail files for root: {}", rootPath);
                    break;

                case PREVIEW:
                    String previewFolder = FilePathUtil.joinPath(rootPath, "preview_image");
                    String previewPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), previewFolder);
                    deleteLocalFolder(previewPath);
                    log.info("Deleted preview files for root: {}", rootPath);
                    break;

                case TS:
                    String[] resolutions = {"720P", "1080P"};
                    for (String resolution : resolutions) {
                        String tsFolder = FilePathUtil.joinPath(rootPath, resolution);
                        String tsPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), tsFolder);
                        deleteLocalFolder(tsPath);
                    }
                    log.info("Deleted TS files for root: {}", rootPath);
                    break;

                case M3U8:
                    String m3u8Path = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), rootPath);
                    deleteLocalFiles(m3u8Path, ".m3u8");
                    log.info("Deleted M3U8 files for root: {}", rootPath);
                    break;

                case KEY:
                    String keyPath = FilePathUtil.joinPath(storageConfig.getLocalBasePath(), rootPath);
                    deleteLocalFiles(keyPath, ".key");
                    log.info("Deleted Key files for root: {}", rootPath);
                    break;

                default:
                    log.warn("Unknown file type for deletion: {}", fileType);
                    break;
            }
        } catch (Exception e) {
            log.error("Error performing delete for type {}: {}", fileType, rootPath, e);
            throw new RuntimeException("Failed to perform delete: " + rootPath, e);
        }
    }

    /**
     * 删除本地文件夹
     */
    private void deleteLocalFolder(String folderPath) throws IOException {
        Path path = Paths.get(folderPath);
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件再删除目录
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        log.error("Failed to delete: {}", p, e);
                    }
                });
            log.debug("Deleted folder: {}", folderPath);
        }
    }

    /**
     * 删除指定扩展名的本地文件
     */
    private void deleteLocalFiles(String folderPath, String extension) throws IOException {
        Path path = Paths.get(folderPath);
        if (Files.exists(path) && Files.isDirectory(path)) {
            Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(extension))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                        log.debug("Deleted file: {}", p);
                    } catch (IOException e) {
                        log.error("Failed to delete file: {}", p, e);
                    }
                });
        }
    }

    /**
     * 处理过期检查消息
     * 当 Redis 键过期时触发
     */
    public void handleExpiration(String key, String message) {
        try {
            log.info("Handling expiration for key: {}, message: {}", key, message);
            
            // 从 Redis 获取根路径
            String rootPath = redisFileManager.getFileRootPath(key);
            if (rootPath == null) {
                log.warn("Root path not found for expired key: {}", key);
                return;
            }
            
            // 根据消息类型删除对应文件
            FileTypeEnum fileType = FileTypeUtil.getFileType(message);
            deleteFilesByType(fileType, rootPath);
            
        } catch (Exception e) {
            log.error("Error handling expiration for key: {}", key, e);
        }
    }

    /**
     * 根据文件类型删除文件
     */
    public void deleteFilesByType(FileTypeEnum fileType, String rootPath) {
        switch (fileType) {
            case THUMBNAIL:
                deleteThumbnailFiles(rootPath);
                break;
            case PREVIEW:
                deletePreviewFiles(rootPath);
                break;
            case TS:
                deleteTsFiles(rootPath);
                break;
            case M3U8:
                deleteM3u8Files(rootPath);
                break;
            case KEY:
                deleteKeyFiles(rootPath);
                break;
            default:
                log.warn("Unknown file type for deletion: {}", fileType);
                break;
        }
    }
}
