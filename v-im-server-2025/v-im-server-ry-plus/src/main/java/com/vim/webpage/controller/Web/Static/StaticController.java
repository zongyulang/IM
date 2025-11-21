package com.vim.webpage.controller.Web.Static;

import com.vim.webpage.Utils.CDN_decrypt;
import com.vim.webpage.service.Storage.AvatarFilePullService;
import com.vim.webpage.service.Storage.VideoFilePullService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源控制器
 * 功能：
 * 1. /proxy/** - 带签名验证的代理访问(图片、视频等)
 * 2. /preview/** - 预览图访问(带签名验证)
 * 3. /video/** - 视频文件访问(需要特定请求头验证)
 * 4. /M3u8/** - M3U8 播放列表访问(带签名验证并重写 TS 路径)
 */
@Slf4j
@RestController

public class StaticController {

    @Autowired
    private CDN_decrypt cdnDecrypt;

    @Autowired
    private VideoFilePullService videoFilePullService;

    @Autowired
    private AvatarFilePullService avatarFilePullService;

    @Value("${cdn.secretIGSK}")
    private String secretIGSK;

    @Value("${cdn.secretTGSK}")
    private String secretTGSK;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 处理 /proxy/** 请求 - 带签名验证的资源代理
     * URL 格式: /api/proxy/{signature}/{base64Path}
     */
    @GetMapping("/proxy/**")
    public ResponseEntity<?> handleProxy(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        log.info("Proxy request: {}", requestUrl);
        Thread currentThread = Thread.currentThread();
        // 输出当前处理线程的信息，便于确认是否为虚拟线程
        log.info("Request handling thread: name={}, id={}, isVirtual={}",
                currentThread.getName(), currentThread.getId(), currentThread.isVirtual());
        try {
            // 验证签名并解码路径
            CDN_decrypt.VerifyResult result = cdnDecrypt.verifySignedPathWithVersion(requestUrl, secretIGSK);

            if (!result.valid) {
                log.error("❌ 验证签名失败: {}", result.reason);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"message\": \"Forbidden\"}");
            }

            log.info("✅ 验证签名成功: {}", result.decodedPath);
            String decodedPath = result.decodedPath;

            // 判断是否是头像文件
            if (decodedPath.startsWith("/Avatar/")) {
                return handleAvatarFile(decodedPath);
            } else {
                // 获取文件（自动下载）
                String localPath = videoFilePullService.getFile(decodedPath);
                return serveFile(localPath, true);
            }

        } catch (Exception e) {
            log.error("❌ Proxy 请求处理失败: {}", requestUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Fetch Static Error\"}");
        }
    }

    /**
     * 处理 /preview/** 请求 - 预览图访问
     * URL 格式: /api/preview/{signature}/{base64Path}
     */
    @GetMapping("/preview/**")
    public ResponseEntity<?> handlePreview(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        log.info("Preview request: {}", requestUrl);

        try {
            // 验证预览图签名
            CDN_decrypt.VerifyResult result = cdnDecrypt.verifyPreviewImagePath(requestUrl, secretIGSK);

            if (!result.valid) {
                log.error("❌ 验证签名失败: {}", result.reason);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"message\": \"Forbidden\"}");
            }

            log.info("✅ 验证签名成功: {}", result.decodedPath);

            // 获取文件
            String localPath = videoFilePullService.getFile(result.decodedPath);
            return serveFile(localPath, true);

        } catch (Exception e) {
            log.error("❌ Preview 请求处理失败: {}", requestUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Fetch Static Error\"}");
        }
    }

    /**
     * 处理 /video/** 请求 - 视频资源访问
     * 需要验证特定请求头 x-edge-cf
     */
    @GetMapping("/video/**")
    public ResponseEntity<?> handleVideo(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String requestPath = requestUrl.substring("/api".length()); // 移除 /api 前缀
        log.info("Video request: {}", requestPath);

        try {
            // 生产环境验证 x-edge-cf 头
            if (!"dev".equals(activeProfile) && !"development".equals(activeProfile)) {
                String cfHeader = request.getHeader("x-edge-cf");
                if (cfHeader == null) {
                    log.warn("❌ 缺少 x-edge-cf 头");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("{\"message\": \"Missing header\"}");
                }
                // 返回的从cfworker中获取的头
                if (!"99681556".equals(cfHeader)) {
                    log.warn("❌ x-edge-cf 值错误: {}", cfHeader);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("{\"message\": \"Invalid header\"}");
                }
            }

            // 获取文件
            String localPath = videoFilePullService.getFile(requestPath);

            // 设置 CORS（开发环境）
            HttpHeaders headers = new HttpHeaders();
            String origin = request.getHeader("Origin");
            if (origin != null && !"production".equals(activeProfile)) {
                headers.add("Access-Control-Allow-Origin", origin);
            }
            headers.setCacheControl("public, max-age=2592000, immutable");

            return serveFileWithHeaders(localPath, headers, false);

        } catch (Exception e) {
            log.error("❌ Video 请求处理失败: {}", requestPath, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\": \"File not found\"}");
        }
    }

    /**
     * 处理 /M3u8/** 请求 - M3U8 播放列表访问
     * URL 格式:
     * /api/M3u8/{pathSig}/{base64path}/{filename}.m3u8?validfrom=...&validto=...&hash=...
     */
    @GetMapping("/M3u8/**")
    public ResponseEntity<?> handleM3u8(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? requestUrl + "?" + queryString : requestUrl;
        log.info("M3U8 request: {}", fullUrl);

        try {
            // 验证 M3U8 签名
            CDN_decrypt.VerifyResult result = cdnDecrypt.verifyM3u8Hash(fullUrl, secretTGSK);

            if (!result.valid) {
                log.error("验证签名失败: {}", result.reason);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"message\": \"Forbidden\"}");
            }

            String decodedPath = result.decodedPath;
            log.info("✅ M3U8 验证成功: {}", decodedPath);

            // 获取文件
            String localPath = videoFilePullService.getFile(decodedPath);
            Path filePath = Paths.get(localPath);

            // 读取 m3u8 内容
            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            // 重写 TS 路径
            String basePath = decodedPath.substring(0, decodedPath.lastIndexOf('/') + 1);
            String fileName = decodedPath.substring(decodedPath.lastIndexOf('/') + 1);
            String hdl = fileName.contains("Middle") ? "-1" : "1";

            // 获取客户端 IP
            String ipa = request.getHeader("x-forwarded-for");
            if (ipa != null && ipa.contains(",")) {
                ipa = ipa.split(",")[0].trim();
            }
            if (ipa == null) {
                ipa = request.getRemoteAddr();
            }

            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null) {
                userAgent = "";
            }

            // 重写内容
            String rewrittenContent = cdnDecrypt.setM3u8TsPath(
                    content, basePath, secretTGSK, ipa, hdl, userAgent, 3600);

            // 返回 M3U8 内容
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
            headers.setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(rewrittenContent);

        } catch (Exception e) {
            log.error("❌ M3U8 请求处理失败: {}", fullUrl, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\": \"m3u8 file not found\"}");
        }
    }

    /**
     * 处理头像文件
     */
    private ResponseEntity<?> handleAvatarFile(String avatarPath) {
        try {
            String localPath = avatarFilePullService.getAvatar(avatarPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            // 设置缓存头
            headers.setCacheControl("public, max-age=31536000, immutable");

            return serveFileWithHeaders(localPath, headers, true);

        } catch (Exception e) {
            log.error("获取头像文件失败: {}", avatarPath, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\": \"Avatar file not found\"}");
        }
    }

    /**
     * 返回文件资源
     */
    private ResponseEntity<Resource> serveFile(String filePath, boolean longCache) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                // 根据扩展名兜底
                String fn = path.getFileName().toString().toLowerCase();
                if (fn.endsWith(".webp")) {
                    contentType = "image/webp";
                } else if (fn.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG_VALUE;
                } else if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) {
                    contentType = MediaType.IMAGE_JPEG_VALUE;
                } else if (fn.endsWith(".gif")) {
                    contentType = MediaType.IMAGE_GIF_VALUE;
                } else if (fn.endsWith(".m3u8")) {
                    contentType = "application/vnd.apple.mpegurl";
                } else if (fn.endsWith(".ts")) {
                    contentType = "video/mp2t";
                } else if (fn.endsWith(".mp4")) {
                    contentType = "video/mp4";
                } else {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            if (longCache) {
                headers.setCacheControl("public, max-age=31536000, immutable");
            } else {
                headers.setCacheControl("public, max-age=2592000, immutable");
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 返回文件资源（带自定义 headers）
     */
    private ResponseEntity<Resource> serveFileWithHeaders(String filePath, HttpHeaders customHeaders, boolean isImage) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path);

            if (customHeaders.getContentType() == null) {
                String contentType = Files.probeContentType(path);
                if (contentType == null) {
                    contentType = isImage ? MediaType.IMAGE_PNG_VALUE : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                customHeaders.setContentType(MediaType.parseMediaType(contentType));
            }

            return ResponseEntity.ok()
                    .headers(customHeaders)
                    .body(resource);

        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}