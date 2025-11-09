package com.vim.webpage.enums;

/**
 * 文件类型枚举
 */
public enum FileTypeEnum {
    THUMBNAIL("Thumbnail", ".webp", ".jpg", ".png",".mp4"),
    M3U8("m3u8", ".m3u8"),
    TS("ts", ".ts"),
    PREVIEW("preview", ".jpg"),
    KEY("key", ".key"),
    AVATAR("avatar", ".webp", ".jpg", ".png"),
    UNKNOWN("unknown");

    private final String type;
    private final String[] extensions;

    FileTypeEnum(String type, String... extensions) {
        this.type = type;
        this.extensions = extensions;
    }

    public String getType() {
        return type;
    }

    public String[] getExtensions() {
        return extensions;
    }

    /**
     * 根据文件扩展名获取文件类型
     */
    public static FileTypeEnum fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }

        for (FileTypeEnum type : values()) {
            for (String typeExt : type.extensions) {
                if (typeExt.equals(ext)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }
}
