package com.vim.webpage.Base.ObjectStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ObjFileManager {

    @Autowired
    private S3Client s3Client;

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
     * 下载单个文件
     * 
     * @param fileKey      S3 对象的键
     * @param downloadPath 本地下载路径
     * @return 下载后的文件路径
     */
    public String downloadFile(String fileKey, String downloadPath) {
        // 去除开头的斜杠
        if (fileKey.startsWith("/")) {
            fileKey = fileKey.substring(1);
        }

        String fileName = Paths.get(fileKey).getFileName().toString();
        String filePath = Paths.get(downloadPath, fileName).toString();
        String tempPath = filePath + ".tmp";
        String dirPath = Paths.get(filePath).getParent().toString();

        try {
            // 创建目录
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            // 先写入临时文件
            try (FileOutputStream fos = new FileOutputStream(tempPath);
                    InputStream is = s3Object) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 原子操作：重命名临时文件
            File tempFile = new File(tempPath);
            File targetFile = new File(filePath);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            tempFile.renameTo(targetFile);

            log.info("Successfully downloaded {} to {}", fileKey, filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error downloading {}: {}", fileKey, e.getMessage(), e);
            // 清理临时文件
            File tempFile = new File(tempPath);
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new RuntimeException("Failed to download file: " + fileKey, e);
        }
    }

    /**
     * 下载文件夹
     * 
     * @param folderPrefix S3 文件夹前缀
     * @param downloadPath 本地下载路径
     * @return 是否下载成功
     */
    public boolean downloadFolder(String folderPrefix, String downloadPath) {
        try {
            // 去除开头的斜杠
            if (folderPrefix.startsWith("/")) {
                folderPrefix = folderPrefix.substring(1);
            }

            boolean isTruncated = true;
            String continuationToken = null;
            List<S3Object> allObjects = new ArrayList<>();

            // Step 1: 列举文件夹下所有对象
            while (isTruncated) {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(folderPrefix);

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

            // 过滤掉无效对象
            allObjects = allObjects.stream()
                    .filter(obj -> obj != null && obj.key() != null)
                    .collect(Collectors.toList());

            // Step 2: 批量下载，每 100 个为一批
            int batchSize = 100;
            for (int i = 0; i < allObjects.size(); i += batchSize) {
                int end = Math.min(i + batchSize, allObjects.size());
                List<S3Object> batch = allObjects.subList(i, end);

                for (S3Object obj : batch) {
                    downloadSingleObject(obj, folderPrefix, downloadPath);
                }
            }

            log.info("Successfully downloaded folder {} to {}", folderPrefix, downloadPath);
            return true;

        } catch (Exception e) {
            log.error("Error downloading folder {}: {}", folderPrefix, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 下载单个对象（内部方法）
     */
    private void downloadSingleObject(S3Object obj, String folderPrefix, String downloadPath) {
        try {
            // 计算本地存储路径
            String relativePath = obj.key().replace(folderPrefix, "");
            String targetFilePath = Paths.get(downloadPath, relativePath).toString().replace("\\", "/");
            String dirPath = Paths.get(targetFilePath).getParent().toString();

            // 创建目录
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(obj.key())
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            try (FileOutputStream fos = new FileOutputStream(targetFilePath);
                    InputStream is = s3Object) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            log.debug("Successfully downloaded {} to {}", obj.key(), targetFilePath);

        } catch (Exception e) {
            log.error("Error downloading {}: {}", obj.key(), e.getMessage(), e);
            throw new RuntimeException("Failed to download object: " + obj.key(), e);
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

    /**
     * 下载多个文件
     * 
     * @param fileList            文件键列表
     * @param defaultDownloadPath 默认下载路径
     * @param concurrency         并发数量，默认5
     * @param continueOnError     遇到错误是否继续，默认true
     * @return 下载汇总结果
     */
    public DownloadSummary downloadFiles(List<String> fileList, String defaultDownloadPath,
            int concurrency, boolean continueOnError) {
        if (fileList == null || fileList.isEmpty()) {
            throw new IllegalArgumentException("fileList must be a non-empty list");
        }

        DownloadSummary summary = new DownloadSummary();
        summary.setTotal(fileList.size());

        List<DownloadResult> allResults = new ArrayList<>();

        // 分批处理，控制并发数
        for (int i = 0; i < fileList.size(); i += concurrency) {
            int end = Math.min(i + concurrency, fileList.size());
            List<String> batch = fileList.subList(i, end);

            for (int j = 0; j < batch.size(); j++) {
                String fileKey = batch.get(j);
                DownloadResult result = new DownloadResult();
                result.setFileKey(fileKey);
                result.setDownloadPath(defaultDownloadPath);
                result.setIndex(i + j);

                try {
                    String filePath = downloadFile(fileKey, defaultDownloadPath);
                    result.setSuccess(true);
                    result.setFilePath(filePath);
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setError(e.getMessage());
                    summary.getErrors().add(result);

                    if (!continueOnError) {
                        log.error("Download batch failed, stopping: {}", e.getMessage());
                        break;
                    }
                }

                allResults.add(result);
            }

            // 记录批次完成日志
            int batchNumber = (i / concurrency) + 1;
            long successCount = allResults.stream().filter(DownloadResult::isSuccess).count();
            long failCount = allResults.size() - successCount;
            log.info("Batch {} completed: {} success, {} failed", batchNumber, successCount, failCount);
        }

        // 设置汇总结果
        summary.setResults(allResults);
        summary.setSuccess((int) allResults.stream().filter(DownloadResult::isSuccess).count());
        summary.setFailed(summary.getTotal() - summary.getSuccess());

        log.info("Download summary: {}/{} files downloaded successfully", summary.getSuccess(), summary.getTotal());

        if (summary.getFailed() > 0) {
            log.warn("{} files failed to download", summary.getFailed());
        }

        return summary;
    }

    /**
     * 下载多个文件（使用默认参数）
     */
    public DownloadSummary downloadFiles(List<String> fileList, String defaultDownloadPath) {
        return downloadFiles(fileList, defaultDownloadPath, 5, true);
    }

    /*
     * 用法示例：
     * 
     * @Autowired
     * private ObjFileManager objFileManager;
     * 
     * // 下载单个文件
     * String filePath = objFileManager.downloadFile("video/2024/file.mp4",
     * "./downloads");
     * 
     * // 下载文件夹
     * boolean success = objFileManager.downloadFolder("video/2024/11/",
     * "./downloads");
     * 
     * // 下载多个文件
     * List<String> files = Arrays.asList("file1.mp4", "file2.mp4");
     * DownloadSummary summary = objFileManager.downloadFiles(files, "./downloads");
     */

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
