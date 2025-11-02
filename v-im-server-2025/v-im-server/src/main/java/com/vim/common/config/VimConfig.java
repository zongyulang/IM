package com.vim.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vim")
public class VimConfig {

    /**
     * 允许加好友
     */
    private String canAddFriend;

    /**
     * 添加好友审核
     */
    private String addFriendValidate;

    /**
     * 允许私聊
     */
    private String canSendMessage;

    /**
     * 语音消息提醒
     */
    private String canSoundRemind;

    /**
     * 通话消息提醒
     */
    private String canVoiceRemind;

    /**
     * 展示手机号
     */
    private String showMobile;


    /**
     * 展示邮箱
     */
    private String showEmail;


    /**
     * 上传文件path,默认用v-im
     */
    private boolean useImAvatar;

    /**
     * ws端口
     */
    private int wsPort;



}
