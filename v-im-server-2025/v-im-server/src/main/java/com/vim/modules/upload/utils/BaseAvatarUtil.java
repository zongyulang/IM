package com.vim.modules.upload.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.vim.common.utils.SpringUtils;
import com.vim.modules.upload.result.UploadResult;
import com.vim.modules.upload.service.UploadService;
import com.vim.modules.upload.service.impl.LocalUploadServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 头像处理基类
 * 封装了图片上传和MultipartFile创建的通用逻辑
 */
public abstract class BaseAvatarUtil {
    protected static final Logger log = LoggerFactory.getLogger(BaseAvatarUtil.class);
    protected static UploadService uploadService;

    // 静态初始化块，在类加载时初始化 uploadService
    static {
        uploadService = SpringUtils.getBean(LocalUploadServiceImpl.class);
    }

    /**
     * 上传图片
     * @param image 要上传的图片
     * @return 上传后的URL
     * @throws Exception 如果图片处理过程中发生错误
     */
    protected static String uploadImage(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        
        MultipartFile multipartFile = createMultipartFile(imageBytes);
        UploadResult result = uploadService.upload(multipartFile);
        return result.getUrl();
    }

    /**
     * 创建MultipartFile
     * @param imageBytes 图片字节数组
     * @return MultipartFile对象
     */
    private static MultipartFile createMultipartFile(byte[] imageBytes) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return IdUtil.simpleUUID() + ".png";
            }

            @Override
            public String getOriginalFilename() {
                return IdUtil.simpleUUID() + ".png";
            }

            @Override
            public String getContentType() {
                return "image/png";
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
                return IoUtil.toStream(imageBytes);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                cn.hutool.core.io.FileUtil.writeBytes(imageBytes, dest);
            }
        };
    }
}
