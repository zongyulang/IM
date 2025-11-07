package com.vim.webpage.controller.Web.Static;

import com.vim.webpage.service.Storage.AvatarFilePullService;
import com.vim.webpage.service.Storage.VideoFilePullService;
import com.vim.webpage.Utils.CDN_decrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源控制器
 * 处理视频、图片、头像等静态资源的请求
 */
@Slf4j
@RestController
@RequestMapping("/api/static")
public class StaticController {

    @Autowired
    private VideoFilePullService videoFilePullService;

    @Autowired
    private AvatarFilePullService avatarFilePullService;

    @Autowired
    private CDN_decrypt cdnDecrypt;

    /**
     * 获取视频文件 (m3u8, ts, key 等)
     * 示例: GET /api/static/video/2025/07/uuid/index.m3u8
     */
    @GetMapping("/video/**")
    public ResponseEntity<Resource> getVideoFile(HttpServletRequest request) {
        try {
            // 提取实际的文件路径
            String requestPath = request.getRequestURI();
            String filePath = requestPath.replace("/api/static/", "");
            
            log.info("Requesting video file: {}", filePath);
            
            // 获取文件（自动处理下载逻辑）
            String localPath = videoFilePullService.getFile(filePath);
            
            return serveFile(localPath);
            
        } catch (Exception e) {
            log.error("Error serving video file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取缩略图
     * 示例: GET /api/static/thumbnail/2025/07/uuid/Thumbnail_combined.mp4
     */
    @GetMapping("/thumbnail/**")
    public ResponseEntity<Resource> getThumbnail(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();
            String filePath = requestPath.replace("/api/static/", "");
            
            log.info("Requesting thumbnail: {}", filePath);
            
            String localPath = videoFilePullService.getFile(filePath);
            
            return serveFile(localPath);
            
        } catch (Exception e) {
            log.error("Error serving thumbnail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取预览图
     * 示例: GET /api/static/preview/2025/07/uuid/preview_image_0.jpg
     */
    @GetMapping("/preview/**")
    public ResponseEntity<Resource> getPreview(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();
            String filePath = requestPath.replace("/api/static/", "");
            
            log.info("Requesting preview image: {}", filePath);
            
            String localPath = videoFilePullService.getFile(filePath);
            
            return serveFile(localPath);
            
        } catch (Exception e) {
            log.error("Error serving preview image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取头像
     * 示例: GET /api/static/avatar/user123.jpg
     */
    @GetMapping("/avatar/**")
    public ResponseEntity<Resource> getAvatar(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();
            String avatarPath = requestPath.replace("/api/static/", "");
            
            log.info("Requesting avatar: {}", avatarPath);
            
            String localPath = avatarFilePullService.getAvatar(avatarPath);
            
            return serveFile(localPath);
            
        } catch (Exception e) {
            log.error("Error serving avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成签名的 M3U8 URL
     * 示例: POST /api/static/sign/m3u8
     * Body: { "path": "video/2025/07/uuid/index.m3u8" }
     */
    @PostMapping("/sign/m3u8")
    public ResponseEntity<SignResponse> signM3u8Url(@RequestBody SignRequest request) {
        try {
            String signedUrl = cdnDecrypt.generateM3u8Hash(request.getPath());
            return ResponseEntity.ok(new SignResponse(true, signedUrl, null));
        } catch (Exception e) {
            log.error("Error signing M3U8 URL", e);
            return ResponseEntity.ok(new SignResponse(false, null, e.getMessage()));
        }
    }

    /**
     * 生成签名的图片 URL
     * 示例: POST /api/static/sign/image
     * Body: { "path": "video/2025/07/uuid/preview_image_0.jpg" }
     */
    @PostMapping("/sign/image")
    public ResponseEntity<SignResponse> signImageUrl(@RequestBody SignRequest request) {
        try {
            String signedUrl = cdnDecrypt.generateSignedPathWithVersion(request.getPath());
            return ResponseEntity.ok(new SignResponse(true, signedUrl, null));
        } catch (Exception e) {
            log.error("Error signing image URL", e);
            return ResponseEntity.ok(new SignResponse(false, null, e.getMessage()));
        }
    }

    /**
     * 提供文件下载
     */
    private ResponseEntity<Resource> serveFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            log.warn("File not found: {}", filePath);
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(path.toFile());
        String contentType = Files.probeContentType(path);
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }

    /**
     * 签名请求参数
     */
    public static class SignRequest {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * 签名响应
     */
    public static class SignResponse {
        private boolean success;
        private String signedUrl;
        private String error;

        public SignResponse(boolean success, String signedUrl, String error) {
            this.success = success;
            this.signedUrl = signedUrl;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getSignedUrl() {
            return signedUrl;
        }

        public void setSignedUrl(String signedUrl) {
            this.signedUrl = signedUrl;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
