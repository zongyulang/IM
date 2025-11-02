package com.vim.tio.messages;

import lombok.Data;

/**
 * 登录信息
 */
@Data
public class ReadyAuth {

    // token
    private String token;

    // 客户端
    private String client;

    // uuid
    private String uuid;
}
