package com.vim.modules.login.param;

import lombok.Data;

@Data
public class RegisterParam {
    private String username;
    private String password;
    private String code;
    private String uuid;
}
