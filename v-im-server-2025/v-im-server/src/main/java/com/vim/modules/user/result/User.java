package com.vim.modules.user.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String loginName;
    private String avatar;
    private String mobile;
    private String email;
    private String sex;
    private String deptId;

    public User() {
    }

    public User(String id, String name, String loginName, String avatar, String mobile, String sex, String deptId, String email) {
        this.id = id;
        this.name = name;
        this.loginName = loginName;
        this.avatar = avatar;
        this.mobile = mobile;
        this.sex = sex;
        this.deptId = deptId;
        this.email = email;
    }


}
