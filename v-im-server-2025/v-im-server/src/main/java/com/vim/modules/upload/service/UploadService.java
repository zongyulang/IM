package com.vim.modules.upload.service;

import com.vim.modules.upload.result.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问URL
     */
    UploadResult upload(MultipartFile file) throws Exception;
}
