package com.vim.modules.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "upload")
public class UploadConfig {

    /**
     * 默认使用本地存储
     */
    private String type;


    private LocalConfig local = new LocalConfig();

}
