package com.vim.webpage.Utils;

import com.vim.webpage.enums.FileTypeEnum;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件路径工具类
 */
public class FilePathUtil {

    // UUID 匹配模式
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    
    /**
     * 标准化路径 - 将 Windows 反斜杠转换为正斜杠
     */
    public static String normalizePath(String filePath) {
        if (filePath == null) {
            return null;
        }
        return filePath.replace("\\", "/");
    }

    /**
     * 获取根路径
     * 规则：
     * - Thumbnail/m3u8: 截取到 UUID 部分
     * - ts/preview/key: 截取到 UUID 的上一级(包含分辨率目录)
     */
    public static String getRootPath(String filePath, FileTypeEnum fileType) {
        if (filePath == null || fileType == null) {
            return null;
        }

        String normalized = normalizePath(filePath);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        Matcher matcher = UUID_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        int uuidEnd = matcher.end();
        
        if (fileType == FileTypeEnum.THUMBNAIL || fileType == FileTypeEnum.M3U8) {
            // 截取到 UUID 结尾
            return normalized.substring(0, uuidEnd);
        } else if (fileType == FileTypeEnum.TS || fileType == FileTypeEnum.PREVIEW || fileType == FileTypeEnum.KEY) {
            // 找到 UUID 后的第一个 '/'
            int nextSlash = normalized.indexOf('/', uuidEnd);
            if (nextSlash > 0) {
                return normalized.substring(0, nextSlash);
            }
        }
        
        return null;
    }

    /**
     * 从文件路径中提取 UUID
     */
    public static String getUUID(String filePath) {
        if (filePath == null) {
            return null;
        }

        String normalized = normalizePath(filePath);
        Matcher matcher = UUID_PATTERN.matcher(normalized);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        String normalized = normalizePath(filePath);
        Path path = Paths.get(normalized);
        return path.getFileName().toString();
    }

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        String fileName = getFileName(filePath);
        int lastDot = fileName.lastIndexOf('.');
        
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot).toLowerCase();
        }
        
        return null;
    }

    /**
     * 组合路径
     */
    public static String joinPath(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == null || parts[i].isEmpty()) {
                continue;
            }
            
            String part = normalizePath(parts[i]);
            
            // 移除开头的 '/' (除了第一个部分)
            if (i > 0 && part.startsWith("/")) {
                part = part.substring(1);
            }
            
            // 移除结尾的 '/'
            if (part.endsWith("/")) {
                part = part.substring(0, part.length() - 1);
            }
            
            if (result.length() > 0 && !result.toString().endsWith("/")) {
                result.append("/");
            }
            result.append(part);
        }
        
        return result.toString();
    }
}
