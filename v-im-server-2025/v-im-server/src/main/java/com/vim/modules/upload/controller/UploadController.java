package com.vim.modules.upload.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.exception.VimBaseException;
import com.vim.modules.sys.result.SysConfig;
import com.vim.modules.upload.config.UploadConfig;
import com.vim.modules.upload.service.UploadService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/vim/upload")
public class UploadController {

    @Resource
    private SysConfig sysConfig;

    @Resource
    @Qualifier("localUploadService")
    private UploadService currentUploadService;

    @PostMapping
    public SaResult upload(@RequestParam("file") MultipartFile file) {
        try {
            // 文件大小验证
            if (file.getSize() > sysConfig.getUploadSize()) {
                throw new VimBaseException("文件大小不能超过" + sysConfig.getUploadSize() / 1024 / 1024 + "MB");
            }
            // 文件类型验证
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            assert extension != null;
            if (!sysConfig.getUploadType().contains(extension.toLowerCase())) {
                throw new VimBaseException("文件类型不支持");
            }
            return SaResult.data(currentUploadService.upload(file));
        } catch (Exception e) {
            log.error(e.getMessage());
            return SaResult.error("上传失败：" + e.getMessage());
        }
    }
}
