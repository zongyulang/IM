package com.vim.modules.login.param;

import lombok.Data;

@Data
public class LoginParam {
    private String username;
    private String password;
    private String device;
    private String uuid;
    private String code;

}
