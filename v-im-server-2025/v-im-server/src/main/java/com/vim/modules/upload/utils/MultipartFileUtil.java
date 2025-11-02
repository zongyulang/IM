package com.vim.modules.upload.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * MultipartFile 工具类
 * 用于将图片字节数组转换为 MultipartFile 对象
 */
public class MultipartFileUtil {

    /**
     * 创建 MultipartFile 对象
     * @param imageBytes 图片字节数组
     * @param suffix 文件后缀，例如：jpg、png
     * @param contentType 文件类型，例如：image/jpeg、image/png
     * @return MultipartFile 对象
     */
    public static MultipartFile createMultipartFile(byte[] imageBytes, String suffix, String contentType) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return IdUtil.simpleUUID() + "." + suffix;
            }

            @Override
            public String getOriginalFilename() {
                return IdUtil.simpleUUID() + "." + suffix;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return imageBytes.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return imageBytes;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                FileUtil.writeBytes(imageBytes, dest);
            }
        };
    }
}
