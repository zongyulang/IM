package com.vim.webpage.Utils;

import com.vim.webpage.enums.FileTypeEnum;

/**
 * 文件类型工具类
 */
public class FileTypeUtil {

    /**
     * 根据文件路径获取文件类型
     */
    public static FileTypeEnum getFileType(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return FileTypeEnum.UNKNOWN;
        }

        String extension = FilePathUtil.getExtension(filePath);
        if (extension == null) {
            return FileTypeEnum.UNKNOWN;
        }

        // 特殊处理：根据路径判断是否为头像
        if (filePath.contains("/avatar/") || filePath.contains("\\avatar\\")) {
            return FileTypeEnum.AVATAR;
        }

        // 特殊处理：根据路径判断是否为预览图
        if (filePath.contains("preview_image_")) {
            return FileTypeEnum.PREVIEW;
        }

        // 特殊处理：根据路径判断是否为缩略图
        if (filePath.contains("Thumbnail")) {
            return FileTypeEnum.THUMBNAIL;
        }

        return FileTypeEnum.fromExtension(extension);
    }

    /**
     * 判断是否为视频相关文件(m3u8/ts/key)
     */
    public static boolean isVideoFile(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.M3U8 
            || fileType == FileTypeEnum.TS 
            || fileType == FileTypeEnum.KEY;
    }

    /**
     * 判断是否为图片文件
     */
    public static boolean isImageFile(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.THUMBNAIL 
            || fileType == FileTypeEnum.PREVIEW 
            || fileType == FileTypeEnum.AVATAR;
    }
}
