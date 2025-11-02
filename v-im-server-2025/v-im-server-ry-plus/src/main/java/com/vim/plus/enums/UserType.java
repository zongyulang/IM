package com.vim.plus.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum UserType {

    SYS_USER("00", "系统用户"),
    APP_USER("11", "应用用户");

    private final String code;
    private final String description;

    UserType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserType getByCode(String code) {
        for (UserType userType : values()) {
            if (userType.getCode().equals(code)) {
                return userType;
            }
        }
        return null;
    }

    /**
     * 检查code是否存在
     */
    public static boolean contains(String code) {
        return getByCode(code) != null;
    }

    /**
     * 获取所有code列表
     */
    public static List<String> getAllCodes() {
        return Arrays.stream(values())
                .map(UserType::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return this.name() + "(" + code + ")";
    }
}