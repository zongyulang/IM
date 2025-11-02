package com.vim.modules.login.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "user.password")
public class LoginConfig {

    //密码最大错误次数
    private int maxRetryCount;

    //锁定时间
    private int lockTime;
}
