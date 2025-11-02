package com.vim.modules.upload.config;

import lombok.Data;

@Data
public class LocalConfig {
    //url 前缀路径
    private String basePath ;
    //上传文件绝对路径
    private String uploadPath;
}
