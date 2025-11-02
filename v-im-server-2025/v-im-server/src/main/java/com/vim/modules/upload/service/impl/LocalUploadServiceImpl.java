package com.vim.modules.upload.service.impl;

import cn.hutool.core.io.FileUtil;
import com.vim.common.utils.ServletUtils;
import com.vim.modules.upload.config.UploadConfig;
import com.vim.modules.upload.result.UploadResult;
import com.vim.modules.upload.service.UploadService;
import com.vim.modules.upload.utils.UploadUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;

@Service("localUploadService")
public class LocalUploadServiceImpl implements UploadService {

    @Resource
    private UploadConfig uploadConfig;

    @Override
    public UploadResult upload(MultipartFile file) throws Exception {
        // 生成新文件名
        String newFileName = UploadUtil.generateFileName(file.getOriginalFilename());

        String uploadPath = uploadConfig.getLocal().getUploadPath();

        // 保存文件到服务器绝对路径
        File dest = new File(Paths.get(uploadPath, newFileName).toString());
        FileUtil.writeFromStream(file.getInputStream(), dest);

        HttpServletRequest request =  ServletUtils.getRequest();
        String serverUrl = "%s://%s:%d%s".formatted(request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath());

        UploadResult result = new UploadResult();
        result.setUrl("%s/%s%s".formatted(serverUrl, uploadConfig.getLocal().getBasePath(), newFileName));
        result.setFileName(newFileName);
        result.setOriginalFilename(file.getOriginalFilename());
        return result;
    }
}
