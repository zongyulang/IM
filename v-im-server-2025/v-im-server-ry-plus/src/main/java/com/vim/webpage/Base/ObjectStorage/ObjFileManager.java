package com.vim.webpage.Base.ObjectStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

//download使用nio的特性completablefuture不阻塞线程
@Slf4j
@Component
public class ObjFileManager {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    // #region 上传文件

    // ----------------------------------------------------upload------------------------------------
    /**
     * 获取目录下所有文件
     */
    private List<Path> getAllFiles(String dirPath) throws IOException {
        List<Path> filePaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            filePaths = paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return filePaths;
    }

    /**
     * 上传单个文件到 S3
     */
    public boolean uploadFile(String filePath, String directoryPath, String s3Prefix) {
        try {
            Path file = Paths.get(filePath);
            Path directory = Paths.get(directoryPath);
            Path relativePath = directory.relativize(file);
            String s3Key = s3Prefix + "/" + relativePath.toString().replace("\\", "/");

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            return true;
        } catch (Exception e) {
            log.error("Error uploading file: {}", filePath, e);
            return false;
        }
    }

    /**
     * 批量上传文件
     */
    private List<String> uploadBatchWithFailures(List<String> files, String directoryPath, String objectStoragePath) {
        List<String> failedFiles = new ArrayList<>();

        for (String file : files) {
            boolean success = uploadFile(file, directoryPath, objectStoragePath);
            if (!success) {
                failedFiles.add(file);
            }
        }

        if (failedFiles.isEmpty()) {
            log.info("Batch of {} files uploaded successfully.", files.size());
        } else {
            log.error("Batch upload failed for files: {}", String.join(", ", failedFiles));
        }

        return failedFiles;
    }

    /**
     * 上传整个目录到 S3
     */
    public boolean uploadDirectory(String directoryPath, String objectStoragePath) {
        try {
            // 获取文件夹下所有文件
            List<Path> files = getAllFiles(directoryPath);
            List<String> filePaths = files.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());

            int batchSize = 50; // 每批上传 50 个文件
            List<String> failedFiles = new ArrayList<>();

            // 分批上传
            for (int i = 0; i < filePaths.size(); i += batchSize) {
                int end = Math.min(i + batchSize, filePaths.size());
                List<String> batch = filePaths.subList(i, end);
                List<String> batchFailed = uploadBatchWithFailures(batch, directoryPath, objectStoragePath);
                failedFiles.addAll(batchFailed);
            }

            // 如果存在上传失败的文件,则重试一次
            if (!failedFiles.isEmpty()) {
                log.info("Retry uploading {} failed files.", failedFiles.size());
                List<String> retryFailed = new ArrayList<>();

                for (int i = 0; i < failedFiles.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, failedFiles.size());
                    List<String> batch = failedFiles.subList(i, end);
                    List<String> batchFailed = uploadBatchWithFailures(batch, directoryPath, objectStoragePath);
                    retryFailed.addAll(batchFailed);
                }

                failedFiles = retryFailed;
            }

            if (!failedFiles.isEmpty()) {
                log.error("Upload failed for {} files: {}", failedFiles.size(), String.join(", ", failedFiles));
                return false;
            } else {
                log.info("All files uploaded successfully");
                return true;
            }

        } catch (Exception e) {
            log.error("Error uploading directory", e);
            return false;
        }
    }

    // 用法示例：
    // @Autowired
    // private ObjFileManager objFileManager;
    // // 上传单个文件
    // objFileManager.uploadFile("/path/to/file.txt", "/base/path", "s3/prefix");
    // // 上传整个目录
    // objFileManager.uploadDirectory("/local/directory", "s3/prefix");

    // endregion
    // ----------------------------------------------------download------------------------------------

    // #region 下载文件

    /**
     * 异步下载文件夹（使用 S3AsyncClient）
     * 按照给定的前缀列举对象，并发下载到本地 downloadPath 下保留相对目录结构。
     *
     * @param folderPrefix S3 文件夹前缀，例如: "video/2025/07/uuid"
     * @param downloadPath 本地下载根路径，例如: "public/video/2025/07/uuid"
     * @return CompletableFuture<Boolean> 所有对象下载成功返回 true，任一失败则抛出异常或返回 false
     */
    public CompletableFuture<Boolean> downloadFolderAsync(String folderPrefix, String downloadPath) {
        try {
            final String sanitizedPrefix = folderPrefix.startsWith("/") ? folderPrefix.substring(1) : folderPrefix;

            boolean isTruncated = true;
            String continuationToken = null;
            final List<S3Object> allObjects = new ArrayList<>();

            while (isTruncated) {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(sanitizedPrefix);
                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(requestBuilder.build());
                if (listObjectsResponse.contents() != null) {
                    allObjects.addAll(listObjectsResponse.contents());
                }
                isTruncated = listObjectsResponse.isTruncated();
                continuationToken = listObjectsResponse.nextContinuationToken();
            }

            final List<S3Object> filteredObjects = allObjects.stream()
                    .filter(obj -> obj != null && obj.key() != null)
                    .collect(Collectors.toList());

            if (filteredObjects.isEmpty()) {
                log.info("No objects found under prefix: {}", sanitizedPrefix);
                return CompletableFuture.completedFuture(true);
            }

            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (S3Object obj : filteredObjects) {
                final String key = obj.key();
                String relativePath;
                if (key.startsWith(sanitizedPrefix)) {
                    relativePath = key.substring(sanitizedPrefix.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                } else {
                    relativePath = key;
                }
                final String targetFilePath = Paths.get(downloadPath, relativePath).toString().replace("\\", "/");
                try {
                    Path parent = Paths.get(targetFilePath).getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                } catch (IOException ioe) {
                    log.error("Failed to create directories for {}", targetFilePath, ioe);
                    return CompletableFuture.failedFuture(ioe);
                }
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                CompletableFuture<?> fut = s3AsyncClient
                        .getObject(getObjectRequest, AsyncResponseTransformer.toFile(Paths.get(targetFilePath)))
                        .thenAccept(resp -> log.debug("Downloaded {} to {}", key, targetFilePath))
                        .exceptionally(ex -> {
                            log.error("Error downloading {} -> {}", key, targetFilePath, ex);
                            throw new RuntimeException("Failed to download object: " + key, ex);
                        });
                futures.add(fut);
            }

            final int objectCount = filteredObjects.size();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                    .thenApply(v -> {
                        log.info("Successfully downloaded folder {} to {} ({} objects)", sanitizedPrefix, downloadPath,
                                objectCount);
                        return true;
                    });
        } catch (Exception e) {
            log.error("Error setting up async download for folder {} -> {}", folderPrefix, downloadPath, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 下载结果类
     */
    public static class DownloadResult {
        private boolean success;
        private String fileKey;
        private String downloadPath;
        private String filePath;
        private String error;
        private int index;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getFileKey() {
            return fileKey;
        }

        public void setFileKey(String fileKey) {
            this.fileKey = fileKey;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    /**
     * 下载汇总结果类
     */
    public static class DownloadSummary {
        private int total;
        private int success;
        private int failed;
        private List<DownloadResult> results;
        private List<DownloadResult> errors;

        public DownloadSummary() {
            this.results = new ArrayList<>();
            this.errors = new ArrayList<>();
        }

        // Getters and Setters
        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public List<DownloadResult> getResults() {
            return results;
        }

        public void setResults(List<DownloadResult> results) {
            this.results = results;
        }

        public List<DownloadResult> getErrors() {
            return errors;
        }

        public void setErrors(List<DownloadResult> errors) {
            this.errors = errors;
        }
    }

    /*
     * 用法示例：
     * 
     * // 异步下载单个文件
     * CompletableFuture<String> future =
     * objFileManager.downloadFileAsync("video/2024/file.mp4", "./downloads");
     * future.thenAccept(path -> System.out.println("Downloaded to: " + path));
     * 
     * // 异步下载多个文件
     * CompletableFuture<DownloadSummary> summaryFuture =
     * objFileManager.downloadFilesAsync(files, "./downloads");
     * summaryFuture.thenAccept(s -> System.out.println("Success: " +
     * s.getSuccess()));
     */

    /**
     * 异步下载单个文件（使用 S3AsyncClient）
     * 
     * @param fileKey      S3 对象的键
     * @param downloadPath 本地下载路径
     * @return CompletableFuture<String> 下载后的文件路径
     */
    public CompletableFuture<String> downloadFileAsync(String fileKey, String downloadPath) {
        // 去除开头的斜杠
        String sanitizedKey = fileKey.startsWith("/") ? fileKey.substring(1) : fileKey;

        String fileName = Paths.get(sanitizedKey).getFileName().toString();
        Path filePath = Paths.get(downloadPath, fileName);

        try {
            // 创建目录
            Files.createDirectories(filePath.getParent());

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(sanitizedKey)
                    .build();

            // 使用异步客户端下载到文件
            return s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toFile(filePath))
                    .thenApply(response -> {
                        log.info("Successfully downloaded {} to {}", sanitizedKey, filePath);
                        return filePath.toString();
                    })
                    .exceptionally(e -> {
                        log.error("Error downloading {}: {}", sanitizedKey, e.getMessage(), e);
                        // 清理可能的部分下载文件
                        try {
                            Files.deleteIfExists(filePath);
                        } catch (IOException ex) {
                            log.warn("Failed to delete partial file: {}", filePath);
                        }
                        throw new RuntimeException("Failed to download file: " + sanitizedKey, e);
                    });

        } catch (Exception e) {
            log.error("Error setting up download for {}: {}", fileKey, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步下载多个文件
     *
     * @param fileList            文件键列表
     * @param defaultDownloadPath 本地下载路径（用于保存下载的文件）
     * @param concurrency         并发批大小
     * @return 下载汇总结果的 Future
     */
    public CompletableFuture<DownloadSummary> downloadFilesAsync(List<String> fileList, String defaultDownloadPath,
            int concurrency) {
        if (fileList == null || fileList.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("fileList must be a non-empty list"));
        }

        DownloadSummary summary = new DownloadSummary();
        summary.setTotal(fileList.size());

        List<CompletableFuture<DownloadResult>> futures = new ArrayList<>();
        List<CompletableFuture<DownloadResult>> batch = new ArrayList<>(concurrency);

        for (int i = 0; i < fileList.size(); i++) {
            final int index = i;
            final String fileKey = fileList.get(i);

            CompletableFuture<DownloadResult> future = downloadFileAsync(fileKey, defaultDownloadPath)
                    .thenApply(filePath -> {
                        DownloadResult result = new DownloadResult();
                        result.setSuccess(true);
                        result.setFileKey(fileKey);
                        result.setDownloadPath(defaultDownloadPath);
                        result.setFilePath(filePath);
                        result.setIndex(index);
                        return result;
                    })
                    .exceptionally(e -> {
                        DownloadResult result = new DownloadResult();
                        result.setSuccess(false);
                        result.setFileKey(fileKey);
                        result.setDownloadPath(defaultDownloadPath);
                        result.setError(e.getMessage());
                        result.setIndex(index);
                        return result;
                    });

            futures.add(future);
            batch.add(future);

            // 控制并发：达到批次大小就等待该批完成
            if (batch.size() == concurrency) {
                CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).join();
                batch.clear();
            }
        }

        // 等待剩余未满批次的任务
        if (!batch.isEmpty()) {
            CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).join();
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<DownloadResult> allResults = futures.stream()
                            .map(CompletableFuture::join)
                            .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
                            .collect(Collectors.toList());

                    summary.setResults(allResults);
                    summary.setSuccess((int) allResults.stream().filter(DownloadResult::isSuccess).count());
                    summary.setFailed(summary.getTotal() - summary.getSuccess());

                    List<DownloadResult> errors = allResults.stream()
                            .filter(r -> !r.isSuccess())
                            .collect(Collectors.toList());
                    summary.setErrors(errors);

                    log.info("Async download summary: {}/{} files downloaded successfully", summary.getSuccess(),
                            summary.getTotal());
                    if (summary.getFailed() > 0) {
                        log.warn("{} files failed to download", summary.getFailed());
                    }
                    return summary;
                });
    }

    /**
     * 异步下载多个文件（使用默认并发数5）
     */
    public CompletableFuture<DownloadSummary> downloadFilesAsync(List<String> fileList, String defaultDownloadPath) {
        return downloadFilesAsync(fileList, defaultDownloadPath, 5);
    }

    // endregion
    // #region 删除文件

    /**
     * 删除 S3 上的文件夹（根据前缀删除所有对象）
     */
    public boolean deleteFolder(String folderPrefix) {
        try {
            boolean isTruncated = true;
            String continuationToken = null;
            List<S3Object> allObjects = new ArrayList<>();

            // 分页列出所有对象
            while (isTruncated) {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(folderPrefix);

                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(requestBuilder.build());

                // 将当前批次的文件加入到 allObjects 列表中
                if (listObjectsResponse.contents() != null) {
                    allObjects.addAll(listObjectsResponse.contents());
                }

                isTruncated = listObjectsResponse.isTruncated();
                continuationToken = listObjectsResponse.nextContinuationToken();
            }

            // 过滤并构建要删除的对象列表
            List<ObjectIdentifier> objectsToDelete = allObjects.stream()
                    .filter(obj -> obj != null && obj.key() != null)
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .collect(Collectors.toList());

            // 如果没有任何要删除的对象，直接返回
            if (objectsToDelete.isEmpty()) {
                log.info("No objects found with prefix \"{}\", skip delete.", folderPrefix);
                return true;
            }

            // 删除所有列出的对象
            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(builder -> builder.objects(objectsToDelete).quiet(false))
                    .build();

            DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(deleteObjectsRequest);
            log.info("Successfully deleted {} objects.", deleteResponse.deleted().size());
            return true;

        } catch (Exception e) {
            log.error("Error deleting objects with prefix: {}", folderPrefix, e);
            return false;
        }
    }
    /*
     * 用法示例：
     * 
     * @Autowired
     * private ObjFileManager objFileManager;
     * 
     * // 删除指定前缀的文件夹
     * String folderPrefix =
     * "video/2025/06/2025-06-20/d96778cb-a57c-4a09-b9c3-c7f09d48e4ab/1080P/";
     * boolean success = objFileManager.deleteFolder(folderPrefix);
     */

    // endregion

    // #region查看文件是否存在
    // --------------------------------------------inspect------------------------------------

    /**
     * 检查对象是否存在于 S3
     * 
     * @param s3Key S3 对象键
     * @return 对象是否存在
     */
    public boolean objectExists(String s3Key) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Key)
                    .maxKeys(1)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            log.debug("S3 response for key {}: {} objects found", s3Key,
                    response.contents() != null ? response.contents().size() : 0);

            return response.contents() != null && !response.contents().isEmpty();

        } catch (Exception e) {
            // 对象不存在时 S3 可能返回 404 或其他错误
            log.error("Error checking object existence for key {}: {}", s3Key, e.getMessage());
            return false;
        }
    }

    /*
     * 用法示例：
     * 
     * @Autowired
     * private ObjFileManager objFileManager;
     * 
     * // 检查对象是否存在
     * boolean exists = objFileManager.objectExists("video/2024/11/file.mp4");
     * if (exists) {
     * System.out.println("Object exists in S3");
     * } else {
     * System.out.println("Object does not exist in S3");
     * }
     */

    // #endregion
}
