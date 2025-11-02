package com.vim.modules.login.param;

import lombok.Data;

/**
 * 密码信息
 */
@Data
public class PwdParam {
    private String oldPassword;
    private String newPassword;
}
