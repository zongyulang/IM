package com.vim.modules.sys.result;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置类
 * 该类映射 application-sys.yml 中的配置属性
 * 用于WebRTC和通信服务器设置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sys")
public class SysConfig {

    /**
     * 是否显示部门信息
     */
    private boolean showDept;

    /**
     * 是否显示应用信息
     */
    private boolean showOauth2;

    /**
     * 是否显示视频通话
     */
    private boolean showVideo;

    /**
     * 是否显示视频会议
     */
    private boolean showMeeting;


    /**
     * 首次登录修改密码
     */
    private boolean passwordFirstLoginUpdate;

    /**
     * 密码有效期 90 天
     */
    private int passwordValidateDays;

    /**
     * 密码的正则
     */
    private String passwordRegex;

    /**
     * 密码的说明
     */
    private String passwordRegexDesc;

    /**
     * 上传文件大小
     */
    private int uploadSize;

    /**
     * 上传文件类型
     */
    private String uploadType;
}
