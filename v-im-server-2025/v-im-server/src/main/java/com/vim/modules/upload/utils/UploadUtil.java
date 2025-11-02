package com.vim.modules.upload.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.io.FilenameUtils;

import java.util.Date;
import java.util.UUID;

public class UploadUtil {
    
    /**
     * 生成新的文件名
     * @param originalFilename 原始文件名
     * @return 新文件名（包含日期路径）
     */
    public static String generateFileName(String originalFilename) {
        return StrUtil.format("{}/{}.{}", 
            DateUtil.format(new Date(), "yyyy/MM/dd"),
            UUID.randomUUID().toString().replace("-", ""),
            FilenameUtils.getExtension(originalFilename)
        );
    }

    /**
     * 获取文件名（不含路径）
     * @param fileName 文件名
     * @return 处理后的文件名
     */
    public static String getFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastUnixPos = fileName.lastIndexOf('/');
        int lastWindowsPos = fileName.lastIndexOf('\\');
        int index = Math.max(lastUnixPos, lastWindowsPos);
        return fileName.substring(index + 1);
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名
     */
    public static String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }
}
